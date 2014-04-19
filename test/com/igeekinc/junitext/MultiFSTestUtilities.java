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

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.igeekinc.util.Volume;
import com.igeekinc.util.logging.ErrorLogMessage;

public class MultiFSTestUtilities
{

    @SuppressWarnings("unchecked")
    public static void setupFSTest(FSTest testSuite, VolumeInfo [] volumeInfo)
    {
        FSRequirement [] fsRequirements = testSuite.getFSRequirements();
        
        if (volumeInfo.length < fsRequirements.length)  // Must be at least one volume for each argument
        {
            Logger.getLogger("").error(fsRequirements.length+" volumes required but only "+volumeInfo.length+" provided");
            throw new IllegalArgumentException(fsRequirements.length+" volumes required but only "+volumeInfo.length+" provided");
        }
        for (int curVolumeNum = 0; curVolumeNum < volumeInfo.length; curVolumeNum++)
        {
            Volume curVolume = volumeInfo[curVolumeNum].getVolume();
            try
            {
                curVolume.enablePermissions();
            } catch (IOException e)
            {
                Logger.getLogger("").error(new ErrorLogMessage("Caught exception"), e);
            }
        }
        ArrayList<VolumeInfo> [] volumesForArgs = new ArrayList[fsRequirements.length];  // The volumes that are valid for each argument
        for (int curArgNum = 0; curArgNum < fsRequirements.length; curArgNum++)
        {
            FSRequirement curRequirement = fsRequirements[curArgNum];
            ArrayList<VolumeInfo> newList = MultiFSTestUtilities.getVolumesForRequirement(volumeInfo, curRequirement);
            volumesForArgs[curArgNum] = newList;
        }
        // we now have a 2D array of Volumes to use.  Let's recurse/permute through it and run tests
        // across all of the combos we can make
        testSuite.setVolumesForArgs(volumesForArgs);
    }

    /**
     * Returns an ArrayList with the VolumeInfo's that satisfy curRequirement
     * @param volumeInfo
     * @param curRequirement
     * @return
     */
    static ArrayList<VolumeInfo> getVolumesForRequirement(VolumeInfo[] volumeInfo, FSRequirement curRequirement)
    {
        ArrayList<VolumeInfo> newList = new ArrayList<VolumeInfo>();
        for (int curVolumeNum = 0; curVolumeNum < volumeInfo.length; curVolumeNum++)
        {
            VolumeInfo curVolumeInfo = volumeInfo[curVolumeNum];
            Volume curVolume = curVolumeInfo.getVolume();
    
            if (curVolume.freeSpace() > curRequirement.getSpaceRequired())
            {
                boolean addVolume = false;
                int [] typesRequired = curRequirement.getTypes();
                for (int curTypeNum = 0; curTypeNum < typesRequired.length; curTypeNum++)
                {
                    String fsType = curVolume.getFsType();
                    switch(typesRequired[curTypeNum])
                    {
                    case FSRequirement.kAnyFS:
                        addVolume = true;
                        break;
                    case FSRequirement.kAppleShare:
                        if (fsType.equals("afpfs"))
                            addVolume = true;
                        break;
                        // How do we tell the difference between FAT32 and FAT16??
                    case FSRequirement.kFAT32:
                    case FSRequirement.kFAT16:
                        if (fsType.equals("msdos"))
                            addVolume = true;
                        break;
                    case FSRequirement.kHFS:
                    case FSRequirement.kHFSPlus:
                    case FSRequirement.kHFSPlusJournaled:
                    case FSRequirement.kHFSX:
                    case FSRequirement.kHFSXJournaled:
                        if (fsType.equals("hfs"))
                            addVolume = true;
                        break;
                    case FSRequirement.kNFS:
                        if (fsType.equals("nfs"))
                            addVolume = true;
                        break;
                    case FSRequirement.kNTFS:
                        if (fsType.equals("ntfs"))
                            addVolume = true;
                        break;
                    case FSRequirement.kSMB:
                        if (fsType.equals("smb") || fsType.equals("cifs"))
                            addVolume = true;
                        break;
                    case FSRequirement.kUFS:
                        if (fsType.equals("ufs"))
                            addVolume = true;
                    default:
    
                            
                    }
                }
                if (addVolume)
                    newList.add(curVolumeInfo);
            }
        }
        return newList;
    }
}
