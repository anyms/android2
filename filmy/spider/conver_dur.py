import re


s = "PT4M13S".replace("PT", "")
hour = re.search(r'[0-9]+H', s)
minute = re.search(r'[0-9]+M', s)
secs = re.search(r'[0-9]+S', s)

print(minute.group(0))
