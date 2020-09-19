(function() {
    const handler = {};


    function onCreate() {
        const fbVideoIds = [];

        if (location.origin.includes("facebook.com")) {
            setInterval(function() {
                const fbVideoLinks = document.querySelectorAll("a[href*='/videos/']");
                for (let i = 0; i < fbVideoLinks.length; i++) {
                    const href = fbVideoLinks[i].getAttribute("href");
                    if (href.startsWith("https://")) {
                        const nodes = href.split("/");
                        const id = nodes[nodes.indexOf("videos") + 1];
                        
                        if (!fbVideoIds.includes(id)) {
                            fbVideoIds.push(id);
                            console.log(id);
                            sendToBackground({cmd: "update_fb", videoId: id});
                        }
                    }
                }
            }, 2000)
        }

    }


    function sendToPopup(payload) {
        chrome.runtime.sendMessage(payload);
    }

    function sendToBackground(payload, callback) {
        const port = chrome.extension.connect({
            name: "Communication1"
        });
        port.postMessage(payload);
        port.onMessage.addListener(function(recv) {
            if (callback !== undefined || callback !== null) {
                callback(recv);
            }
        });
    }
    

    handler.open = payload => {
        const url = new URL(payload.tab.url);
        if (url.origin.includes("youtube.com")) {
            const video = _("video")[0];
            if (video !== null) {
                const params = url.searchParams;
                params.set("t", parseInt(video.currentTime))
                sendToBackground({cmd: "open", url: encodeURIComponent(url.toString()), title: document.title});
            } else {
                sendToBackground({cmd: "open", url: payload.encodedUrl, title: document.title});
            }
        } else {
            sendToBackground({cmd: "open", url: payload.encodedUrl, title: document.title});
        }
    };

    handler.read = payload => {
        const url = new URL(payload.tab.url);
        sendToBackground({cmd: "read", url: payload.encodedUrl, title: document.title});
    };

    handler.connection = payload => {
        if (payload.isStatus) {
            sendToBackground(payload, res => {
                sendToPopup(res);
            });
        } else if (payload.isDisconnect) {
            sendToBackground(payload);
        } else {
            chrome.storage.local.get(["server"], res => {
                const server = prompt("Enter the Pyrum sever address (192.168.x.x)", res.server)
                if (server.trim() !== "") {
                    payload.server = server;
                    sendToBackground(payload);
                }
            });
        }
    };

    handler.update = payload => {
        sendToBackground(payload, res => {
            sendToPopup(res);
        });
    };

    handler.inapp = payload => {
        if (payload.type === "images") {
            sendToBackground(payload);
        } else {
            sendToBackground({cmd: "update"}, res => {
                if (res.media.length === 1 || payload.type === "last-video" || payload.type === "last-music") {
                    const video = document.querySelector("video");
                    const audio = document.querySelector("audio");
                    if (video !== null) {
                        payload.currentTime = parseInt(video.currentTime);
                    } else if (audio !== null) {
                        payload.currentTime = parseInt(audio.currentTime);
                    } else {
                        payload.currentTime = 0;
                    }
                } else {
                    payload.currentTime = 0;
                }
                sendToBackground(payload);
            });
        }
    };


    chrome.runtime.onMessage.addListener(payload => {
        handler[payload.cmd](payload);
    });


    onCreate();

})();