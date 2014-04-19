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

public class MetaDataRetrieveFailedReason extends FileCompareExceptionReason
{
    boolean secondary;
    
    public MetaDataRetrieveFailedReason(boolean secondary)
    {
        this.secondary = secondary;
    }
    
    public boolean isPrimary()
    {
        return !secondary;
    }
    
    public boolean isSecondary()
    {
        return secondary;
    }
    
    public String toString()
    {
        return "Could not read meta data for "+(secondary?" secondary ":" primary ")+" sym link file";
    }
}
