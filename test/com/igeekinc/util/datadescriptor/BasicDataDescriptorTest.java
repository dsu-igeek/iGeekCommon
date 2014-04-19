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

package com.igeekinc.util.datadescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import junit.framework.TestCase;

import com.igeekinc.testutils.TestFilesTool;

/**
 * @author David Smith-Uchida
 * Copyright (C) 2005 iGeek, Inc.
 */
public class BasicDataDescriptorTest extends TestCase
{
private static final int	kOneMegabyte	= 1024*1024;
private static final int	kTestFileSize	= kOneMegabyte * 1024;
/*
    public BasicDataDescriptorTest()
	{
		int cryptixPos =
			Security.insertProviderAt(
				new cryptix.jce.provider.CryptixCrypto(),
				2);
		try
		{
		  Provider[] providers = Security.getProviders();
		  for( int i=0; i<providers.length; i++ )
		  {
		   System.out.println( "Provider: " + providers[ i ].getName() + ", " + providers[ i ].getInfo() );
		   for( Iterator itr = providers[ i ].keySet().iterator(); 
		itr.hasNext(); )
		   {
			 String key = ( String )itr.next();
			 String value = ( String )providers[ i ].get( key );
			 System.out.println( "\t" + key + " = " + value );
		   }
		  }
		}
		catch( Exception e )
		{
		  e.printStackTrace();
		}
	}
	*/
    /*
     * Class under test for void BasicDataDescriptor(byte[])
     */
    public void testBasicDataDescriptorbyteArray() throws IOException
    {
        byte [] test10 = new byte[10];
        byte [] test100 = new byte[100];
        byte [] test1024 = new byte[1024];
        byte [] test1M = new byte[kOneMegabyte];
        fillInArray(test10);
        fillInArray(test100);
        fillInArray(test1024);
        fillInArray(test1M);
        
        BasicDataDescriptor bdd10 = new BasicDataDescriptor(test10);
        checkDataDescriptor(bdd10);
        BasicDataDescriptor bdd100 = new BasicDataDescriptor(test100);
        checkDataDescriptor(bdd100);
        BasicDataDescriptor bdd1024 = new BasicDataDescriptor(test1024);
        checkDataDescriptor(bdd1024);
        BasicDataDescriptor bdd1M = new BasicDataDescriptor(test1M);
        checkDataDescriptor(bdd1M);
    }

    public void testFileInputPerformance()  throws IOException
    {
    	File testFile = File.createTempFile("tfip", "dat");
    	TestFilesTool.createTestFile(testFile, kTestFileSize);	// 1GB
    	FileInputStream testStream = new FileInputStream(testFile);
    	long startTime = System.currentTimeMillis();
    	long bytesRead = 0;
    	while (bytesRead < kTestFileSize)
    	{
    		BasicDataDescriptor testDescriptor = new BasicDataDescriptor(testStream, kOneMegabyte);
    		bytesRead += testDescriptor.getLength();
    	}
    	testStream.close();
    	long endTime = System.currentTimeMillis();
    	long elapsed = endTime - startTime;
    	System.out.println("Time to read 1GB via InputStream = "+elapsed+" ms");
    	double bytesPerSecond = bytesRead / elapsed * 1000.0;
    	System.out.println("Bytes per second = "+bytesPerSecond);
    	testFile.delete();
    }
    
    public void testFileChannelPerformance()  throws IOException
    {
    	File testFile = File.createTempFile("tfip", "dat");
    	TestFilesTool.createTestFile(testFile, kTestFileSize);	// 1GB
    	FileChannel testChannel = new FileInputStream(testFile).getChannel();
    	long startTime = System.currentTimeMillis();
    	long bytesRead = 0;
    	while (bytesRead < kTestFileSize)
    	{
    		BasicDataDescriptor testDescriptor = new BasicDataDescriptor(testChannel, kOneMegabyte);
    		bytesRead += testDescriptor.getLength();
    	}
    	testChannel.close();
    	long endTime = System.currentTimeMillis();
    	long elapsed = endTime - startTime;
    	System.out.println("Time to read 1GB via FileChannel = "+elapsed+" ms");
    	double bytesPerSecond = bytesRead / elapsed * 1000.0;
    	System.out.println("Bytes per second = "+bytesPerSecond);
    	testFile.delete();
    }
    
    public void testMemoryMappedPerformance()  throws IOException
    {
    	File testFile = File.createTempFile("tfip", "dat");
    	TestFilesTool.createTestFile(testFile, kTestFileSize);	// 1GB
    	FileChannel testChannel = new FileInputStream(testFile).getChannel();
    	long startTime = System.currentTimeMillis();
    	long bytesRead = 0;
    	while (bytesRead < kTestFileSize)
    	{
    		MappedByteBuffer curBuffer = testChannel.map(MapMode.READ_ONLY, bytesRead, kOneMegabyte);
    		curBuffer.load();
    		BasicDataDescriptor testDescriptor = new BasicDataDescriptor(curBuffer);
    		bytesRead += testDescriptor.getLength();
    	}
    	testChannel.close();
    	long endTime = System.currentTimeMillis();
    	long elapsed = endTime - startTime;
    	System.out.println("Time to read 1GB via FileChannel = "+elapsed+" ms");
    	double bytesPerSecond = bytesRead / elapsed * 1000.0;
    	System.out.println("Bytes per second = "+bytesPerSecond);
    	testFile.delete();
    }
    void fillInArray(byte [] arrayToFill)
    {
        for (int curByteNum = 0; curByteNum < arrayToFill.length; curByteNum++)
            arrayToFill[curByteNum] = (byte)curByteNum;
    }
    
    void checkDataDescriptor(DataDescriptor descriptorToCheck) throws IOException
    {
    	byte [] curByte = new byte[1];
    	for (int curByteNum = 0; curByteNum < descriptorToCheck.getLength(); curByteNum++)
    	{
    		descriptorToCheck.getData(curByte, 0, curByteNum, 1, false);
    		assertEquals((byte)curByteNum, curByte[0]);
    	}
    }
    /*
     * Class under test for void BasicDataDescriptor(byte[], int, int)
     */
    public void testBasicDataDescriptorbyteArrayintint()
    {
    }

}
