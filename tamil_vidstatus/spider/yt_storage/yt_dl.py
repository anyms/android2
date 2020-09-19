import youtube_dl
import json


ydl = youtube_dl.YoutubeDL({'outtmpl': '%(id)s.%(ext)s'})

with ydl:
    result = ydl.extract_info(
        'http://www.youtube.com/watch?v=E9TKWRaYrU0',
        download=False # We just want to extract the info
    )

if 'entries' in result:
    # Can be a playlist or a list of videos
    video = result['entries'][0]
else:
    # Just a video
    video = result


formats = video["formats"]
print(formats[-1]["url"])

# for v in video["requested_formats"]:
#     if v["format"].find("audio") == -1:
#         requested_videos.append(v["url"])

# print(requested_videos)

# f = open("test.json", "w+")
# f.write(json.dumps(video))
# f.close()

# print(video)
# video_url = video['url']
# print(video_url)