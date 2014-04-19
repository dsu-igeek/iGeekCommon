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

import com.igeekinc.junitext.iGeekTestCase;

public class BitTwiddleTest extends iGeekTestCase
{

	public void testIntToByteArray()
	{
		for (long simpleTestValue = 1; simpleTestValue < (int)Math.pow(2, 31); simpleTestValue = simpleTestValue * 2)
		{
			byte [] testResults = new byte[4];
			BitTwiddle.intToByteArray((int)simpleTestValue, testResults, 0, BitTwiddle.kBigEndian);
			if ((long)simpleTestValue < (long)Math.pow(2, 8))
			{
				assertEquals(testResults[3], (byte)simpleTestValue);
				assertEquals(testResults[2], 0);
				assertEquals(testResults[1], 0);
				assertEquals(testResults[0], 0);
			}
			if ((long)simpleTestValue >= (long)Math.pow(2, 8) && (long)simpleTestValue <(long)Math.pow(2, 16))
			{
				assertEquals(testResults[3], 0);
				assertEquals(testResults[2], (byte)((simpleTestValue >> 8) & 0xff));
				assertEquals(testResults[1], 0);
				assertEquals(testResults[0], 0);
			}
			if ((long)simpleTestValue >= (long)Math.pow(2, 16) && (long)simpleTestValue < (long)Math.pow(2, 24))
			{
				assertEquals(testResults[3], 0);
				assertEquals(testResults[2], 0);
				assertEquals(testResults[1], (byte)((simpleTestValue >> 16) & 0xff));
				assertEquals(testResults[0], 0);
			}
			if ((long)simpleTestValue >= (long)Math.pow(2, 24) && (long)simpleTestValue <= (long)Math.pow(2, 31))
			{
				assertEquals(testResults[3], 0);
				assertEquals(testResults[2], 0);
				assertEquals(testResults[1], 0);
				assertEquals(testResults[0], (byte)((simpleTestValue >> 24) & 0xff));
			}
		}
	}

	public void testByteArrayToInt()
	{
	}

	public void testShortToByteArray()
	{
	}

	public void testByteArrayToShort()
	{
	}

	public void testLongToByteArray()
	{
	}

	public void testByteArrayToLong()
	{
	}

    public void testLongs()
    {
        long [] testLongs = {0x1111111111111111L, 0x0000000000000001L, 0x100000000000000L,
                0x0102030405060708L, 0x8070605040302010L};
        for (int curTestLongNum = 0; curTestLongNum < testLongs.length; curTestLongNum++)
        {
            byte [] longArray = new byte[8];
            BitTwiddle.longToJavaByteArray(testLongs[curTestLongNum], longArray, 0);
            long testBack = BitTwiddle.javaByteArrayToLong(longArray, 0);
            assertEquals(testLongs[curTestLongNum], testBack);
        }
    }
	/*
	 * Class to test for String toHexString(byte, int)
	 */
	public void testToHexStringbyteint()
	{
	}

	/*
	 * Class to test for String toHexString(int, int)
	 */
	public void testToHexStringintint()
	{
	}

	/*
	 * Class to test for String toHexString(long, int)
	 */
	public void testToHexStringlongint()
	{
	}

	/*
	 * Class to test for String toDecString(byte, int)
	 */
	public void testToDecStringbyteint()
	{
	}

	/*
	 * Class to test for String toDecString(int, int)
	 */
	public void testToDecStringintint()
	{
	}

	/*
	 * Class to test for String toDecString(long, int)
	 */
	public void testToDecStringlongint()
	{
	}

	public void testLongFromHighLowInt()
	{
		assertEquals(0x000000007fffffffL, BitTwiddle.longFromHighLowInt(0, Integer.MAX_VALUE));
		assertEquals(0x00000000ffffffffL, BitTwiddle.longFromHighLowInt(0, 0xffffffff));
		assertEquals(0x0000000100000001L, BitTwiddle.longFromHighLowInt(1, 1));
		assertEquals(0xffffffffffffffffL, BitTwiddle.longFromHighLowInt(0xffffffff, 0xffffffff));
	
	
	}
	
	public void testHighLogIntFromLong()
	{
		assertEquals(0xffffffff, BitTwiddle.lowIntFromLong(0x0000000ffffffffL));
		assertEquals(0x0, BitTwiddle.highIntFromLong(0x0000000ffffffffL));
		assertEquals(0x00000001, BitTwiddle.lowIntFromLong(0xffffffff00000001L));
		assertEquals(0xffffffff, BitTwiddle.highIntFromLong(0xffffffff00000001L));
	}
}
