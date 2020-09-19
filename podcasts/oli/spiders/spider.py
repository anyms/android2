from selenium import webdriver
import json
from time import sleep


class Spider:
    def __init__(self):
        print(" * initiating the spider")
        self.urls = [
            "https://www.listennotes.com/search/?q=tamil&sort_by_date=0&scope=podcast&offset=0&only_in=author,title&date_filter=any&language=Any%20language&country=Any%20region&age_filter=any&ecount_min=0&ecount_max=0",
            "https://www.listennotes.com/search/?q=tamil&sort_by_date=0&scope=podcast&offset=10&only_in=author,title&date_filter=any&language=Any%20language&country=Any%20region&age_filter=any&ecount_min=0&ecount_max=0",
            "https://www.listennotes.com/search/?q=tamil&sort_by_date=0&scope=podcast&offset=20&only_in=author,title&date_filter=any&language=Any%20language&country=Any%20region&age_filter=any&ecount_min=0&ecount_max=0",
            "https://www.listennotes.com/search/?q=tamil&sort_by_date=0&scope=podcast&offset=30&only_in=author,title&date_filter=any&language=Any%20language&country=Any%20region&age_filter=any&ecount_min=0&ecount_max=0",
            "https://www.listennotes.com/search/?q=tamil&sort_by_date=0&scope=podcast&offset=40&only_in=author,title&date_filter=any&language=Any%20language&country=Any%20region&age_filter=any&ecount_min=0&ecount_max=0",
            "https://www.listennotes.com/search/?q=tamil&sort_by_date=0&scope=podcast&offset=50&only_in=author,title&date_filter=any&language=Any%20language&country=Any%20region&age_filter=any&ecount_min=0&ecount_max=0",
            "https://www.listennotes.com/search/?q=tamil&sort_by_date=0&scope=podcast&offset=60&only_in=author,title&date_filter=any&language=Any%20language&country=Any%20region&age_filter=any&ecount_min=0&ecount_max=0",
            "https://www.listennotes.com/search/?q=tamil&sort_by_date=0&scope=podcast&offset=70&only_in=author,title&date_filter=any&language=Any%20language&country=Any%20region&age_filter=any&ecount_min=0&ecount_max=0"
        ]
        self.browser = webdriver.Chrome("./chromedriver")
        self.channels = []
        self.added_channels = []

        f = open("data/episodes.json", "rt")
        eps = json.loads(f.read())
        f.close()

        for key, _ in eps:
            self.added_channels.append(key)

    def crawl(self):
        # for url in self.urls:
        #     self.browser.get(url)
        #     input("Do you want to get channels? ")
        #     channels = self.browser.execute_script("""
        #         var channels = document.querySelectorAll(".ln-search-results-card:not(.row):not(.ln-desktop-only):not(.ln-mobile-only)");
        #         var data = [];
        #         for (var i = 0; i < channels.length; i++) {
        #             var channel = channels[i];
        #             var title = channel.querySelector(".ln-episode-card-title").textContent.trim();
        #             var author = channel.querySelector(".ln-search-results-author").textContent.trim();
        #             var image = channel.querySelector(".ln-episode-image-medium").src;
        #             var url = channel.querySelector(".ln-episode-card-title").href;

        #             data.push({
        #                 title: title,
        #                 author: author,
        #                 image: image,
        #                 url: url
        #             });
        #         }
        #         return data;
        #     """)

        #     for channel in channels:
        #         can_add = input("Do you want to add '{}'? (Y/n) ".format(channel["title"]))

        #         if can_add.strip() == "" or can_add.lower() == "y" or can_add.lower() == "yes":
        #             print("'{}' added.".format(channel["title"]))
        #             self.channels.append(channel)
        
        # f = open("data/channels.json", "wb+")
        # f.write(json.dumps(self.channels, ensure_ascii=False).encode("utf-8"))
        # f.close()
        f = open("data/channels.json", "rt")
        for channel in json.loads(f.read()):
            self.channels.append(channel)
        f.close()
        self.crawl_episodes()


    def write_episode(self, channel_url, episodes):
        f = open("data/episodes.json", "rb")
        data = json.loads(f.read().decode("utf-8"))
        f.close()
        data[channel_url] = episodes
        f = open("data/episodes.json", "wb+")
        f.write(json.dumps(data, ensure_ascii=False).encode("utf-8"))
        f.close()


    def crawl_episodes(self):
        loading_btn_id = "div.ln-channel-episode-load-more-button-container > button.btn-lg"
        data = {}

        for channel in self.channels:
            if channel["url"] in self.added_channels:
                continue

            self.browser.get(channel["url"])
            # self.wait_for(loading_btn_id)

            while self.is_exist(loading_btn_id):
                self.click(loading_btn_id)
                sleep(3)

            episodes = []
            # fetching latest episode
            episodes.append(self.extract_latest_episode_details())
        
            for ep in self.extract_episodes_details():
                episodes.append(ep)

            for i, ep in enumerate(episodes):
                print(" * extracting audio for : {}".format(ep["title"]))
                self.browser.get(ep["audio"])
                sleep(1)
                episodes[i]["audio"] = self.extract_audio()

            self.write_episode(channel["url"], episodes)
        
        # f = open("data.json", "wb+")
        # f.write(json.dumps(data, ensure_ascii=False).encode("utf-8"))
        # f.close()

    def extract_audio(self):
        return self.browser.execute_script("""
            return document.querySelector("#episode-play-button-toolbar").getAttribute("data-audio");
        """)

    def extract_episodes_details(self):
        print(" * extracting episodes")
        return self.browser.execute_script("""
            var data = [];
            var episodes = document.querySelectorAll(".ln-channel-individual-episode-card");

            for (var i = 0; i < episodes.length; i++) {
                var episode = episodes[i];
                var title = episode.querySelector(".ln-channel-episode-card-info-title").textContent.trim();
                var date = episode.querySelector(".ln-date-text").getAttribute("datetime");
                var duration = episode.querySelector(".ln-episode-timestamp").textContent.trim();
                var audio = episode.querySelector(".ln-channel-episode-card-info-title a").getAttribute("href");
                data.push({
                    id: 0,
                    title: title,
                    date: date,
                    duration: duration,
                    audio: audio
                });
            }
            return data;
        """)
    
    def extract_latest_episode_details(self):
        print(" * extracting latest episode")
        return self.browser.execute_script("""
            var episode = document.querySelectorAll(".ln-channel-episode-detail-card")[2];
            var title = episode.querySelector(".ln-channel-episode-card-info-title").textContent.trim();
            var date = episode.querySelector(".ln-date-text").getAttribute("datetime");
            var duration = episode.querySelector(".ln-episode-timestamp").textContent.trim();
            var audio = episode.querySelector(".ln-channel-episode-card-info-title a").getAttribute("href");
            return {
                id: 0,
                title: title,
                date: date,
                duration: duration,
                audio: audio
            };
        """)
        

    def click(self, identifier):
        self.browser.execute_script("""
            document.querySelector("{}").click();
        """.format(identifier))

    def is_exist(self, identifier):
        return self.browser.execute_script("""
            return document.querySelectorAll("{}").length > 0;
        """.format(identifier))

    def wait_for(self, identifier):
        while True:
            is_exist = self.browser.execute_script("""
                return document.querySelectorAll("{}").length > 0;
            """.format(identifier))
            if is_exist:
                break
            sleep(1)


if __name__ == "__main__":
    spider = Spider()
    spider.crawl()
