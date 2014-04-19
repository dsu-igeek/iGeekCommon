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

import com.igeekinc.util.FileLike;

public class FileDoesNotExistReason extends FileCompareExceptionReason
{
    private FileLike doesNotExistFile;
    
    public FileDoesNotExistReason(FileLike doesNotExistFile)
    {
        this.doesNotExistFile = doesNotExistFile;
    }
    
    public FileLike getDoesNotExistFile()
    {
        return doesNotExistFile;
    }
    
    public String toString()
    {
        return("File "+doesNotExistFile.getAbsolutePath()+" does not exist");
    }
}
