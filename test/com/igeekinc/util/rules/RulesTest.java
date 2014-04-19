/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * @Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.@
 */

package com.igeekinc.util.rules;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Vector;

import com.igeekinc.junitext.iGeekTestCase;
import com.igeekinc.util.ClientFile;
import com.igeekinc.util.SystemInfo;

public class RulesTest extends iGeekTestCase
{
	File baseDir = new File("/tmp","testdata/rulestest");
	File dir1 = new File(baseDir, "dir1");
	File spaceDir = new File(baseDir, "name has spaces");
	File moreDir = new File(spaceDir, "more");
	
	File [] dirs = {baseDir, dir1, spaceDir, moreDir};
	
	File abcFile = new File(spaceDir, "abc");
	File abcTiffFile = new File(spaceDir, "abc.tiff");
	File midABCFile = new File(moreDir, "This hasabcin the middle");
	File fourKFile = new File(dir1, "iam4k");
	File eightKFile = new File(spaceDir, "iam8k");
	File almostEightKFile = new File(moreDir, "iamAlmost8k");
	File todayFile = new File(dir1, "todayFile");
	File twoWeeksAgoFile = new File(baseDir, "twoWeeksAgoFile");
	File sixMonthsAgoFile = new File(spaceDir, "sixMonthsAgoFile");
	File [] files = {abcFile, abcTiffFile, midABCFile, fourKFile, eightKFile, almostEightKFile,
		todayFile, twoWeeksAgoFile, sixMonthsAgoFile};
	long [] sizes = {0L, 0L, 0L, 4096L, 8196L, 8000L, 0L, 0L, 0L};
	
	long twoWeeksAgo = System.currentTimeMillis()-(3600L*24L*14L*1000L);
	long sixMonthsAgo = System.currentTimeMillis()-(3600L*24L*30L*6L*1000L);
	Date [] dates = {null, null, null, null, null, null, null, new Date(twoWeeksAgo), new Date(sixMonthsAgo)};
	ClientFile baseCF;
	public void setUp() throws IOException
	{
		eraseDir(baseDir);
		for (int curDirNum = 0; curDirNum < dirs.length; curDirNum++)
			assertTrue(dirs[curDirNum].mkdirs());
		for (int curFileNum = 0; curFileNum < files.length; curFileNum++)
		{
			File curFile = files[curFileNum];
			if (sizes[curFileNum] == 0)
			{	
				FileWriter fw = new FileWriter(curFile);
				fw.write(curFile.getAbsolutePath()+"\n");
				fw.close();
			}
			else
			{	
				BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(curFile));
				for (long curByte = 0; curByte < sizes[curFileNum]; curByte++)
					bo.write((int)(curByte&0xff));
				bo.close();
			}
			if (dates[curFileNum] != null)
				curFile.setLastModified(dates[curFileNum].getTime());
		}
		baseCF = SystemInfo.getSystemInfo().getClientFileForPath(baseDir.getAbsolutePath());
	}
	
	public void tearDown()
	{
		//eraseDir(baseDir);
	}
	
	public void testCaseSensitivty()
	throws IOException
	{
		File [] caseSensitiveAnswers = {abcFile, abcTiffFile, midABCFile};
		NameContainsRule caseSensitiveRule = new NameContainsRule("ABC", false, false);
		testRule(caseSensitiveRule, baseCF, caseSensitiveAnswers);
		File [] containsAnswers2 = {spaceDir, midABCFile};
		caseSensitiveRule = new NameContainsRule("hAs", false, false);
		testRule(caseSensitiveRule, baseCF, containsAnswers2);
		
	}
	
	public void testContains()
	throws IOException
	{
		File [] containsAnswers = {abcFile, midABCFile, abcTiffFile};
		NameContainsRule containsRule = new NameContainsRule("abc", true, false);
		testRule(containsRule, baseCF, containsAnswers);
		File [] containsAnswers2 = {spaceDir, midABCFile};
		containsRule = new NameContainsRule("has", true, false);
		testRule(containsRule, baseCF, containsAnswers2);

	}
	
	public void testEndsWith()
	throws IOException
	{
		File [] endsWithAnswers = {abcFile, abcTiffFile};
		NameEndsWithRule endsWithRule = new NameEndsWithRule("abc", true, false);
		testRule(endsWithRule, baseCF, endsWithAnswers);
	}
	
	public void testStartsWith()
	throws IOException
	{
		File [] startsWithAnswers = {dir1};
		NameStartsWithRule startsWithRule = new NameStartsWithRule("d", true, false);
		testRule(startsWithRule, baseCF, startsWithAnswers);
	}
	
	public void testNameEqualsRule()
	throws IOException
	{
		File [] equalsAnswers = {midABCFile};
		NameEqualsRule nameEqualsRule = new NameEqualsRule("This hasabcin the middle", true, false);
		testRule(nameEqualsRule, baseCF, equalsAnswers);
		File [] equalsAnswers2 = {abcFile, abcTiffFile};
		NameEqualsRule nameEqualsRule2 = new NameEqualsRule("abc", true, false);
		testRule(nameEqualsRule2, baseCF, equalsAnswers2);
	}
	
	public void testSizeEqualRule()
	throws IOException
	{
		File [] sizeEqualsAnswers = {fourKFile};
		SizeEqualsRule sizeEqualsRule = new SizeEqualsRule(4L);
		testRule(sizeEqualsRule, baseCF, sizeEqualsAnswers);
	}
	
	public void testSizeLessThanRule()
	throws IOException
	{
		File [] lessThanAnswers = {abcFile, abcTiffFile, midABCFile, fourKFile, almostEightKFile, todayFile, twoWeeksAgoFile, sixMonthsAgoFile};
		SizeLessThanRule sizeLessThanRule = new SizeLessThanRule(8L);
		testRule(sizeLessThanRule, baseCF, lessThanAnswers);
	}
	
	public void testGreaterLessThanRule()
	throws IOException
	{
		File [] greaterThanAnswers = {eightKFile, almostEightKFile};
		SizeGreaterThanRule sizeGreaterThanRule = new SizeGreaterThanRule(4L);
		testRule(sizeGreaterThanRule, baseCF, greaterThanAnswers);
	}
	
	public void testDateBeforeRule()
	throws IOException
	{
		File [] dateBeforeAnswers = {sixMonthsAgoFile};
		GregorianCalendar workCalendar = new GregorianCalendar();
		workCalendar.setTime(new Date());
		workCalendar.add(Calendar.MONTH, -5);
		
		DateBeforeRule dateBeforeRule = new DateBeforeRule(workCalendar.getTime(), DateRule.kModifiedTime);
		testRule(dateBeforeRule, baseCF, dateBeforeAnswers);
		
	}
    
    public void testMatchSpotlightDir()
    throws IOException
    {
        File tmpDir = new File("/tmp/testdata/spotlightdirTest");
        if (!tmpDir.exists())
            assertTrue(tmpDir.mkdir());
            
        ClientFile tmpDirCF = SystemInfo.getSystemInfo().getClientFileForFile(tmpDir);

        ClientFile spotlightDir = (ClientFile)tmpDirCF.getChild(".Spotlight-V100");
        if (!spotlightDir.exists())
            assertTrue(spotlightDir.mkdir());
        ClientFile child1 = (ClientFile)spotlightDir.getChild("child1");
        FileWriter fw = new FileWriter(child1);
        fw.write(child1.getAbsolutePath()+"\n");
        fw.close();
        
        File [] spotlightDirAnswers = {spotlightDir};
        NameEqualsRule testRule = new NameEqualsRule(".Spotlight-V100", false, false);
        testRule(testRule, tmpDirCF, spotlightDirAnswers);
        
        File [] childOfAnswers = {spotlightDir, child1};
        SubDirectoryOfRule childRule = new SubDirectoryOfRule(spotlightDir, true);
        testRule(childRule, tmpDirCF, childOfAnswers);
    }
	void eraseDir(File dir)
	{
		if (!dir.exists())
			return;
		File [] children = dir.listFiles();
		for (int curFileNum = 0; curFileNum < children.length; curFileNum++)
		{	
			File curFile = children[curFileNum];
			if (curFile.isDirectory())
				eraseDir(curFile);
			else
				curFile.delete();
		}
		dir.delete();
	}
	
	void testRule(Rule ruleToTest, ClientFile startFile, File [] answers) throws IOException
	{
		logger.warn("Testing rule "+ruleToTest.toString());
		Vector testResults = testRule(ruleToTest, startFile);
		printVector(testResults);
		checkVector(testResults, answers);

	}

	Vector testRule(Rule ruleToTest, ClientFile startFile) throws IOException
	{
		Vector returnVec = new Vector();
		if (startFile.isDirectory() && !startFile.isMountPoint())
		{	
			String [] fileNames = startFile.list();
			for (int curChildNum = 0; curChildNum < fileNames.length; curChildNum++)
			{
				ClientFile curChildFile = (ClientFile)startFile.getChild(fileNames[curChildNum]);
				Vector matchesVec = testRule(ruleToTest, curChildFile);
				returnVec.addAll(matchesVec);
				
			}
		}
		if (ruleToTest.matchesRule(startFile)!=RuleMatch.kNoMatch)
			returnVec.add(startFile);
		return(returnVec);
	}
	
	void checkVector(Vector checkVector, File [] answers)
	{
		assertEquals(answers.length, checkVector.size());
		for (int curAnswerNum = 0; curAnswerNum < answers.length; curAnswerNum++)
		{
			boolean wasFound=false;
			Iterator checkIterator = checkVector.iterator();
			while(checkIterator.hasNext())
			{
				File checkFile = (File)checkIterator.next();
				if (checkFile.getAbsolutePath().equals(answers[curAnswerNum].getAbsolutePath()))
				{
					wasFound = true;
					break;
				}
			}
			assertTrue(wasFound);
		}
	}
	
	void printVector(Vector testResults)
	{
		Iterator resultsIterator = testResults.iterator();

		while(resultsIterator.hasNext())
		{
			ClientFile result = (ClientFile)resultsIterator.next();
			logger.warn("Matched "+result.getAbsolutePath());
		}
	}
	
	
}
