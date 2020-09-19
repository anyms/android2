import requests
import json
import os


def main():
    with open("data.json", "r") as f:
        data = json.loads(f.read())

    for status in data:
        if os.path.exists("vids/{}.mp4".format(status["uId"])):
            print("Skipping... {}".format(status["uId"]))
            continue
        resp = requests.get(status["video"], stream=True)
        print("Downloading... {}: Status Code: {}".format(status["uId"], resp.status_code))
        if not resp.headers.get('content-type', "").startswith("video/"):
            print("Skipping... {}".format(status["video"]))
            continue
        total = int(resp.headers.get('content-length', 0))
        current = 0
        with open("vids/{}.mp4".format(status["uId"]), "wb+") as f:
            for chunk in resp.iter_content(chunk_size=1024):
                current += len(chunk)
                print("Progress: {}%".format(int(current / total * 100)), sep='', end='\r',flush=True)
                f.write(chunk)

        # os.system("""ffmpeg -i v.mp4 -vf drawtext="fontfile=font.ttf: text='Tamil VidStatus': fontcolor=white: fontsize=30: box=1: boxcolor=black@1: boxborderw=5: x=(15): y=(h-text_h/0.70)" -codec:a copy vids/{}.mp4 -y""".format(status["uId"]))

        # os.system("""ffmpeg -i vids/{}.mp4 -ss 00:00:04 -vframes 1 thumbs/{}.png -y""".format(status["uId"], status["uId"]))


if __name__ == "__main__":
    main()
