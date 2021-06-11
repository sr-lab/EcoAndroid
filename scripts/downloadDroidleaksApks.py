import os
import requests
import urllib.request as urllib2

from bs4 import BeautifulSoup as soup
from time import sleep

"""
The script downloads all Apks from the DroidLeaks benchmark
into the directory from where the script is ran.
"""

curr_dir = os.getcwd()
print(curr_dir)

src_url = 'http://sccpu2.cse.ust.hk/droidleaks/bugs/apks/'
page = urllib2.urlopen(src_url)
soup = soup(page.read())
table = soup.find("table")

for a in table.find_all('a'):
    file_name = a['href']
    file_path = curr_dir + '/' + file_name
    if (file_name.endswith('.apk') and not os.path.isfile(file_path)):
        try:
            print("Downloading " + file_name + ' to ' + curr_dir)
            """"
            r = requests.get(src_url + file_name)
            with open(file_name, 'wb') as f:
                f.write(r.content)
            sleep(1)  # as to not overload the server
            """
        except:
            continue
