#################### ---------------- IMPORTS ---------------- ####################
from xml.dom import minidom
import xml.etree.ElementTree as ET
from github import Github
import pygit2
import pandas as pd
from time import sleep
import xlsxwriter
import openpyxl
import sys
import os
import subprocess
import re
from openpyxl import Workbook

#################### ---------------- GLOBAL VARIABLES ---------------- ####################
maxApps = 100
index = 2

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
worksheet.write('D1', '#INFOWARNINGFCM')
worksheet.write('E1', '#CHECKNETWORK')
worksheet.write('F1', '#CHECKLAYOUTSIZE')
worksheet.write('G1', '#CHECKMETADATA')
worksheet.write('H1', '#SSLSESSIONCACHING')
worksheet.write('I1', '#PASSIVEPROVIDERLOCATION')
worksheet.write('J1', '#HTTPSURLCONNCACHEMECHANISM')
worksheet.write('K1', '#DIRTYRENDERING')
worksheet.write('H1', '#GZIPCOMPRESSION')

#################### ---------------- CLONE REPOSITORY ---------------- ####################
print('cloning repositories')
inspections = ['DynamicWaitTime.xml', 'InfoWarningFCM.xml', 'CheckNetwork.xml', 'CheckLayoutSize.xml', 'CheckMetadata.xml', 
'SSLSessionCaching.xml','PassiveProviderLocation.xml', 'HTTPsURLConnCacheMechanism.xml', 'DirtyRendering.xml', 'GZIPCompression.xml']

for i in range(0, len(linkColumn)):
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
	os.system("rm -rf " + path + "/inspectionResults")
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
		print(inspection)
		inspectionFilePath = path + "/inspectionResults/" + inspection
		fileExists = os.path.isfile(inspectionFilePath)
		if(fileExists):
	 		indexFile = minidom.parse(inspectionFilePath)
	 		sourceTag = indexFile.getElementsByTagName('problem')
	 		print(sourceTag.length)
	 		worksheet.write(currLetter + str(index), sourceTag.length) 
		else:
			worksheet.write(currLetter + str(index), 0)
		currLetter = chr(ord(currLetter) + 1)
	index = index + 1
workbook.close()






