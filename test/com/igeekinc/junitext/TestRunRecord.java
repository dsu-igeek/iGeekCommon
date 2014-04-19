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

import junit.framework.TestCase;

enum TestRunState
{
    Passed,
    Failed,
    Errors;
    
    public String toString()
    {
        if (this.equals(Passed))
            return "pass";
        if (this.equals(Failed))
            return "fails";
        if (this.equals(Errors))
            return "errors";
        return "Unknown";
    }
}

public class TestRunRecord
{
    private TestCase test;
    private Date start, end;
    private ArrayList<TestRunVolume>testRunVolumes = new ArrayList<TestRunVolume>();
    private TestRunState state;
    private String volumeFormatList;
    private Throwable error;
    
    public TestRunRecord(TestCase test)
    {
        this(test, "");
    }
    
    public TestRunRecord(TestCase test, String volumeFormatList)
    {
        this.test = test;
        start = new Date();
        this.volumeFormatList = volumeFormatList;
    }
    
    public void testFinished()
    {
        end = new Date();
    }

    public void setState(TestRunState state)
    {
        this.state = state;   
    }
    
    
    public TestRunState getState()
    {
        return state;
    }
    
    public TestCase getTest()
    {
        return test;
    }

    public Date getStart()
    {
        return start;
    }

    public Date getEnd()
    {
        return end;
    }

    public Throwable getError()
    {
        return error;
    }

    public void setError(Throwable error)
    {
        this.error = error;
    }

    public String toString()
    {
        return test.toString()+" started = "+start+" finished = "+end;
    }
    
    public void addTestRunVolume(TestRunVolume addVolume)
    {
        testRunVolumes.add(addVolume);
    }
    
    public TestRunVolume [] getTestRunVolumes()
    {
        TestRunVolume [] returnVolumes = new TestRunVolume[testRunVolumes.size()];
        returnVolumes = testRunVolumes.toArray(returnVolumes);
        
        return returnVolumes;
    }
    
    public String getVolumeFormatList()
    {
        return volumeFormatList;
    }
}
