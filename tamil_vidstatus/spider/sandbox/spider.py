import json
import urllib.parse as urlparse
from urllib.parse import parse_qs
import requests

from spidy.spidy import Spidy


class Spider:
    def __init__(self):
        pass

    def get_all_from_category(self, category):
        data = []
        soup = Spidy("https://tamil.statusdp.com/whatsapp-status-videos/{}.html".format(category.lower()))
        for d in self.parse_data(category, soup):
            data.append(d)

        needle = 24
        while True:
            resp = requests.post("https://tamil.statusdp.com/videos/getData.php", data={"folder": "files/{}/".format(category.title()), "acttype": 0, "row": needle})
            soup = Spidy(resp)
            parsed = self.parse_data(category, soup)
            if not parsed:
                break

            for d in parsed:
                data.append(d)
            needle *= 2

        print("Total statuses: {}".format(len(data)))
        return data


    def parse_data(self, category, soup):
        containers = soup.css("a[href^='/whatsapp-status-video']")
        try:
            links = containers.css("img.full").attr("src")
        except KeyError:
            links = containers.css("img.full").attr("data-src")
        titles = containers.css(".vidtitle").text()
        durations = containers.css(".duration").text()

        data = []
        for i, link in enumerate(links):
            try:
                uId = int(link.split("/")[-1].split(".")[0])
                d = {
                    "uId": uId,
                    "video": "https://tamil.statusdp.com/videos/files/{}/{}.mp4".format(category.title(), uId),
                    "title": titles[i].strip(),
                    "duration": durations[i].strip(),
                    "category": category.lower()
                }

                data.append(d)
            except (IndexError, ValueError):
                continue

        return data


if __name__ == "__main__":
    spider = Spider()
    cats = [
        "action",
        "bgm",
        "comedy",
        "devotional",
        "dialogues",
        "friendship",
        "general",
        "life",
        "love",
        "love_failure",
        "sentimental",
        "motivational",
    ]
    data = []
    for cat in cats:
        print("Fetching category '{}'".format(cat))
        for d in spider.get_all_from_category(cat):
            data.append(d)
    
    f = open("data.json", "wb+")
    f.write(json.dumps(data).encode("utf-8"))
    f.close()



ffmpeg -i v.mp4 -vf drawtext="fontfile=font.ttf: text='Tamil VidStatus': fontcolor=white: fontsize=30: box=1: boxcolor=black@0.8: boxborderw=5: x=(15): y=(h-text_h/0.65)" -codec:a copy output.mp4

