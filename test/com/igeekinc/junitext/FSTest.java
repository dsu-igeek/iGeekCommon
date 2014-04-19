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

import junit.framework.Test;

public interface FSTest extends Test
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
    public FSRequirement[] getFSRequirements();

    /**
     * This will be called before each test is run and will specify which volumes
     * should be used for the current test run.  The volumes correspond to the FSRequirements returned
     * by getFSRequirements.  This is called normally by FSTestCase.runTest().
     * @param volumes
     */
    public void setVolumesForRun(VolumeInfo[] volumes);
    public VolumeInfo [] getVolumesForRun();
    
    /**
     * This must be called before running an FSTest and sets which volumes will be used to
     * permute across the different runs
     * @param volumesForArgs
     */
    public void setVolumesForArgs(ArrayList<VolumeInfo> [] volumesForArgs);
    
}