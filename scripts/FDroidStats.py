#################### ---------------- IMPORTS ---------------- ####################
from xml.dom import minidom
from github import Github
from github import RateLimitExceededException
import xlsxwriter
import pandas as pd

#################### ---------------- GLOBAL VARIABLES ---------------- ####################
index = 2
java = 0
archived = 0
date = 0

#################### ---------------- PROCESS REPOSITORIES ---------------- ####################

def processRepo(repo_name, worksheet):
	global archived
	global java
	global date
	
	repo = git.get_repo(repo_name)

	#checking if the repository is java 
	if(repo.language != "Java"):

		java = java + 1
		print("ERROR1: NOT JAVA")
		return

	#checking if the repository is archived
	if(repo.archived):
		archived += 1
		print("ERROR2: READ ONLY")
		return

	#checking if the repository has had a commit in the last two years
	if(repo.pushed_at.year < 2018):
		date += 1
		print("ERROR3: TOO OLD")
		return

	closedPulls = repo.get_pulls(state='closed')
	totalClosedPulls = closedPulls.totalCount

	mergedPulls = list(filter(lambda x: x.merged, closedPulls))
	percentage = 0
	if(closedPulls.totalCount > 0):
		percentage = len(mergedPulls) / totalClosedPulls

	#writing to the output file
	worksheet.write('A' + str(index), str(repo.full_name))
	worksheet.write('B' + str(index), str(urlName))
	worksheet.write('C' + str(index), repo.subscribers_count)
	worksheet.write('D' + str(index), repo.stargazers_count)
	worksheet.write('E' + str(index), repo.forks_count)
	worksheet.write('F' + str(index), repo.get_contributors().totalCount)
	worksheet.write('G' + str(index), str(repo.pushed_at))
	worksheet.write('H' + str(index), len(mergedPulls))
	worksheet.write('I' + str(index), totalClosedPulls)
	worksheet.write('J' + str(index), percentage)

#################### ---------------- MAIN ---------------- ####################

workbook = xlsxwriter.Workbook('FDroidStatsTRY.xlsx')
worksheet = workbook.add_worksheet()
worksheet.write('A1', 'APPLICATION NAME')
worksheet.write('B1', 'GITHUB LINK')
worksheet.write('C1', '#WATCHERS')
worksheet.write('D1', '#STARS')
worksheet.write('E1', '#FORKS')
worksheet.write('F1', 'CONTRIBUTORS')
worksheet.write('G1', 'DATE OF LAST COMMIT')
worksheet.write('H1', 'TOTAL MERGED PULL REQUESTS')
worksheet.write('I1', 'TOTAL CLOSED PULL REQUESTS')
worksheet.write('J1', '% OF PULL REQUESTS ACCEPTED')


indexFile = minidom.parse('index.xml')
print("opened index.xml file")

sourceTag = indexFile.getElementsByTagName('source')
print("number of source elements to look")
print(sourceTag.length)

git = Github("ACCESS_TOKEN")

for url in sourceTag:
	if(url.firstChild != None):
		urlName = url.firstChild.data
		if(urlName.startswith('https://github.com/')):
			print(urlName)
			splittedString = urlName.split('https://github.com/')
			if(len(splittedString) < 1):
				continue
			repo_name = splittedString[1]
			try:
				processRepo(repo_name, worksheet)
				index  = index + 1 
			except RateLimitExceededException:
				print('waiting for half an hour')
				sleep(1800)
				print('waiting for half an hour')
				sleep(1800)
				try:
					processRepo(repo_name, worksheet)
					index = index + 1
					continue
				except:
					continue
			
workbook.close()

#printing the number of errors to the terminal
print("JAVA ERRORS")
print(java)
print("READ ERRORS")
print(archived)
print("TOO OLD ERRORS")
print(date)	

#sorting values and writing them to an output file
df = pd.read_excel('FDroidStatsTRY.xlsx')
df = df.sort_values(by = ['% OF PULL REQUESTS ACCEPTED', 'DATE OF LAST COMMIT', 'TOTAL MERGED PULL REQUESTS', '#STARS', '#WATCHERS'], ascending = [False, False, False, False, False])
writer = pd.ExcelWriter('sortedValues.xlsx')
df.to_excel(writer, sheet_name = 'Sorted Values', index = False)

