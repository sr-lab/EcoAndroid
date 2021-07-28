import os
import sys

if len(sys.argv) < 3:
    print("Missing arguments: python3 runAndroidLeaks.py sdkPath apkDatasetPath outputFolder")
    exit()

android_sdk_path = sys.argv[1]
apks_root_path = sys.argv[2]
output_folder = sys.argv[3]
gradle_path = '../build.gradle'
cmd_part = 'gradle -b' + gradle_path + ' standalone -PappArgs=' + android_sdk_path + ","

for dirpath, dirnames, files in os.walk(apks_root_path):
    for file in files:
        if file.endswith('.apk'):
            file_path = '"' + dirpath + '/' + file + '"'
            full_cmd = cmd_part + file_path + "," + output_folder
            print('Running: ' + full_cmd)
            os.system(full_cmd)
