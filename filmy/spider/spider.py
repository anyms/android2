import requests
import re
import sys
import json


# GET UPLOAD ID: https://www.googleapis.com/youtube/v3/channels?id=<CHANNEL_ID>&key=AIzaSyDlIZpGOOegBcOTuurJvuyG3V2vWNX_9sA&part=contentDetails
class Spider:
    def __init__(self):
        self.playlist_ids = ["UUI4R-QyfBZUHbaTMT_tqaWg"]
        self.videos = []

    def run(self):
        for playlist_id in self.playlist_ids:
            self.fetch(playlist_id)

        with open("data.json", "wb+") as f:
            f.write(json.dumps(self.videos).encode("utf-8"))

        print("* Done.")

    def fetch(self, playlist_id, pageToken=None):
        # https://www.googleapis.com/youtube/v3/videos?id=LC8sbeDh8U4&part=contentDetails,statistics&key=AIzaSyDlIZpGOOegBcOTuurJvuyG3V2vWNX_9sA
        if pageToken is None:
            print("* Fetching initial page...")
            resp = requests.get("https://www.googleapis.com/youtube/v3/playlistItems?playlistId={}&key=AIzaSyDlIZpGOOegBcOTuurJvuyG3V2vWNX_9sA&part=snippet&maxResults=50".format(playlist_id))
        else:
            print("* Fetching page {}...".format(pageToken))
            resp = requests.get("https://www.googleapis.com/youtube/v3/playlistItems?playlistId={}&key=AIzaSyDlIZpGOOegBcOTuurJvuyG3V2vWNX_9sA&part=snippet&maxResults=50&pageToken={}".format(playlist_id, pageToken))

        data = self.parse_data(resp.json())
        for v in data["videos"]:
            try:
                resp = requests.get("https://www.googleapis.com/youtube/v3/videos?id={}&part=contentDetails&key=AIzaSyDlIZpGOOegBcOTuurJvuyG3V2vWNX_9sA".format(v["video_id"]))
                dur_s = resp.json()["items"][0]["contentDetails"]["duration"]
                hour = re.search(r'[0-9]+H', dur_s)
                minute = re.search(r'[0-9]+M', dur_s)
                secs = re.search(r'[0-9]+S', dur_s)
                if not hour:
                    continue
                if int(hour.group(0).replace("H", "")) < 1:
                    continue
                if int(minute.group(0).replace("M", "")) < 55:
                    continue
                print("* Fetching duration {}".format(dur_s))
                self.videos.append(v)
            except KeyboardInterrupt:
                sys.exit()
            except Exception as e:
                print(e)
                pass

        if data["has_next"]:
            self.fetch(playlist_id, data["pageToken"])


    def parse_data(self, data):
        ret = {"has_next": True, "videos": []}
        if "nextPageToken" not in data:
            ret["has_next"] = False
        else:
            ret["pageToken"] = data["nextPageToken"]

        for item in data["items"]:
            v = item["snippet"]
            title = v["title"]
            video_id = v["resourceId"]["videoId"]
            ret["videos"].append({"title": title, "video_id": video_id})

        return ret


if __name__ == "__main__":
    spider = Spider()
    spider.run()
