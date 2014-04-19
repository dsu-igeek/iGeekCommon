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
import java.util.HashSet;
import java.util.Iterator;

import junit.framework.Protectable;
import junit.framework.TestResult;

public abstract class FSTestCase extends iGeekTestCase implements FSTest
{
    /**
     * Get the kind of file systems that this test needs in order to run.
     * The number of FSRequirements returned should reflect the number of file
     * systems required for a single run of the test (e.g. a copy from one volume to another
     * should return 2 FSRequirements).
     * All tests will be run with all possible permutations of volumes available that match
     * the requirements
     * @return
     */
    public abstract FSRequirement [] getFSRequirements();
    /**
     * This will be called before any tests are run and will specify which volumes
     * should be used for the test.  The volumes correspond to the FSReuqiremnts returned
     * by getFSRequirements
     * @param volumes
     */
    public void setVolumesForArgs(ArrayList<VolumeInfo> [] volumesForArgs)
    {
        this.volumesForArgs = volumesForArgs;
    }
    
    private ArrayList<VolumeInfo> [] volumesForArgs;
    private VolumeInfo [] volumesForRun;
    
    private HashSet<String> volumeFormatsIterated = new HashSet<String>();
        
    public ArrayList<VolumeInfo> [] getVolumesForArgs()
    {
        return volumesForArgs;
    }
    
    public void setVolumesForRun(VolumeInfo[] volumes)
    {
       volumesForRun = volumes;
    }
    
    public VolumeInfo [] getVolumesForRun()
    {
        return volumesForRun;
    }
    
    public void run(TestResult result)
    {
        recurseAndPermuteTests(0, null, result);    // Run through all of the possible combos
    }
    
    @SuppressWarnings("unchecked")
    void recurseAndPermuteTests(int argNum, ArrayList<VolumeInfo> volumeInfoToUse, TestResult result)
    {
        if (volumeInfoToUse == null)
            volumeInfoToUse = new ArrayList<VolumeInfo>();
        if (argNum == volumesForArgs.length)
        {
            // Set up the arguments and run the test
            VolumeInfo [] fsVolumes = new VolumeInfo [0];
            fsVolumes = volumeInfoToUse.toArray(fsVolumes);
            
            String volumeFormatList = "";
            for (int curVolumeInfoNum = 0; curVolumeInfoNum < fsVolumes.length; curVolumeInfoNum++)
            {
            	if (volumeFormatList.length() > 0)
            		volumeFormatList = volumeFormatList + "-";
            	volumeFormatList = volumeFormatList+fsVolumes[curVolumeInfoNum].getVolume().getFsType();
            }
            
            if (volumeFormatsIterated.contains(volumeFormatList))
            	return;
            volumeFormatsIterated.add(volumeFormatList);
            setVolumesForRun(fsVolumes);
            if (result instanceof iGeekTestResult)
                ((iGeekTestResult)result).startTest(this, volumeFormatList);
            else
                result.startTest(this);
            Protectable p= new Protectable() {
                public void protect() throws Throwable {
                    runBare();
                }
            };
            
            if (result instanceof iGeekTestResult)
                ((iGeekTestResult)result).runProtected(this, p, volumeFormatList);
            else
                result.runProtected(this, p);
            if (result instanceof iGeekTestResult)
                ((iGeekTestResult)result).endTest(this, volumeFormatList);
            else
                result.endTest(this);
        }
        else
        {
            // Iterate over all of the possible combos for this argNum and recurse to the next argNum
            Iterator<VolumeInfo> volumeIterator = volumesForArgs[argNum].iterator();
            while(volumeIterator.hasNext())
            {
                VolumeInfo curVolume = volumeIterator.next();
                if (volumeInfoToUse.contains(curVolume))
                    continue;
                ArrayList<VolumeInfo> nextVolumeInfoToUse = (ArrayList<VolumeInfo>)volumeInfoToUse.clone();
                nextVolumeInfoToUse.add(curVolume);
                recurseAndPermuteTests(argNum+1, nextVolumeInfoToUse, result);
            }
        }
    }
}
