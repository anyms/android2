class FacebookVideoDetector {
    constructor() {
        this.detectedUrls = {};
    }

    run(payload) {
        if (this.detectedUrls[payload.tabId] === undefined) {
            this.detectedUrls[payload.tabId] = [];
        }
        if (!this.detectedUrls[payload.tabId].includes(payload.url)) {
            fetch("https://facebook.com/watch/?v=" + payload.videoId).then(res => {
                res.text().then(text => {
                    const rx = /{"prefetch_dash_segments":\[(.+?)\]\,"is_final":/g;
                    const arr = rx.exec(text);
                    if (arr.length >= 2) {
                        const dat = JSON.parse(arr[1]);
                        this.detectedUrls[payload.tabId].push({vSrc: dat.video[0].url, aSrc: dat.audio[0].url, title: ""});
                    }
                }).catch(console.log);
            }).catch(console.log);
        }
    }

    clear(tabId) {
        if (this.detectedUrls[tabId] === undefined) {
            this.detectedUrls[tabId] = [];
        }
        this.detectedUrls[tabId] = [];
    }

    get(tabId) {
        if (this.detectedUrls[tabId] === undefined) {
            this.detectedUrls[tabId] = [];
        }
        return this.detectedUrls[tabId];
    }
}