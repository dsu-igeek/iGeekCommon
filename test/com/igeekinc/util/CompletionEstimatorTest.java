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

public class CompletionEstimatorTest extends iGeekTestCase
{
    private static final int	kTotalMS	= 10000000;

	public void testSingleRateFakeTime()
    {
        CompletionEstimator testEstimator = new CompletionEstimator(new long [] {kTotalMS/1000});
        long [] itemsCompleted = {1};
        testEstimator.getNextEstimate(itemsCompleted, 0);
        for (long curTime = 1000; curTime < kTotalMS; curTime+= 1000)
        {
            long timeExpected = kTotalMS - (curTime+1000);   // One item was completed at time 0
            long timeEstimated = testEstimator.getNextEstimate(itemsCompleted, curTime);
            if (Math.abs(timeExpected - timeEstimated) > 2000)    // Allow max drift of 2000
                assertEquals(timeExpected/1000, timeEstimated/1000);
        }
    }
    
    public void testDoubleRateFakeTime()
    {
        CompletionEstimator testEstimator = new CompletionEstimator(new long [] {kTotalMS/1000, kTotalMS/1000});
        long [] itemsCompleted = {1,2};
        testEstimator.getNextEstimate(itemsCompleted, 0);
        for (long curTime = 1000; curTime < kTotalMS; curTime+= 1000)
        {
            long timeExpected = kTotalMS - (curTime+1000);   // One item was completed at time 0
            long timeEstimated = testEstimator.getNextEstimate(itemsCompleted, curTime);
            if (Math.abs(timeExpected - timeEstimated) > 2000)    // Allow max drift of 2000
                assertEquals(timeExpected/1000, timeEstimated/1000);
        }
    }
    
    public static final int kRealTimeMS = 100*1000;
    public void testSingleRateRealTime()
    throws Exception
    {
        CompletionEstimator testEstimator = new CompletionEstimator(new long [] {kRealTimeMS/1000});
        long [] itemsCompleted = {1};
        testEstimator.getNextEstimate(itemsCompleted);
        for (long curTime = 1000; curTime < kRealTimeMS; curTime+= 1000)
        {
            Thread.sleep(1000);
            long timeExpected = kRealTimeMS - (curTime+1000);   // One item was completed at time 0
            long timeEstimated = testEstimator.getNextEstimate(itemsCompleted);
            if (Math.abs(timeExpected - timeEstimated) > 2000)    // Allow max drift of 2000
                assertEquals(timeExpected/1000, timeEstimated/1000);
            logger.warn("timeEstimated = "+(timeEstimated/1000)+" seconds, timeExpected = "+(timeExpected/1000));
        }
    }
}
