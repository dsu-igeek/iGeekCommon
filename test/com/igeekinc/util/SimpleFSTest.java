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
 
package com.igeekinc.util;

import com.igeekinc.junitext.FSRequirement;
import com.igeekinc.junitext.FSTestCase;
import com.igeekinc.junitext.VolumeInfo;

public class SimpleFSTest extends FSTestCase
{
    
    
    protected void setUp() throws Exception
    {
        // TODO Auto-generated method stub
        super.setUp();
    }

    public FSRequirement[] getFSRequirements()
    {
        FSRequirement [] returnRequirements = 
            {new FSRequirement("Volume 1", new int[]{FSRequirement.kAnyFS}, 1000), 
                new FSRequirement("Volume 2", new int[]{FSRequirement.kAnyFS}, 1000)};
        return returnRequirements;
    }
    
    public void testDisplay()
    {
        VolumeInfo [] curVolumeInfo = getVolumesForRun();
        assertEquals(2, curVolumeInfo.length);
        assertNotNull(curVolumeInfo[0]);
        assertNotNull(curVolumeInfo[1]);
        
        logger.warn("testDisplay called with volumeInfo 0 volume  = "+curVolumeInfo[0].getVolume().getVolumeName()+
                ", path = "+curVolumeInfo[0].getSelectedPath()+", space = "+curVolumeInfo[0].getVolume().freeSpace());
        logger.warn("testDisplay called with volumeInfo 1 volume  = "+curVolumeInfo[1].getVolume().getVolumeName()+
                ", path = "+curVolumeInfo[1].getSelectedPath()+", space = "+curVolumeInfo[1].getVolume().freeSpace());
    }
}
