import os
import sys

if len(sys.argv) < 3:
    print("Missing arguments: python3 run.py sdkPath androidLeaksPath")
    exit()

android_sdk_path = sys.argv[1]  # '/home/ricardo/Android/Sdk/platforms'
android_leaks_path = sys.argv[2]
gradle_path = '../build.gradle'
cmd = 'gradle -b' + gradle_path + ' standalone -PappArgs=' + android_sdk_path + ","

for dirpath, dirnames, files in os.walk(android_leaks_path):
    for file in files:
        if file.endswith('.apk'):
            file_path = '"' + dirpath + '/' + file + '"'
            print('Running: ' + cmd + file_path)
            os.system(cmd + file_path)
