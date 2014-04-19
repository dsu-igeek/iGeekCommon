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
 
package com.igeekinc.util.formats.splitfile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.igeekinc.junitext.iGeekTestCase;
import com.igeekinc.testutils.TestFilesTool;
import com.igeekinc.util.ClientFile;
import com.igeekinc.util.SHA1HashID;
import com.igeekinc.util.SystemInfo;

public class SplitFileOutputStreamTest extends iGeekTestCase
{

    public SplitFileOutputStreamTest() throws IOException
    {
        
    }
    public void testA()
    throws Exception
    {
        ClientFile testFileCF = SystemInfo.getSystemInfo().getClientFileForFile(File.createTempFile("splitTest", "tst"));
        SplitFileSegmentInfo segment = new SplitFileSegmentInfo(-1, testFileCF);
        SplitFileDescriptor descriptor = new SimpleSplitFileDescriptor(segment);
        SplitFileOutputStream outStream = new SplitFileOutputStream(System.currentTimeMillis(), "test", descriptor);
        outStream.startFork("test");
        SHA1HashID testFileHash = TestFilesTool.writeTestDataToOutputStream(outStream, 1024*1024 + 357);
        outStream.closeFork();
        outStream.close();
        
        descriptor = new SimpleSplitFileDescriptor(segment);
        SplitFileInputStream testStream = new SplitFileInputStream(descriptor);
        assertEquals("test", testStream.getNextFork());
        TestFilesTool.verifyInputStream(testStream, 1024*1024 + 357, testFileHash);
        //TestFilesTool.verifyFile(testFileCF, testFileHash, 1024*1024 + 357);
        
    }
    
    public void testB()
    throws Exception
    {
        ArrayList<SplitFileSegmentInfo> segmentInfoList = new ArrayList<SplitFileSegmentInfo>();
        for (int curFileNum = 0; curFileNum < 10; curFileNum++)
        {
            ClientFile curTestFileCF = SystemInfo.getSystemInfo().getClientFileForFile(File.createTempFile("splitTest", "tst"));
            SplitFileSegmentInfo curSegment = new SplitFileSegmentInfo(1024*1024, curTestFileCF);
            segmentInfoList.add(curSegment);
        }
        SplitFileDescriptor descriptor = new SimpleSplitFileDescriptor(segmentInfoList);
        SplitFileOutputStream outStream = new SplitFileOutputStream(System.currentTimeMillis(), "test", descriptor);
        
        outStream.startFork("test");
        SHA1HashID testFileHash = TestFilesTool.writeTestDataToOutputStream(outStream, 1024*1024*10);
        outStream.closeFork();
        outStream.close();
        

        descriptor = new SimpleSplitFileDescriptor(segmentInfoList);

        SplitFileInputStream inStream = new SplitFileInputStream(descriptor);
        assertEquals("test", inStream.getNextFork());
        TestFilesTool.verifyInputStream(inStream, 10*1024*1024, testFileHash);

    }

    public void testMultipleForks() throws Exception
    {
        ArrayList<SplitFileSegmentInfo> segmentInfoList = new ArrayList<SplitFileSegmentInfo>();
        int segmentLength = 1024*1024;
        int totalLength = 10 * segmentLength;
        int numForks = 37;
        int forkLength = totalLength/numForks;
        for (int curFileNum = 0; curFileNum < 10; curFileNum++)
        {
            ClientFile curTestFileCF = SystemInfo.getSystemInfo().getClientFileForFile(File.createTempFile("splitTest", "tst"));
            SplitFileSegmentInfo curSegment = new SplitFileSegmentInfo(1024*1024, curTestFileCF);
            segmentInfoList.add(curSegment);
        }
        SplitFileDescriptor descriptor = new SimpleSplitFileDescriptor(segmentInfoList);
        SplitFileOutputStream outStream = new SplitFileOutputStream(System.currentTimeMillis(), "test", descriptor);
        
        SHA1HashID [] testFileHashes = new SHA1HashID[numForks];
        
        for (int curForkNum = 0; curForkNum < numForks; curForkNum++)
        {
            outStream.startFork("test"+curForkNum);
            testFileHashes[curForkNum] = TestFilesTool.writeTestDataToOutputStream(outStream, forkLength);
            outStream.closeFork();
        }
        outStream.close();
        

        descriptor = new SimpleSplitFileDescriptor(segmentInfoList);


        SplitFileInputStream inStream = new SplitFileInputStream(descriptor);
        for (int curForkNum = 0; curForkNum < numForks; curForkNum++)
        {
            assertEquals("test"+curForkNum, inStream.getNextFork());
            TestFilesTool.verifyInputStream(inStream, forkLength, testFileHashes[curForkNum]);
        }
    }
}
