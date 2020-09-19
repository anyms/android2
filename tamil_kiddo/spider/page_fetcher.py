import requests
import json
from bs4 import BeautifulSoup


def main():
    level = 4

    with open("books{}.json".format(level), "rb") as f:
        books = json.loads(f.read())

    data = []


    for book in books:
        book["pages"] = []
        print(book["slug"])
        url = "https://storyweaver.org.in/api/v1/stories/{}/read?&ignore_count=false".format(book["slug"])
        try:
            resp = requests.get(url).json()["data"]
        except:
            continue

        book["orientation"] = resp["orientation"]
        resp["pages"] = resp["pages"][1:][:-3]

        print(book["title"])
        for i, page in enumerate(resp["pages"]):
            page["html"] = convert_html(page["html"])
            book["pages"].append(page["html"])

        data.append(book)

    with open("level{}.json".format(level), "wb+") as f:
        f.write(json.dumps(data).encode("utf-8"))


def convert_html(html):
    soup = BeautifulSoup(html, "html.parser")
    for s in soup.select("script, svg, .page_number, #pb-dictionary-loder"):
        s.decompose()
    
    style_link = soup.new_tag("link")
    style_link["rel"] = "stylesheet"
    style_link["href"] = "style.css"

    style = soup.new_tag("style")
    style.append("""
    @media only screen and (min-width: 960px) {
        .page-container-landscape {
            width: inherit;
            height: inherit;
        }
    }
    """)

    script = soup.new_tag("script")
    script.append("""
    const reader = document.querySelector("#storyReader");
    const texts = document.querySelectorAll(".newStories");
    const offsetTop = texts[1].offsetTop + texts[1].clientHeight;
    const bottom = reader.clientHeight - offsetTop;
    console.log("offsetTop", offsetTop);
    console.log("bottom", bottom);

    if (bottom < 0) {
        texts[1].style.top = "auto";
        texts[1].style.bottom = "10px";
    }
    """)
    soup.find("div", {"id": "storyReader"}).append(script)
    soup.find("div", {"id": "storyReader"}).append(style_link)
    soup.find("div", {"id": "storyReader"}).append(style)

    img = soup.find("img", {"class": "responsive_illustration"})
    if img is not None:
        img["src"] = img["data-size4-src"]

    return str(soup)

if __name__ == "__main__":
    main()
