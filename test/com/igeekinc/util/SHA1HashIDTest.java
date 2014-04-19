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

import java.util.Arrays;

import com.igeekinc.junitext.iGeekTestCase;

public class SHA1HashIDTest extends iGeekTestCase 
{
	private static final String kAllFs = "ffffffffffffffffffffffffffffffffffffffff";
    private static final String kSimpleTestDataHash = "47b46f58748e674512b192cd29a5622623a450e1";
    String simpleTestDataStr = "Now is the time for all good men to come to the aid of their country.  The quick red fox jumps over the lazy brown dog.";
	byte [] simpleTestHash = {0x47, (byte)0xb4, 0x6f, 0x58, 0x74, (byte)0x8e, 0x67, 0x45, 0x12, (byte)0xb1, (byte)0x92, (byte)0xcd, 0x29, (byte)0xa5, 0x62, 0x26, 0x23, (byte)0xa4, 0x50, (byte)0xe1};
	public byte [] getSimpleTestData()
	{
		return simpleTestDataStr.getBytes();
	}
	/*
	 * Class under test for void SHA1HashID()
	 */
	public void testSHA1HashID() 
	{
		SHA1HashID testID = new SHA1HashID();	// Just check to make sure the constructor works
		testID.update(getSimpleTestData());
		testID.finalizeHash();
		byte [] data = testID.getHashData();
		assertTrue(Arrays.equals(data, simpleTestHash));
	}

	/*
	 * Class under test for void SHA1HashID(byte[])
	 */
	public void testSHA1HashIDbyteArray() 
	{
		SHA1HashID testID = new SHA1HashID(getSimpleTestData());
		byte [] data = testID.getHashData();
		assertTrue(Arrays.equals(data, simpleTestHash));
	}

	public void testSHA1HashIDstring()
	{
		SHA1HashID testID1 = new SHA1HashID(kSimpleTestDataHash);
		SHA1HashID testID2 = new SHA1HashID(getSimpleTestData());
		assertEquals(testID1, testID2);
		assertEquals(kSimpleTestDataHash, testID1.toString());
		assertEquals(kSimpleTestDataHash, testID1.toString(16));
		boolean illegalArgExceptionCaught = false;
		try
		{
			SHA1HashID testIDshort = new SHA1HashID("47b46f58748e674512b192cd29a5622623a450e");
			
		}
		catch (IllegalArgumentException e)
		{
			illegalArgExceptionCaught = true;
		}
		
		assertTrue(illegalArgExceptionCaught);
		illegalArgExceptionCaught = false;
		try
		{
			SHA1HashID testIDlong = new SHA1HashID("47b46f58748e674512b192cd29a5622623a450e1a");
		}
		catch (IllegalArgumentException e)
		{
			illegalArgExceptionCaught = true;
		}
		
		SHA1HashID testMaxInt = new SHA1HashID(kAllFs);
		assertEquals(kAllFs, testMaxInt.toString());
		assertEquals(kAllFs, testMaxInt.toString(16));
	}
	/*
	 * Class under test for void update(byte)
	 */
	public void testUpdatebyte() 
	{
		SHA1HashID testID = new SHA1HashID();	// Just check to make sure the constructor works
		byte [] testData = getSimpleTestData();
		for (int curByteNum = 0; curByteNum < testData.length; curByteNum++)
		{
			testID.update(testData[curByteNum]);
		}
		testID.finalizeHash();
		byte [] data = testID.getHashData();
		assertTrue(Arrays.equals(data, simpleTestHash));
		
		boolean illegalArgExceptionCaught = false;
		try
		{
			testID.update(testData[0]);
		}
		catch (IllegalArgumentException e)
		{
			illegalArgExceptionCaught = true;
		}
		assertTrue(illegalArgExceptionCaught);
	}

	/*
	 * Class under test for void update(byte[])
	 */
	public void testUpdatebyteArray() 
	{
		SHA1HashID testID = new SHA1HashID();	// Just check to make sure the constructor works
		byte [] testData = getSimpleTestData();
		byte [] copyBuf = new byte[4];
		for (int curByteNum = 0; curByteNum < testData.length; curByteNum+=4)
		{
			int bytesToCopy = 4;
			if (curByteNum+4 > testData.length)
			{
				bytesToCopy = testData.length-curByteNum;
				copyBuf = new byte[bytesToCopy];
			}
			System.arraycopy(testData, curByteNum, copyBuf, 0, bytesToCopy);
			testID.update(copyBuf);
		}
		testID.finalizeHash();
		byte [] data = testID.getHashData();
		assertTrue(Arrays.equals(data, simpleTestHash));
		boolean illegalArgExceptionCaught = false;
		try
		{
			testID.update(testData);
		}
		catch (IllegalArgumentException e)
		{
			illegalArgExceptionCaught = true;
		}
		assertTrue(illegalArgExceptionCaught);
	}

	/*
	 * Class under test for void update(byte[], int, int)
	 */
	public void testUpdatebyteArrayintint() 
	{
		SHA1HashID testID = new SHA1HashID();	// Just check to make sure the constructor works
		byte [] testData = getSimpleTestData();
		for (int curByteNum = 0; curByteNum < testData.length; curByteNum+=4)
		{
			int bytesToCopy = 4;
			if (curByteNum+4 > testData.length)
			{
				bytesToCopy = testData.length-curByteNum;
			}
			testID.update(testData, curByteNum, bytesToCopy);
		}
		testID.finalizeHash();
		byte [] data = testID.getHashData();
		assertTrue(Arrays.equals(data, simpleTestHash));
		boolean illegalArgExceptionCaught = false;
		try
		{
			testID.update(testData, 0, testData.length);
		}
		catch (IllegalArgumentException e)
		{
			illegalArgExceptionCaught = true;
		}
		assertTrue(illegalArgExceptionCaught);
	}

	public void testGetHashData() 
	{
		SHA1HashID testID = new SHA1HashID();	// Just check to make sure the constructor works
		testID.update(getSimpleTestData());
		testID.finalizeHash();
		byte [] data = testID.getHashData();
		assertTrue(Arrays.equals(data, simpleTestHash));
	}

	/*
	 * Class under test for boolean equals(Object)
	 */
	public void testEqualsObject() 
	{
		SHA1HashID testID1 = new SHA1HashID(getSimpleTestData());
		SHA1HashID testID2 = new SHA1HashID(getSimpleTestData());
		assertEquals(testID1, testID2);
		SHA1HashID testID3 = new SHA1HashID(testID1.getHashData());
		assertFalse(testID2.equals(testID3));
		assertFalse(testID2.equals(simpleTestDataStr));
	}

	/*
	 * Class under test for boolean equals(SHA1HashID)
	 */
	public void testEqualsSHA1HashID() 
	{
		SHA1HashID testID1 = new SHA1HashID(getSimpleTestData());
		SHA1HashID testID2 = new SHA1HashID(getSimpleTestData());
		assertEquals(testID1, testID2);
	}

	/*
	 * Class under test for String toString()
	 */
	public void testToString() 
	{
		SHA1HashID testID1 = new SHA1HashID(getSimpleTestData());
		String stringVal = testID1.toString();
		assertEquals(kSimpleTestDataHash, stringVal);
		assertEquals(stringVal, testID1.toString());
	}
}
