const axios    = require("axios");
const cheerio  = require("cheerio");
const _        = require("domlang");

const agents   = require("../data/agents");


class EpisodeFetcher {
    constructor(channel) {
        this.channel = channel
        this.userAgent = _.random(agents).ua
    }

    getSoup(url) {
        const self = this;
        let promise = new Promise((resolve, reject) => {
            const headers = {
                "User-Agent": self.userAgent,
                "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
            };
            axios.get(url, {headers: headers}).then(res => {
                resolve(cheerio.load(res.data));
            }).catch(err => {
                reject(err);
            });
        });

        return promise;
    }

    parseSoup($) {
        const card = $(".ln-channel-episode-detail-card").eq(2);
        const title = card.find(".ln-channel-episode-card-info-title").text().trim()
        const duration = card.find(".ln-episode-timestamp").eq(0).text().trim()
        const date = card.find(".ln-channel-episode-card-info-subtitle time").attr("datetime");
        const audio = card.find("div[data-type='episode-audio-player']").attr("data-audio");

        return {
            title: title,
            duration: duration,
            date: date,
            audio: audio
        };
    }

    run(callback) {
        return this.getSoup(encodeURI(this.channel.url)).then($ => {
            callback(this.parseSoup($));
        }).catch(err => {
            console.error(err);
        });
    }
}

module.exports = EpisodeFetcher;

// for (channel of channels) {
//     new EpisodeFetcher(channel).run(ep => {
//         console.log(ep);
//     });
// }
