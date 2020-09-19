import requests
from urllib.parse import unquote
import json
import urllib.parse as urlparse
from urllib.parse import parse_qs
import time


def fetch(video_id):
    resp = requests.get("https://www.youtube.com/get_video_info?video_id={}".format(video_id))
    data = json.loads(unquote(parse_str(resp.text)["player_response"]))
    video_details = data["videoDetails"]
    streaming_data = data["streamingData"]

    with open("fetched.json", "w+") as f:
        f.write(json.dumps(streaming_data))

    qualities = []
    audio = None
    duration = 0
    expire = 0

    video = {"qualities": []}
    for node in streaming_data["adaptiveFormats"]:
        if node["mimeType"].find("vp9") > -1 and node["qualityLabel"] not in qualities:
            qualities.append(node["qualityLabel"])
            # duration = node["approxDurationMs"]
            parsed = urlparse.urlparse(node["url"])
            expire = int(parse_qs(parsed.query)['expire'][0])
            print("{}   ||   {}".format(expire, int(time.time())))

            video["qualities"].append({"label": node["qualityLabel"], "url": node["url"]})
        elif node["mimeType"].startswith("audio/") and audio is None:
            audio = node["url"]

    video["duration"] = duration
    video["audio"] = audio
    video["expire"] = expire

    print(video["audio"])
            


def parse_str(s):
    data = {}
    for n in s.split("&"):
        node = n.split("=")
        data[node[0]] = node[1]
    return data


if __name__ == "__main__":
    fetch("Mm9K07TcGEs")
