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

import java.io.File;
import java.io.IOException;

import com.igeekinc.junitext.iGeekTestCase;
import com.igeekinc.testutils.TestFilesTool;
import com.igeekinc.util.ClientFile;
import com.igeekinc.util.SystemInfo;

public class IncludeExcludeRuleTest extends iGeekTestCase
{
	public void testAlpha()
	throws IOException
	{
		File testFile = File.createTempFile("IncludeExcludeRuleTest", "test");
		File root = testFile.getParentFile();
		File dir1 = new File(root, "dir1");
		dir1.mkdir();
		File dir2 = new File(root, "dir2");
		dir2.mkdir();
		try
		{
			IncludeExcludeRule basicRule = new IncludeExcludeRule();
			basicRule.setRoot(root.getAbsolutePath(), true);
			basicRule.includeExclude(dir1.getAbsolutePath(), false);
			basicRule.includeExclude(dir2.getAbsolutePath(), true);
			ClientFile dir1cf = SystemInfo.getSystemInfo().getClientFileForPath(dir1.getAbsolutePath());
			assertEquals(RuleMatch.kNoMatch, basicRule.matchesRule(dir1cf));
			ClientFile dir2cf = SystemInfo.getSystemInfo().getClientFileForPath(dir2.getAbsolutePath());
			assertEquals(RuleMatch.kSubdirsMatch, basicRule.matchesRule(dir2cf));
			ClientFile testFileCF = SystemInfo.getSystemInfo().getClientFileForPath(testFile.getAbsolutePath());
			assertEquals(RuleMatch.kNoMatch, basicRule.matchesRule(testFileCF));
		}
		finally
		{
			dir2.delete();
			dir1.delete();
			testFile.delete();
		}
	}
    
    public void testBigTree()
    throws IOException
    {
        File testFileDir = File.createTempFile("includeExcludeTest", "");
        assertTrue(testFileDir.delete());
        assertTrue(testFileDir.mkdir());
        TestFilesTool.makeTestHierarchy(testFileDir, 5, 5, 5);
        try
        {
            IncludeExcludeRule basicRule = new IncludeExcludeRule();
            basicRule.setRoot(testFileDir.getAbsolutePath(), true);
            assertEquals(RuleMatch.kSubdirsMatch, basicRule.matchesRule(testFileDir.getAbsolutePath()));
            assertEquals(RuleMatch.kSubdirsMatch, basicRule.matchesRule(testFileDir.getAbsolutePath()+"/"+"dir0"));
            
            basicRule.includeExclude(testFileDir.getAbsolutePath()+"/"+"dir0", false);
            assertEquals(RuleMatch.kNoMatch, basicRule.matchesRule(testFileDir.getAbsolutePath()+"/"+"dir0"));
            assertEquals(RuleMatch.kNoMatch, basicRule.matchesRule(testFileDir.getAbsolutePath()+"/"+"dir1"));
            assertEquals(RuleMatch.kSubdirsMatch, basicRule.matchesRule(testFileDir.getAbsolutePath()));
            
            basicRule = new IncludeExcludeRule();
            basicRule.setRoot(testFileDir.getAbsolutePath(), true);
            basicRule.includeExclude(testFileDir.getAbsolutePath(), false);
            basicRule.includeExclude(testFileDir.getAbsolutePath()+"/dir0/dir0", true);
            assertEquals(RuleMatch.kSubdirsMatch, basicRule.matchesRule(testFileDir.getAbsolutePath()));
            assertEquals(RuleMatch.kNoMatch, basicRule.matchesRule(testFileDir.getAbsolutePath()+"/dir1"));
            assertEquals(RuleMatch.kSubdirsMatch, basicRule.matchesRule(testFileDir.getAbsolutePath()+"/dir0/dir0"));
            assertEquals(RuleMatch.kSubdirsMatch, basicRule.matchesRule(testFileDir.getAbsolutePath()+"/dir0/dir0/file1"));
            assertEquals(RuleMatch.kSubdirsMatch, basicRule.matchesRule(testFileDir.getAbsolutePath()+"/dir0/dir0/dir1"));
            assertEquals(RuleMatch.kNoMatch, basicRule.matchesRule(testFileDir.getAbsolutePath()+"/dir0/dir1"));
        }
        finally
        {
            TestFilesTool.deleteTree(testFileDir);
        }
    }
    
}
