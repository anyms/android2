import requests
import json
from slugify import slugify


def main():
    f = open("data/channels.json", "rb")
    channels = json.loads(f.read().decode("utf-8"))
    f.close()

    for i, channel in enumerate(channels):
        file_name = slugify(channel["title"]).replace("-", "_")

        res = requests.get(channel["image"])
        f = open("data/images/{}.jpg".format(file_name), "wb+")
        f.write(res.content)
        f.close()
        print("{} is downloaded".format(channel["title"]))

        channels[i]["image"] = file_name

    f = open("data/updated_channels.json", "wb+")
    f.write(json.dumps(channels, ensure_ascii=False).encode("utf-8"))
    f.close()


if __name__ == "__main__":
    main()
