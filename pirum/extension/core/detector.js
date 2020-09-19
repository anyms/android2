class Detector {
    constructor() {
        this.m3u8Detector = new M3U8Detector();
        this.googleVideoDetector = new GoogleVideoDetector();
        this.facebookVideoDetector = new FacebookVideoDetector();
        this.videoDetector = new VideoDetector();
        this.musicDetector = new MusicDetector();

        this.checkedUrls = {};
    }

    getAll(tabId) {
        const media = [];

        for (const v of this.videoDetector.get(tabId)) {
            console.log(v);
            media.push({
                "src": encodeURIComponent(v.src),
                "type": "join",
                "title": encodeURIComponent(v.title)
            });
        }

        for (const v of this.m3u8Detector.get(tabId)) {
            media.push({
                "src": encodeURIComponent(v.src),
                "type": "stream",
                "title": encodeURIComponent(v.title)
            });
        }

        for (const v of this.musicDetector.get(tabId)) {
            media.push({
                "src": encodeURIComponent(v.src),
                "type": "join",
                "title": v.title
            });
        }

        for (const v of this.googleVideoDetector.get(tabId)) {
            if (v.aSrc !== undefined) {
                media.push({
                    "vSrc": encodeURIComponent(v.vSrc),
                    "aSrc": encodeURIComponent(v.aSrc),
                    "type": "separate",
                    "title": encodeURIComponent(v.title)
                });
            }
        }

        for (const v of this.facebookVideoDetector.get(tabId)) {
            if (v.aSrc !== undefined) {
                media.push({
                    "vSrc": encodeURIComponent(v.vSrc),
                    "aSrc": encodeURIComponent(v.aSrc),
                    "type": "separate",
                    "title": encodeURIComponent(v.title)
                });
            }
        }

        return media;
    }

    clearTab(tabId) {
        if (this.checkedUrls[tabId] === undefined) {
            this.checkedUrls[tabId] = [];
        }
        this.checkedUrls[tabId] = [];

        this.m3u8Detector.clear(tabId);
        this.googleVideoDetector.clear(tabId);
        this.facebookVideoDetector.clear(tabId);
        this.videoDetector.clear(tabId);
        this.musicDetector.clear(tabId);
    }

    run(type, payload) {
        const self = this;
        
        if (type === "m3u8") {
            const xhttp = new XMLHttpRequest();
            xhttp.onreadystatechange = function() {
                if (this.readyState == 4 && this.status == 200) {
                    const lines = this.responseText.split("\n");
                    for (const line of lines) {
                        if (!line.startsWith("#") && line.trim() !== "") {
                            const plainLine = line.split("?")[0];
                            if (!plainLine.endsWith(".m3u8")) {

                                chrome.tabs.query({lastFocusedWindow: true, active: true}, function (tabs) {
                                    payload.title = tabs[0].title;
                                    self.m3u8Detector.run(payload);
                                });
                            
                                break
                            }
                        }
                    }
                }
            };
            xhttp.open("GET", payload.url, true);
            xhttp.send();
            
            return
        }

        const xhttp = new XMLHttpRequest();
        xhttp.onreadystatechange = function() {
            if (this.readyState == 4 && this.status == 200) {
                switch (type) {
                    case "video":
                        chrome.tabs.query({lastFocusedWindow: true, active: true}, function (tabs) {
                            payload.title = tabs[0].title;
                            self.videoDetector.run(payload);
                        });
                        break
                    case "music":
                        chrome.tabs.query({lastFocusedWindow: true, active: true}, function (tabs) {
                            payload.title = tabs[0].title;
                            self.musicDetector.run(payload);
                        });
                        break
                    case "google_video":
                        chrome.tabs.onUpdated.addListener(function (tabId, changeInfo, tab) { 
                            if (changeInfo.title !== undefined) {
                                payload.title = changeInfo.title;
                                self.googleVideoDetector.run(payload, false);                                
                            }
                        });
                        break
                    case "google_audio":
                        self.googleVideoDetector.run(payload, true);
                        break
                }
            }
        };
        xhttp.open("HEAD", payload.url, true);
        xhttp.send();
    }

    detect(payload) {
        if (payload.type !== undefined) {
            if (payload.type === "fb_video") {
                this.facebookVideoDetector.run(payload);
                return;
            }
        }

        const plainUrl = payload.url.split("://")[1].split("?")[0].split("/")[0];
        const contentLength = payload.responseHeaders["content-length"];
        const contentType = payload.responseHeaders["content-type"];
        
        if (this.checkedUrls[payload.tabId] === undefined) {
            this.checkedUrls[payload.tabId] = [];
        }

        if (contentType === undefined || contentLength === undefined || this.checkedUrls[payload.tabId].includes(payload.url) || payload.tabId === -1) {
            return
        }

        if (plainUrl.includes("fbcdn.net") && (plainUrl.includes("video.") || plainUrl.includes("/v/") )) {
            this.checkedUrls[payload.tabId].push(payload.url);
            this.run("facebook", payload)
        } else if (contentLength > 0 && !contentType.toLowerCase().startsWith("video/mp2t") && (contentType.startsWith("video/") ||  
                    contentType.startsWith("audio/") ||
                    contentType.toLowerCase().startsWith("application/x-mpegurl") ||
                    contentType.toLowerCase().startsWith("application/vnd.apple.mpegurl"))) {
            
            if (contentType.toLowerCase().startsWith("application/x-mpegurl") ||
                contentType.toLowerCase().startsWith("application/vnd.apple.mpegurl")) {  
                this.checkedUrls[payload.tabId].push(payload.url);
                this.run("m3u8", payload)
            } else if (contentType.startsWith("video/")) {
                this.checkedUrls[payload.tabId].push(payload.url);
                if (plainUrl.includes("googlevideo.com")) {
                    this.run("google_video", payload)
                } else {
                    this.run("video", payload)
                }
            } else if (contentType.startsWith("audio/")) {
                this.checkedUrls[payload.tabId].push(payload.url);
                if (plainUrl.includes("googlevideo.com")) {
                    this.run("google_audio", payload)
                } else {
                    this.run("music", payload)
                }
            }

        }
    }
}