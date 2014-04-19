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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.igeekinc.junitext.iGeekTestCase;

class GrowingBuffersSource implements Runnable
{
    private BufferQueueOutputStream outStream;
    public GrowingBuffersSource(BufferQueueOutputStream outStream)
    {
        this.outStream = outStream;
    }
    
    public void run()
    {
        try
        {
            for (int i = 1; i < 256; i++)
            {
                byte [] bytes = new byte[i];
                for (int j = 0; j < i; j++)
                    bytes[j] = (byte)i;
                outStream.write(bytes);
                Thread.sleep(50);
            }
            outStream.close();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
public class BufferQueueTest extends iGeekTestCase
{
    public void testSimple()
    throws Exception
    {
        BufferQueue testQueue = new BufferQueue(100);
        OutputStream outStream = testQueue.getOutputStream();
        outStream.write(1);
        outStream.close();
        InputStream inStream = testQueue.getInputStream();
        assertEquals(1, inStream.read());
        assertEquals(-1, inStream.read());
    }
    
    public void testWriteBufOffsetLen()
    throws Exception
    {
        byte [] testData = new byte[256];
        for (int i = 0; i < 256; i++)
            testData[i] = (byte)i;
        BufferQueue testQueue = new BufferQueue(100);
        OutputStream outStream = testQueue.getOutputStream();
        outStream.write(testData, 0, testData.length);
        outStream.close();
        InputStream inStream = testQueue.getInputStream();
        byte [] checkData = new byte[256];
        assertEquals(256, inStream.read(checkData));
        for (int i = 0; i < 256; i++)
            assertEquals(testData[i], checkData[i]);
    }
    public void testGrowingBuffers()
    throws Exception
    {
        BufferQueue testQueue = new BufferQueue(300);
        BufferQueueOutputStream outStream = testQueue.getOutputStream();
        GrowingBuffersSource source = new GrowingBuffersSource(outStream);
        Thread sourceThread = new Thread(source);
        sourceThread.start();
        
        InputStream inStream = testQueue.getInputStream();
        for (int i = 1; i < 256; i++)
        {
            byte [] bytes = new byte[i];
            assertEquals(bytes.length, inStream.read(bytes));
            for (int j = 0; j < i; j++)
                assertEquals((byte)i, bytes[j]);

        }
    }
    
    public void testGrowingBuffersReadSingleByte()
    throws Exception
    {
        BufferQueue testQueue = new BufferQueue(300);
        BufferQueueOutputStream outStream = testQueue.getOutputStream();
        GrowingBuffersSource source = new GrowingBuffersSource(outStream);
        Thread sourceThread = new Thread(source);
        sourceThread.start();
        
        InputStream inStream = testQueue.getInputStream();
        for (int i = 1; i < 256; i++)
        {
            for (int j = 0; j < i; j++)
                assertEquals((byte)i, (byte)inStream.read());
        }
    }
    
    
}
