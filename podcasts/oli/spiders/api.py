import requests
import time
import json
from bs4 import BeautifulSoup
import os
from selenium import webdriver
from datetime import datetime


def fetch_podcast():
    offset = 0
    counter = 0
    while True:
        url = 'https://listen-api.listennotes.com/api/v2/search?q=tamil&sort_by_date=0&type=podcast&offset={}&len_min=1&len_max=300&published_before={}&published_after=0&only_in=title%2Cdescription&language=Tamil&safe_mode=0'.format(offset, time.time() * 1000)
        headers = {
        'X-ListenAPI-Key': '2f35ee1e850548339924734e7b1d6be7',
        }
        response = requests.get(url, headers=headers)

        if response.json()["count"] == 0:
            break

        with open("podcasts/podcast_{}.json".format(counter), "wb+") as f:
            f.write(response.content)
        
        counter += 1
        offset += 10


def clean():
    podcasts = []
    for root, dirs, files in os.walk("podcasts"):
        for fil in files:
            path = "{}/{}".format(root, fil)
            with open(path, "rt") as f:
                data = json.loads(f.read())
                podcasts.extend(data["results"])

    with open("podcasts.json", "wb+") as f:
        f.write(json.dumps(podcasts).encode("utf-8"))


def update_rss_feed():
    with open("podcasts.json", "rt") as f:
        podcasts = json.loads(f.read())

    for podcast in podcasts:
        resp = requests.get(podcast["listennotes_url"], headers={"user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36 Edg/84.0.522.52"})
        soup = BeautifulSoup(resp.content, "html.parser")

        podcast["rss"] = soup.select("[data-channel-rss]")[0]["data-channel-rss"]
        del podcast["email"]
        print(podcast["rss"])
    
    with open("podcasts.json", "w+") as f:
        f.write(json.dumps(podcasts))


def clean_rss_feed():
    with open("podcasts.json", "rt") as f:
        podcasts = json.loads(f.read())

    data = []
    for podcast in podcasts:
        if podcast["rss"].startswith("https://anchor.fm"):
            data.append(podcast)
        print(podcast["rss"])

    with open("podcasts.json", "w+") as f:
        f.write(json.dumps(podcasts))


def fetch_episodes():
    with open("podcasts.json", "rt") as f:
        podcasts = json.loads(f.read())

    browser = webdriver.Chrome("./chromedriver.exe")
    
    eps = []

    for podcast in podcasts:
        print("* Going to '{}'".format(podcast["listennotes_url"]))
        browser.get(podcast["listennotes_url"])
        time.sleep(5)
        print("* Page loading finished")
        counter = 0
        while True:
            if counter > 50:
                break

            is_exists = browser.execute_script("""
                var loadMoreBtn = document.querySelectorAll(".ln-channel-episode-load-more-button-container")[1];
                if (loadMoreBtn == undefined) {
                    return false;
                } else {
                    loadMoreBtn.querySelector("button").click();
                    return true;
                }
            """)
            if not is_exists:
                break
            print(" * Loading more...")
            counter += 1
            time.sleep(5)

        while True:
            is_exists = browser.execute_script("""
                var el = document.querySelectorAll(".ln-channel-episode-detail-card .ln-channel-episode-card-body")[0].parentElement.querySelector("a[aria-label='MORE']");
                return el != undefined
            """)
            if is_exists:
                break
            time.sleep(0.1)

        titles = browser.execute_script("""
            var els = document.querySelectorAll(".ln-channel-episode-detail-card .ln-channel-episode-card-body");
            var titles = [];
            for (var i = 0; i < els.length; i++) {
                var el = els[i].parentElement;
                el.querySelector("a[aria-label='MORE']").click();
                var title = el.querySelector(".ln-channel-episode-card-info-title").textContent.trim();
                titles.push(title);
            }
            return titles;
        """)

        audios = browser.execute_script("""
            var els = document.querySelectorAll(".ln-channel-episode-detail-card .ln-channel-episode-card-body");
            var audios = [];
            for (var i = 0; i < els.length; i++) {
                var el = els[i].parentElement;
                var audio = el.querySelector("a[download]").href;
                audios.push(audio);
            }
            return audios;
        """)

        _dates = browser.execute_script("""
            var els = document.querySelectorAll(".ln-channel-episode-detail-card .ln-channel-episode-card-body");
            var dates = [];
            for (var i = 0; i < els.length; i++) {
                var el = els[i].parentElement;
                var d = el.querySelector("time").getAttribute("datetime");
                dates.push(d);
            }
            return dates;
        """)
        dates = []
        for d in _dates:
            s = d.split("T")[0]
            date = datetime.strptime(s, "%Y-%m-%d")
            dates.append(date.strftime("%b %d, %Y"))

        for i, _ in enumerate(titles):
            eps.append({
                "title": titles[i],
                "audio": audios[i],
                "date": dates[i],
                "channel_id": podcast["id"]
            })

            print(eps[-1])

    with open("episodes.json", "w+") as f:
        f.write(json.dumps(eps))


def update_category():
    with open("podcasts.json", "rt") as f:
        podcasts = json.loads(f.read())

    cats = []

    for podcast in podcasts:
        resp = requests.get(podcast["listennotes_url"])
        soup = BeautifulSoup(resp.content, "html.parser")
        category = soup.find_all("div", {"class": "ln-channel-genre-tag"})[0].text.strip()

        if category not in cats:
            cats.append(category)

        podcast["category"] = category
        print(category)

    
    with open("podcasts.json", "w+") as f:
        f.write(json.dumps(podcasts))

    with open("oli_categories.json", "w+") as f:
        f.write(json.dumps(cats))


if __name__ == "__main__":
    update_category()
