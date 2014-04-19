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

public class FSRequirement
{
    public static final int kAnyFS = 0;
    public static final int kHFS = 1;
    public static final int kHFSPlus = 2;
    public static final int kHFSPlusJournaled = 3;
    public static final int kHFSX = 4;
    public static final int kHFSXJournaled = 5;
    public static final int kFAT16 = 6;
    public static final int kFAT32 = 7;
    public static final int kNTFS = 8;
    public static final int kUFS = 9;
    public static final int kAppleShare = 10;
    public static final int kSMB = 11;
    public static final int kNFS = 12;
    
    private int [] types;
    private long spaceRequired;
    private String description;
    
    public FSRequirement(String description, int [] types, long spaceRequired)
    {
        this.description = description;
        this.types = types;
        this.spaceRequired = spaceRequired;
    }

    public String getDescription()
    {
        return description;
    }
    
    public long getSpaceRequired()
    {
        return spaceRequired;
    }

    public int[] getTypes()
    {
        return types;
    }
}
