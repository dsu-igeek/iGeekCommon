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
 
package com.igeekinc.testutils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.igeekinc.util.FileLike;
import com.igeekinc.util.SystemInfo;
import com.igeekinc.util.rules.Rule;
import com.igeekinc.util.rules.RuleMatch;

public abstract class FileCompare
{
    private static FileCompare fileCompare;
    
    public static FileCompare getFileCompare()
    {
        if (fileCompare == null)
        {
            String  osName = System.getProperty("os.name"); //$NON-NLS-1$
            String className = null;
            Logger logger = org.apache.log4j.Logger.getLogger(SystemInfo.class);

            if (osName.equals("Mac OS X")) //$NON-NLS-1$
            {
                className = "com.igeekinc.testutils.macos.macosx.MacOSXFileCompare"; //$NON-NLS-1$
            }
            try
            {
                Class<?> fileCompareClass = Class.forName(className);

                Class<?> [] constructorArgClasses = {};
                Constructor<?> fileCompareConstructor = fileCompareClass.getConstructor(constructorArgClasses);
                Object [] constructorArgs = {};
                fileCompare = (FileCompare)fileCompareConstructor.newInstance(constructorArgs);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                logger.error("Caught exception creating SystemInfo", e); //$NON-NLS-1$
                throw new InternalError("Caught exception creating SystemInfo"); //$NON-NLS-1$
            }
        }
        return fileCompare;
    }


    public void compareFiles(FileLike primary, FileLike secondary) throws IOException, FileCompareException
    {
    	compareFiles(primary, secondary, false);
    }
    
    public abstract void compareFiles(FileLike primary, FileLike secondary, boolean ignoreDirectoryTimes) throws IOException, FileCompareException;
    
    boolean matchesRules(FileLike checkFile, Rule [] rules)
    {
        if (rules == null || rules.length == 0)
            return false;
        for (int curRuleNum = 0; curRuleNum < rules.length; curRuleNum++)
        {
            if (rules[curRuleNum].matchesRule(checkFile) != RuleMatch.kNoMatch)
                return true;
        }
        return false;
    }
    
    public void compareDirs(FileLike primaryDir, Rule [] primaryExcludes, FileLike secondaryDir, Rule [] secondaryExcludes)
    throws IOException, TreeCompareException
    {
    	compareDirs(primaryDir, primaryExcludes, secondaryDir, secondaryExcludes, false);
    }
    
    public void compareDirs(FileLike primaryDir, Rule [] primaryExcludes, FileLike secondaryDir, Rule [] secondaryExcludes, boolean ignoreDirectoryTimes)
    	    throws IOException, TreeCompareException
    {
        TreeCompareException exception = new TreeCompareException(primaryDir, secondaryDir);
        boolean throwException = false;
        
        try
        {
            compareFiles(primaryDir, secondaryDir, ignoreDirectoryTimes);
        }
        catch (FileCompareException e)
        {
            FileCompareExceptionReason reasons[] = e.getReasons();
            boolean throwNow = false;
            for (int curReasonNum = 0; curReasonNum < reasons.length; curReasonNum++)
            {
                // Directories lengths are hard to predict and can differ easily
                if (!(reasons[curReasonNum] instanceof DataLengthDiffersReason))
                {
                    exception.addReason(reasons[curReasonNum]);
                    throwException = true;
                }
                if (reasons[curReasonNum] instanceof FileDoesNotExistReason ||
                        reasons[curReasonNum] instanceof MetaDataRetrieveFailedReason)
                    
                    throwNow = true;
            }
            if (throwNow)
                throw exception;    // One or both dirs are missing - bail now
        }
        TreeMap<String, Object> primarySorted = loadSorted(primaryDir, primaryExcludes);

        TreeMap<String, Object> secondarySorted = loadSorted(secondaryDir, secondaryExcludes);        
        Iterator<String> primaryIterator = primarySorted.keySet().iterator();
        while (primaryIterator.hasNext())
        {
            String curPrimaryName = primaryIterator.next();
            if (secondarySorted.containsKey(curPrimaryName))
            {
                FileLike curPrimaryChild = primaryDir.getChild(curPrimaryName);
                FileLike curSecondaryChild = secondaryDir.getChild(curPrimaryName);
                
                if (curPrimaryChild.isDirectory() && curSecondaryChild.isDirectory())
                {
                    try
                    {
                        compareDirs(curPrimaryChild, primaryExcludes, curSecondaryChild, secondaryExcludes, ignoreDirectoryTimes);
                    }
                    catch (FileCompareException e)
                    {
                        exception.addSubException(e);
                        throwException = true;
                    }
                }
                else
                {
                    try
                    {
                        compareFiles(curPrimaryChild, curSecondaryChild, ignoreDirectoryTimes);
                    }
                    catch (FileCompareException e)
                    {
                        exception.addSubException(e);
                        throwException = true;
                    }
                }
                secondarySorted.remove(curPrimaryName);
            }
            else
            {
                FileMissingReason reason = new FileMissingReason(secondaryDir, curPrimaryName);
                exception.addReason(reason);
                throwException = true;
            }
        }
        
        // OK, all that is left in secondarySorted are the names that didn't match anything in the primary
        Iterator<String> secondaryIerator = secondarySorted.keySet().iterator();
        while (secondaryIerator.hasNext())
        {
            String curSecondaryName = secondaryIerator.next();
            FileMissingReason reason = new FileMissingReason(primaryDir, curSecondaryName);
            exception.addReason(reason);
            throwException = true;
        }
        if (throwException)
            throw exception;
    }


    private TreeMap<String, Object> loadSorted(FileLike parentDir, Rule[] excludes) throws IOException
    {
        TreeMap<String, Object> returnSorted = new TreeMap<String, Object>();
        
        String [] children = parentDir.list();
        for (int curChildNum = 0; curChildNum < children.length; curChildNum++)
        {
            String curChildName = children[curChildNum];
            FileLike curCheckFile = parentDir.getChild(curChildName);
            if (matchesRules(curCheckFile, excludes))
                continue;
            returnSorted.put(curChildName, null);
        }
        return returnSorted;
    }
}
