class GoogleVideoDetector {
    constructor() {
        this.detectedUrls = {};
        this.videoIds = [];
        this.audioIds = [];
    }

    run(payload, isAudio) {
        const self = this;
        if (this.detectedUrls[payload.tabId] === undefined) {
            this.detectedUrls[payload.tabId] = [];
        }
        
        const url = new URL(payload.url);
        const params = url.searchParams;

        if (!params.has("range") || !params.has("id")) {
            return
        }

        const id = params.get("id");
        params.set("range", "0-9999999999999");

        if (!this.detectedUrls[payload.tabId].includes(payload.url)) {
            if (!this.videoIds.includes(id) && !isAudio) {
                this.videoIds.push(id);
                console.log(payload.url);
                this.detectedUrls[payload.tabId].push({
                    "vSrc": url.toString(),
                    "id": id,
                    "title": payload.title
                })
            } else if (isAudio) {
                if (this.videoIds.includes(id) && !this.audioIds.includes(id)) {
                    this.audioIds.push(id);

                    for (let i = 0; i < this.detectedUrls[payload.tabId].length; i++) {
                        if (this.detectedUrls[payload.tabId][i].id === id) {
                            this.detectedUrls[payload.tabId][i].aSrc = url.toString();
                            break
                        }
                    }
                }
            }
        }
    }

    clear(tabId) {
        if (this.detectedUrls[tabId] === undefined) {
            this.detectedUrls[tabId] = [];
        }
        this.detectedUrls[tabId] = [];
        console.log("cleared");
    }

    get(tabId) {
        if (this.detectedUrls[tabId] === undefined) {
            this.detectedUrls[tabId] = [];
        }
        return this.detectedUrls[tabId];
    }
}