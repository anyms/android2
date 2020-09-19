import requests
import json

page_num = 1
books = []
level = 4

while True:
    print("* Fetching page {}".format(page_num))
    url = "https://storyweaver.org.in/api/v1/books-search?languages[]=Tamil&levels[]={}&page={}&per_page=24&sort=Relevance".format(level, page_num)
    resp = requests.get(url).json()

    if resp["data"]:
        for book in resp["data"]:
            books.append({
                "title": book["title"],
                "level": book["level"],
                "slug": book["slug"],
                "coverImage": book["coverImage"]["sizes"][1]["url"],
                "authors": [author["name"] for author in book["authors"]],
                "illustrators": [illustrator["name"] for illustrator in book["illustrators"]],
                "readsCount": 0,
                "likesCount": 0,
                "description": book["description"],
                "publisher": book["publisher"]["name"]
            })
    else:
        break

    page_num += 1

with open("books{}.json".format(level), "wb+") as f:
    f.write(json.dumps(books).encode("utf-8"))

