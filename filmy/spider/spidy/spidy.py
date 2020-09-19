import requests
from bs4 import BeautifulSoup
from urllib.parse import unquote as decode_url

from spidy.selector import Selector


class Spidy:
    def __init__(self, url, headers={}, cookies={}, timeout=10):
        if type(url) is BeautifulSoup:
            self.url = None
            self.soup = url
            self.status_code = None
            self.headers = None
            self.cookies = None
            self.ok = None
            self.reason = None
            self.content = None
            self.title = Selector(self.soup.title)
        else:
            if type(url) is requests.models.Response:
                self.url = url.url
                resp = url
            else:
                self.url = url
                resp = requests.get(url, headers=headers, cookies=cookies, timeout=timeout)
            self.soup = BeautifulSoup(resp.content, "html.parser")
            self.status_code = resp.status_code
            self.headers = resp.headers
            self.cookies = resp.cookies
            self.ok = resp.ok
            self.reason = resp.reason
            self.content = resp.content
            self.title = Selector(self.soup.title)

    def __repr__(self):
        return str(self.soup)

    def css(self, selector):
        els = []

        sels = self.soup.select(selector)
        for sel in sels:
            els.append(sel)

        if len(els) == 1:
            return Selector(els[0])
        else:
            return Selector(els)


# if __name__ == "__main__":
#     spidy = Spidy("https://www.google.com/search?q=Prithiv+tamilbotnet&num=100")
    
#     print(spidy.css("script")[0].text())
    
    # links = spidy.css("a[href^='/url?q=']").attr("href")
    # emails = []

    # for link in links:
    #     url = decode_url(link.split("/url?q=")[-1])
    #     print("Navigating to '{}'".format(url))
    #     try:
    #         spidy = Spidy(url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36 Edg/83.0.478.61"})
    #     except KeyboardInterrupt:
    #         break
    #     except:
    #         continue
    #     spidy.css("script").dispose()
    #     matches = spidy.css("body *").regex(r"[a-z0-9](?!.*?[^\na-z0-9]{2})[^\s@]+@[^\s@]+\.[^\s@]+[a-z0-9]")
    #     for match in matches:
    #         emails.append(match)

    # print("\n\nEmails:\n\n")
    # for email in set(emails):
    #     print(email)
