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

import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import com.igeekinc.util.exceptions.ForkNotFoundException;


/**
 * <p>Title: Indelible File System</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: iGeek, Inc.</p>
 * @author unascribed
 * @version 1.0
 */

public class TestClientFile extends ClientFile
{

    private static final long serialVersionUID = -794762988961741140L;
    public TestClientFile(String base, String name)
    {
        super(base, name);
    }
    public String[] getForkNames()
    {
        /**@todo Implement this com.igeekinc.util.ClientFile abstract method*/
        throw new java.lang.UnsupportedOperationException("Method getForkNames() not yet implemented.");
    }
    public int getNumForks()
    {
        /**@todo Implement this com.igeekinc.util.ClientFile abstract method*/
        throw new java.lang.UnsupportedOperationException("Method getNumForks() not yet implemented.");
    }
    public void setMetaData(ClientFileMetaData newMetaData) throws java.io.IOException
    {
        /**@todo Implement this com.igeekinc.util.ClientFile abstract method*/
    }
    public ClientFileMetaData getMetaData()
    {
        /**@todo Implement this com.igeekinc.util.ClientFile abstract method*/
        throw new java.lang.UnsupportedOperationException("Method getMetaData() not yet implemented.");
    }
    /* (non-Javadoc)
     * @see com.igeekinc.util.ClientFile#getForkInputStream(java.lang.String)
     */
    public InputStream getForkInputStream(String streamName) throws ForkNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }
    /* (non-Javadoc)
     * @see com.igeekinc.util.ClientFile#getForkOutputStream(java.lang.String)
     */
    public OutputStream getForkOutputStream(String streamName) throws ForkNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TestClientFile[] listClientFiles(FilenameFilter filter)
    {
        return listClientFilesInt(filter, TestClientFile.class);
    }
    public String[] list(FileLikeFilenameFilter filter) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public FileChannel getForkChannel(String forkName, boolean writeable)
    throws ForkNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public FileChannel getForkChannel(String forkName, boolean noCache, boolean writeable)
            throws ForkNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public InputStream getForkInputStream(String streamName, boolean noCache)
            throws ForkNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public OutputStream getForkOutputStream(String streamName,
            boolean noCache) throws ForkNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }
}