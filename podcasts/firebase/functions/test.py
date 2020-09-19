import requests

res  = requests.get("https://us-central1-video-downloader-efff2.cloudfunctions.net/cyberwireAddToken", params={
    "token": "hello, world"
})
print(res.text)
