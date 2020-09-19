import requests
import json
from bs4 import BeautifulSoup

from spidy.spidy import Spidy


class Extractor:
    def __init__(self):
        with open("data.json", "rb") as f:
            self.data = json.loads(f.read())

        self.tried_titles = []

    def fetch_movie_info(self, url):
        print("* IMDb: {}".format(url))
        soup = Spidy(url)

        poster = soup.css("div.poster img").attr("src")
        year = soup.css("#titleYear a").text().strip()        
        try:
            duration = soup.css(".subtext time").text().strip()
        except:
            duration = None
        genres = soup.css(".subtext a[href*='genres']").text()
        if type(genres) is not list:
            genres = [genres]
        
        
        print()
        print("Poster: {}".format(poster))
        print("Year: {}".format(year))
        print("Duration: {}".format(duration))
        print("Genres: {}".format(genres))
        print()

    def run(self):
        for v in self.data:
            title = v["title"].split("|")[0].split("-")[0].strip()
            
            if title not in self.tried_titles:
                ctitle = title
                print("* Fetching {} ({})".format(ctitle, v["video_id"]))
                if not self.is_ascii(ctitle):
                    ctitle = input("Enter the title in English: ")

                link = self.get_imdb_link(ctitle)
                # if link is None:
                #     print("\nTitle: {}\nVideo ID: {}".format(title, v["video_id"]))
                #     ctitle = input("Enter the title in English: ")
                #     if ctitle.strip() == "":
                #         continue
                #     link = self.get_imdb_link(ctitle)

                if link is None:
                    continue
                
                self.fetch_movie_info(link)

            self.tried_titles.append(title)

    def get_imdb_link(self, title):
        resp = requests.get("https://www.google.com/search?q={}+tamil+movie+imdb".format(title.replace(" ", "+")))
        soup = BeautifulSoup(resp.content, "html.parser")
        anchors = soup.find_all("a")
        for anchor in anchors:
            link = anchor["href"]
            if link.startswith("/url?q="):
                link = link[7:].split("&")[0]
                if link.startswith("https://www.imdb.com"):
                    return link
        return None

    def is_ascii(self, s):
        return all(ord(c) < 128 for c in s)


if __name__ == "__main__":
    extractor = Extractor()
    extractor.run()
