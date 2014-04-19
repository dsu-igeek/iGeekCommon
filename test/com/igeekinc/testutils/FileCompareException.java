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

import java.util.ArrayList;
import java.util.Iterator;

import com.igeekinc.util.FileLike;

public class FileCompareException extends Exception
{
    /**
     * 
     */
    private static final long serialVersionUID = -2372866955379245091L;
    private ArrayList<FileCompareExceptionReason> reasons;
    private FileLike file1, file2;
    private String toolOutput;
    public FileCompareException(FileLike file1, FileLike file2)
    {
        this.file1 = file1;
        this.file2 = file2;
        reasons = new ArrayList<FileCompareExceptionReason>();
    }
    
    public void setToolOutput(String toolOutput)
    {
        this.toolOutput = toolOutput;
    }
    
    public String getToolOutput()
    {
        return toolOutput;
    }
    public synchronized void addReason(FileCompareExceptionReason newReason)
    {
        reasons.add(newReason);
    }
    
    public synchronized FileCompareExceptionReason [] getReasons()
    {
        FileCompareExceptionReason [] returnReasons = new FileCompareExceptionReason[reasons.size()];
        returnReasons = reasons.toArray(returnReasons);
        return returnReasons;
    }
    
    public synchronized String toString()
    {
        StringBuffer strBuf = new StringBuffer("Differences between files "+file1.getAbsolutePath()+" and "+file2.getAbsolutePath()+"\n");
        Iterator<FileCompareExceptionReason> reasonsIterator = reasons.iterator();
        while (reasonsIterator.hasNext())
        {
            FileCompareExceptionReason curReason = reasonsIterator.next();
            strBuf.append(curReason.toString()+"\n");
        }
        if (toolOutput != null)
            strBuf.append(toolOutput);
        return strBuf.toString();
    }
}
