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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * @author David Smith-Uchida
 * Copyright (C) 2005 iGeek, Inc.
 */
public class CompositeDataDescriptorTest extends TestCase
{
private static final int	kChunk200	= 200;
private static final int	k1MSize	= 1024*1024;
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
    public void testCompositeDataDescriptorbyteArray() throws IOException
    {
        byte [] test1M = new byte[k1MSize];
        fillInArray(test1M);
        ArrayList<DataDescriptor>dataDescriptors = new ArrayList<DataDescriptor>();
        for (int curOffset = 0; curOffset < k1MSize; curOffset += kChunk200)
        {
        	int curLength = 200;
        	if (curLength + curOffset > k1MSize)
        	{
        		curLength = k1MSize - curOffset;
        	}
        	BasicDataDescriptor curDescriptor = new BasicDataDescriptor(test1M, curOffset, curLength);
        	dataDescriptors.add(curDescriptor);
        }
        DataDescriptor [] baseDescriptors = new DataDescriptor[dataDescriptors.size()];
        baseDescriptors = dataDescriptors.toArray(baseDescriptors);
        CompositeDataDescriptor testDescriptor = new CompositeDataDescriptor(baseDescriptors);
        checkDataDescriptor(testDescriptor);
        byte [] check1M = new byte[k1MSize];
        assertEquals(k1MSize, testDescriptor.getData(check1M, 0, 0, k1MSize, false));
        assertTrue(Arrays.equals(test1M, check1M));
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
    
    void fillInArray(byte [] arrayToFill)
    {
        for (int curByteNum = 0; curByteNum < arrayToFill.length; curByteNum++)
            arrayToFill[curByteNum] = (byte)curByteNum;
    }
    /*
     * Class under test for void BasicDataDescriptor(byte[], int, int)
     */
    public void testBasicDataDescriptorbyteArrayintint()
    {
    }

}
