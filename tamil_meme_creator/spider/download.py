import requests

start_range = 181920988 + 31569920 + 225489820 + 32112640
r = requests.get("http://dl3.perserver.site/Movie/1399/01/Gretel.and.Hansel.2020/Gretel.and.Hansel.2020.720p.WEB-DL.Pahe_PerDL.mkv", stream=True, headers={
    "Range": "bytes={}-".format(start_range)
})

with open("v.mkv", 'ab+') as f:
    total_length = int(r.headers.get('content-length'))
    current = 0
    for chunk in r.iter_content(chunk_size=10*1024): 
        if chunk:
            f.write(chunk)
            f.flush()
            current += len(chunk)

            print("Progress: {}%, Total Bytes: {}B/{}B".format(int(current/total_length*100.0), total_length, current), end="\r")
