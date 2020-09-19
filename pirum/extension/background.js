(function() {
    const handler = {};
    const detector = new Detector();
    const imageDetector = new ImageDetector();
    const ignoreExtensions = [".m4s"];

    function onCreate() {
        chrome.webRequest.onHeadersReceived.addListener(function(details){
            let shouldIgnore = false;
            for (const ext of ignoreExtensions) {
                if (details.url.split("?")[0].endsWith(ext)) {
                    shouldIgnore = true;
                    break;
                }
            }

            const headers = {};
            for (const head of details.responseHeaders) {
                headers[head.name.toLowerCase()] = head.value;
            }

            if (headers["content-type"] !== undefined) {
                if (headers["content-type"].startsWith("image/")) {
                    if (parseInt(headers["content-length"]) >= 20000) {
                        imageDetector.run({url: details.url, tabId: details.tabId, responseHeaders: headers})
                    }
                }

                if (!shouldIgnore && !headers["content-type"].startsWith("image/")) {
                    const payload = {
                        url: details.url,
                        responseHeaders: headers,
                        tabId: details.tabId
                    }
                    detector.detect(payload);
                }
            }
        },
        {urls: ["*://*/*"]},["responseHeaders"]);

        chrome.webNavigation.onCommitted.addListener(function (details) { 
            console.log(details.transitionType);
            if (details.transitionType === "reload" || details.transitionType === "typed" || details.transitionType === "link") {
                imageDetector.clear(details.tabId);
                detector.clearTab(details.tabId);
            }
        });
    }

    function sendToClient(payload) {
        chrome.storage.local.get(["server"], res => {
            _.post("http://" + res.server + ":49670/handler", {
                form: payload
            }).catch(err => {
                console.log(err);
            });
        });
    }

    handler.open = (payload, port) => {
        sendToClient(payload);
    };

    handler.read = (payload, port) => {
        console.log("READ ======> ", payload);
        sendToClient(payload);
    };

    handler.connection = (payload, port) => {
        if (payload.isStatus) {
            chrome.storage.local.get(["server"], res => {
                if (res.server) {
                    fetch("http://" + res.server + ":49670/check")
                        .then(response => {
                            port.postMessage({cmd: "connection", isConnected: true});
                        })
                        .catch(data => {
                            port.postMessage({cmd: "connection", isConnected: false});
                        });
                } else {
                    port.postMessage({cmd: "connection", isConnected: false});
                }
            });
        } else if (payload.isDisconnect) {
            chrome.storage.local.set({server: null});
        } else {
            chrome.storage.local.set({server: payload.server});
        }
    };

    handler.update = (payload, port) => {
        chrome.tabs.query({currentWindow: true, active: true }, (tabs = []) => {
            if (tabs.length) {
                const res = detector.getAll(tabs[0].id);
                const images = imageDetector.get(tabs[0].id);
                port.postMessage({cmd: "update", media: res, images: images});
            }
        });
    };

    handler.inapp = (payload, port) => {
        chrome.tabs.query({currentWindow: true, active: true }, (tabs = []) => {
            if (tabs.length) {
                const res = detector.getAll(tabs[0].id);
                const images = imageDetector.get(tabs[0].id);
                if (payload.type === "video") {
                    if (res.length !== 0) {
                        sendToClient({cmd: "inapp", app: "videoPlayer", data: JSON.stringify(res), currentTime: payload.currentTime});
                    }
                } else if (payload.type === "music") {
                    if (res.length !== 0) {
                        sendToClient({cmd: "inapp", app: "musicPlayer", data: JSON.stringify(res), currentTime: payload.currentTime});
                    }
                } else if (payload.type === "images") {
                    if (images.length !== 0) {
                        console.log(images);
                        sendToClient({cmd: "inapp", app: "imageSlider", data: JSON.stringify(images)});
                    }
                } else if (payload.type === "last-video") {
                    if (res.length !== 0) {
                        sendToClient({cmd: "inapp", app: "videoPlayer", data: JSON.stringify([res[res.length - 1]]), currentTime: payload.currentTime});
                    }
                } else if (payload.type === "last-music") {
                    if (res.length !== 0) {
                        sendToClient({cmd: "inapp", app: "musicPlayer", data: JSON.stringify([res[res.length - 1]]), currentTime: payload.currentTime});
                    }
                }
            }
        });
    };

    handler.update_fb = (payload, port) => {
        chrome.tabs.query({currentWindow: true, active: true }, (tabs = []) => {
            if (tabs.length) {
                detector.detect({type: "fb_video", videoId: payload.videoId, tabId: tabs[0].id})
            }
        });
    };

    // https://dmn92m25mtw4z.cloudfront.net/topic_vids/understand-graphs-of-linear-relations/understand-graphs-of-linear-relations-1a/v2/hls_480/480.m3u8
    chrome.extension.onConnect.addListener(function(port) {
        port.onMessage.addListener(function(payload) {
            try {
                handler[payload.cmd](payload, port);
            } catch(e) {}
        });
   });


   onCreate();

})();