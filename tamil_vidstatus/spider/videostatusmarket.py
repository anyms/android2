import requests
from bs4 import BeautifulSoup
import json

from spidy.spidy import Spidy


class Spider:
    def __init__(self):
        pass

    def run(self):
        # videostatusmarket
        data = []
        headers = {
            "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.89 Safari/537.36 Edg/84.0.522.40",
        }
        for page in range(1, 8+1):
            print("* Fetching page: {}".format(page))
            resp = requests.get("https://www.videostatusmarket.com/full-screen-video/tamil-status-videos-download/{}".format(page), headers=headers)
            soup = Spidy(resp)
            titles = soup.css(".card .title").text()
            thumbs = soup.css(".card img").attr("src")

            links = soup.css(".card a.title").attr("href")

            for i, link in enumerate(links):
                resp = requests.get(link, headers=headers)
                soup = Spidy(resp)

                video = soup.css("video source").attr("src")
                tags = []
                for tag in soup.css("#myList button").text():
                    tags.append(tag.strip())
                print("* Video: {}".format(video))
                data.append({
                    "title": titles[i],
                    "thumb": thumbs[i],
                    "video": video,
                    "tags": tags
                })

        f = open("videostatusmarket.json", "wb+")
        f.write(json.dumps(data).encode("utf-8"))
        f.close()
        print("* Done.")


    def test_read(self):
        f = open("test.html", "rb")
        content = f.read()
        f.close()
        return content

    def test_write(self, content):
        with open("test2.html", "wb+") as f:
            f.write(content)


if __name__ == "__main__":
    spider = Spider()
    spider.run()
