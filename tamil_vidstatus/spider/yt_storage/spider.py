import json
import requests
import re
import sys

from spidy.spidy import Spidy


class Spider:
    def __init__(self):
        with open("filters.json", "rb") as f:
            self.filters = json.loads(f.read())

    def fetch(self, url):
        home_page = Spidy(url)
        script = None
        for s in home_page.css("script"):
            if s.text().find('window["ytInitialData"]') > -1:
                script = s.text()
                break

        data = json.loads((script.split("};")[0] + "}").replace('window["ytInitialData"] = ', ""))
        data = data["contents"]["twoColumnWatchNextResults"]["playlist"]["playlist"]["contents"]

        print("* Total videos found: {}".format(len(data)))
        f = open("categories.json", "rt")
        categories = json.loads(f.read())
        f.close()
        for i, cat in enumerate(categories):
            print("{}. {}".format(i+1, cat))
        category = categories[int(input("\nSelect: ")) - 1]
        videos = []
        for d in data:
            v = d["playlistPanelVideoRenderer"]
            # with open("test.json", "wb+") as f:
            #     f.write(json.dumps(d).encode("utf-8"))
            video_id = v["videoId"]

            try:
                title = v["title"]["simpleText"].lower()
            except:
                continue
            for emojie in self.filters:
                title = title.replace(emojie, " ")
            title = re.sub(r"[ ]{2,}", " ", title).title().strip()
            print("* Adding '{}'".format(title))

            videos.append({
                "download_count": 0,
                "share_count": 0,
                "view_count": 0,
                "tags": [],
                "video_id": video_id,
                "title": title,
                "category": category
            })

        return videos


if __name__ == "__main__":
    spider = Spider()
    spider.fetch("https://www.youtube.com/watch?v=vbt4kS5Ctu8&list=PLHvWPITggrq1gRYGq5CnNa6K9OPK3CzHc")
