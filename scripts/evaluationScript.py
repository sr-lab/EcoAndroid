#################### ---------------- IMPORTS ---------------- ####################

from xml.dom import minidom
import xml.etree.ElementTree as ET
from github import Github
import pygit2
import pandas as pd
from time import sleep
import xlsxwriter
import sys
import os
import subprocess
import re

#################### ---------------- GLOBAL VARIABLES ---------------- ####################
maxApps = 20
index = 2
ACCESS_TOKEN = 'ACCESS_TOKEN'

#################### ---------------- OPEN XLXS FILE ---------------- ####################
print('opening xlxs file')
data = pd.read_excel('FDroidStats.xlsx')[:maxApps]
linkColumn = data['GITHUB LINK']
nameColumn = data['APPLICATION NAME']

#################### ---------------- CREATE OUTPUT XLXS FILE ---------------- ####################
workbook = xlsxwriter.Workbook('evaluationStats.xlsx')
worksheet = workbook.add_worksheet()
worksheet.write('A1', 'APPLICATION NAME')
worksheet.write('B1', 'GITHUB LINK')
worksheet.write('C1', '#DYNAMICWAITTIME')
worksheet.write('D1', '#CHECKNETWORK')
worksheet.write('E1', '#CHECKLAYOUTSIZE')
worksheet.write('F1', '#CHECKMETADATA')
worksheet.write('G1', '#SSLSESSIONCACHING')
worksheet.write('H1', '#PASSIVEPROVIDERLOCATION')
worksheet.write('I1', '#HTTPSURLCONNCACHEMECHANISM')
worksheet.write('J1', '#DIRTYRENDERING')

#################### ---------------- CLONE REPOSITORY ---------------- ####################
print('cloning repositories')
git = Github(ACCESS_TOKEN)
inspections = ['DynamicWaitTime.xml', 'CheckNetwork.xml', 'CheckLayoutSize.xml', 'CheckMetadata.xml', 
'SSLSessionCaching.xml','PassiveProviderLocation.xml', 'HTTPsURLConnCacheMechanism.xml', 'DirtyRendering.xml']

i = 0 
for i in range(len(linkColumn)):
	print("index is " + str(i))
	path = nameColumn.values[i].split('/')[0]

	isdir = os.path.isdir(path)  
	if(isdir == False):
		os.mkdir(path)
	else:
		path = path + "_new"

	clone = "git clone " + linkColumn.values[i] + " " + path
	inspect = "./idea.sh inspect " + path + "/ ProjectDefault.xml " + " " + path + "/inspectionResults/ -v2"
	closing = "osascript -e 'quit app \"Intellij\"'"
	number = "ps aux | grep -v grep | grep -ci intellij"

	os.system(clone)
	os.system("rm -rf " + path + "/.idea")
	os.system("cp -a .idea/ " + path + "/.idea")
	os.system(inspect)
	os.system(number)
	os.system("date")
	try:
	 	line = subprocess.check_output(number, shell=True, stderr=subprocess.STDOUT)
	 	line = int(str(line)[2])
	 	while(line > 0):
	 		print("going to wait a 10 seconds")
	 		sleep(10)
	 		line = subprocess.check_output(number, shell=True, stderr=subprocess.STDOUT)
	 		line = int(str(line)[2])
	except subprocess.CalledProcessError as e:
	 	#do nothing
	 	print("done")

	os.system("date")
	#write in the output file
	worksheet.write('A' + str(index), str(nameColumn.values[i]))
	worksheet.write('B' + str(index), str(linkColumn.values[i]))
	currLetter = 'C'
	for inspection in inspections: 
	 	inspectionFilePath = path + "/inspectionResults/" + inspection
	 	fileExists = os.path.isfile(inspectionFilePath)
	 	#print(inspectionFilePath)
	 	if(fileExists):
	 		indexFile = minidom.parse(inspectionFilePath)
	 		sourceTag = indexFile.getElementsByTagName('problem')
	 		worksheet.write(currLetter + str(index), str(sourceTag.length))
	 	else:
	 		worksheet.write(currLetter + str(index), '0')
	 	currLetter = chr(ord(currLetter) + 1)
	index = index + 1
workbook.close()






