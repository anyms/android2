(function() {
    function onCreate() {
        chrome.runtime.onMessage.addListener(payload => {
            if (payload.cmd === "connection") {
                if (payload.isConnected) {
                    _("#connectBtn").text("Disconnect").css("background-color", "#ED5153");
                    _("#controls .indicator").removeClass("offline").text("Live");
                } else {
                    _("#connectBtn").text("Connect").css("background-color", "#16A085");
                    _("#controls .indicator").addClass("offline").text("Offline");
                }
            } else if (payload.cmd === "update") {
                _(".menuItem.detect .count").text(payload.media.length);
                _(".menuItem.detect .count").text(payload.media.length);
                _(".menuItem.detect-images .count").text(payload.images.length);
            }
        });

        _(".menuItem").on("click", function() {
            window.close();
            const action = this.attr("data-action");
    
            switch(action) {
                case "open":
                    sendToContentScript({cmd: "open"});
                    break
                case "read":
                    sendToContentScript({cmd: "read"});
                    break
                case "video":
                    sendToContentScript({cmd: "inapp", type: "video"});
                    break
                case "last-video":
                    sendToContentScript({cmd: "inapp", type: "last-video"});
                    break
                case "music":
                    sendToContentScript({cmd: "inapp", type: "music"});
                    break
                case "last-music":
                    sendToContentScript({cmd: "inapp", type: "last-music"});
                    break
                case "images":
                    sendToContentScript({cmd: "inapp", type: "images"})
                    break
            }
        });

        setInterval(() => {
            updateControls();
        }, 500);
        updateControls();
    }

    function updateControls() {
        sendToContentScript({cmd: "connection", isStatus: true, isDisconnect: false});
        sendToContentScript({cmd: "update"});
    }
    
    
    function sendToContentScript(payload) {
        chrome.tabs.query({active: true, currentWindow: true}, function(tabs) {
            const tab = tabs[0];
            payload.tab = tab;
            payload.url = tab.url;
            payload.encodedUrl = encodeURIComponent(payload.url);
            chrome.tabs.sendMessage(tab.id, payload);
        });
    }


    _("#connectBtn").on("click", function() {
        window.close();
        if (this.text() === "Connect") {
            sendToContentScript({cmd: "connection", isDisconnect: false, isStatus: false});
        } else {
            sendToContentScript({cmd: "connection", isDisconnect: true, isStatus: false});
        }
    });


    onCreate();

})();