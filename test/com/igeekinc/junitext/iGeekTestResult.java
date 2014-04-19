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
 
package com.igeekinc.junitext;

import java.util.ArrayList;
import java.util.Date;

import junit.framework.AssertionFailedError;
import junit.framework.Protectable;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;

import com.igeekinc.util.Volume;

public class iGeekTestResult extends TestResult
{
    protected ArrayList<TestRunRecord>testRuns = new ArrayList<TestRunRecord>();
    protected Date startTime, endTime;

    private TestRunRecord getTestRunForTest(Test test)
    {
        for (TestRunRecord curCheckRecord:testRuns)
        {
            if (curCheckRecord.getTest().equals(test))
                return curCheckRecord;
        }
        return null;
    }

    private TestRunRecord getTestRunForTest(Test test, String volumeFormatList)
    {
        for (TestRunRecord curCheckRecord:testRuns)
        {
            if (curCheckRecord.getTest().equals(test) && curCheckRecord.getVolumeFormatList().equals(volumeFormatList))
                return curCheckRecord;
        }
        return null;
    }
    
    @Override
    public synchronized void addError(Test test, Throwable t)
    {
        super.addError(test, t);
        TestRunRecord errorRecord = getTestRunForTest(test);
        if (errorRecord != null)
        {
            errorRecord.setState(TestRunState.Errors);
            errorRecord.setError(t);
        }
    }

    private void addError(FSTestCase test, Throwable t, String volumeFormatList)
    {
        super.addError(test, t);
        TestRunRecord errorRecord = getTestRunForTest(test, volumeFormatList);
        if (errorRecord != null)
        {
            errorRecord.setState(TestRunState.Errors);
            errorRecord.setError(t);
        }
    }
    
    @Override
    public synchronized void addFailure(Test test, AssertionFailedError t)
    {
        super.addFailure(test, t);
        TestRunRecord failureRecord = getTestRunForTest(test);
        if (failureRecord != null)
        {
            failureRecord.setState(TestRunState.Failed);
            failureRecord.setError(t);
        }
    }
    
    private void addFailure(FSTestCase test, AssertionFailedError t,
            String volumeFormatList)
    {
        super.addFailure(test, t);
        TestRunRecord failureRecord = getTestRunForTest(test, volumeFormatList);
        if (failureRecord != null)
        {
            failureRecord.setState(TestRunState.Failed);
            failureRecord.setError(t);
        }
    }


    @Override
    public void startTest(Test test)
    {
        startTime = new Date();
        super.startTest(test);
        if (test instanceof TestCase)
        {
            TestRunRecord testRun = new TestRunRecord((TestCase)test);
            testRun.setState(TestRunState.Passed);  // Assume we passed
            testRuns.add(testRun);
        }
    }
    

    public void startTest(FSTestCase test, String volumeFormatList)
    {
        startTime = new Date();
        super.startTest(test);
        TestRunRecord testRun = new TestRunRecord(test, volumeFormatList);
        testRun.setState(TestRunState.Passed);  // Assume we passed
        FSRequirement [] requirements = test.getFSRequirements();
        VolumeInfo [] volumeInfos = test.getVolumesForRun();
        for (int volumeNum = 0; volumeNum < volumeInfos.length; volumeNum++)
        {
            VolumeInfo curVolumeInfo = volumeInfos[volumeNum];
            Volume curVolume = curVolumeInfo.getVolume();
            TestRunVolume curTestRunVolume = new TestRunVolume(requirements[volumeNum].getDescription(),
                    curVolume.getRoot().getAbsolutePath(), curVolumeInfo.getSelectedPath().toString(), curVolume.getFsType());
            testRun.addTestRunVolume(curTestRunVolume);
        }
        testRuns.add(testRun);
    }
    
    @Override
    public void endTest(Test test)
    {
        endTime = new Date();
        super.endTest(test);
        TestRunRecord finishedRecord = getTestRunForTest(test);
        if (finishedRecord != null)
            finishedRecord.testFinished();
    }

    public void endTest(FSTestCase test, String volumeFormatList)
    {
        endTime = new Date();
        super.endTest(test);
        TestRunRecord finishedRecord = getTestRunForTest(test, volumeFormatList);
        if (finishedRecord != null)
            finishedRecord.testFinished();
    }
    
    public TestRunRecord [] getTestRuns()
    {
        TestRunRecord [] returnRecords = new TestRunRecord[testRuns.size()];
        returnRecords = testRuns.toArray(returnRecords);
        return returnRecords;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void runProtected(FSTestCase test, Protectable p,
            String volumeFormatList)
    {
        try {
            p.protect();
        } 
        catch (AssertionFailedError e) {
            addFailure(test, e, volumeFormatList);
        }
        catch (ThreadDeath e) { // don't catch ThreadDeath by accident
            throw e;
        }
        catch (Throwable e) {
            addError(test, e, volumeFormatList);
        }
    }    
}
