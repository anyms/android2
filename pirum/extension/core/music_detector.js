class MusicDetector {
    constructor() {
        this.detectedUrls = {};
    }

    run(payload) {
        if (this.detectedUrls[payload.tabId] === undefined) {
            this.detectedUrls[payload.tabId] = [];
        }
        if (!this.detectedUrls[payload.tabId].includes(payload.url)) {
            this.detectedUrls[payload.tabId].push({src: payload.url, title: payload.title});
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