from selenium import webdriver
from time import sleep
import sys

from spider import Spider


def main():
    browser = webdriver.Chrome("../bin/chromedriver.exe")
    spider = Spider()

    videos = spider.fetch(sys.argv[1])
    tags = input("Tags : ")

    for video in videos:
        if video["title"].lower().find("intro") == -1 and video["title"].lower().find("how to") == -1:
            browser.get("https://www.suyambu.net/api/v1/vidstatus/upload?key=yOaNqItBPEoM2r9pxvyude0PjJsXFR30")
            browser.execute_script('document.querySelector(".video_id").value = "{}";'.format(video["video_id"]))
            browser.execute_script('document.querySelector(".title").value = "{}";'.format(video["title"]))
            browser.execute_script('document.querySelector(".category").value = "{}";'.format(video["category"]))
            browser.execute_script('document.querySelector(".tags").value = "{}";'.format(tags))
            sleep(2)
            browser.execute_script('document.querySelector("button").click();')

            while True:
                status = browser.execute_script('return document.querySelector("#status").textContent;')
                if status != "Processing...":
                    print("{}: {}".format(video["video_id"], status))
                    break
                
                sleep(0.5)

            sleep(2)
    
    print("Done.")
    browser.close()


if __name__ == "__main__":
    main()
