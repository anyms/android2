import requests

API_KEY = "c48d90ae25495eff1a4d41bd05ce8b8e"
URL = "https://api.imgbb.com/1/upload?key={}".format(API_KEY)

files = {'image': open('test.png', 'rb')}
resp = requests.post(URL, files=files)

print(resp.json()["data"]["url"])