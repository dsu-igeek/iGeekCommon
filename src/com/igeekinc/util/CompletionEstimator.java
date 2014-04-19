/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.igeekinc.util;

import java.util.ArrayList;

/**
 * CompletionEstimator keeps track of the rate of completion of n different
 * types of items being completed and will produce an estimate of when the entire process
 * will be completed.<br/>
 * Internally, there are n fifos of rates (currently set to hold 10 rates each).  As
 * items are completed, a new rate for each item type is calculated and then averaged against
 * the previous rates and then this average rate is used with the number of items remaining
 * to calculate how much time completing the remaining items will take.  The longest time from
 * all the item types is returned.
 */
public class CompletionEstimator
{
    protected ArrayList<Double> [] rates;
    protected long [] itemsRemaining;
    protected long lastUpdateTime;
    protected static final int kRatesToAverage = 10;
    
    /**
     * Initialize a CompletionEstimator with the number of item types
     * to track and the number of items for each type.  The position of each type in the 
     * inItemsRemaining is the position that they need to be in in calls to
     * getNextEstimate()
     * @param numItemTypesToTrack
     * @param inItemsRemaining
     */
    @SuppressWarnings("unchecked")
	public CompletionEstimator(int numItemTypesToTrack, long [] inItemsRemaining)
    {
        if (inItemsRemaining.length != numItemTypesToTrack)
            throw new IllegalArgumentException("Number of rates to track and number of \"items remaining\" must be the same");
        rates = (ArrayList<Double>[])new Object[numItemTypesToTrack];
        itemsRemaining = new long[inItemsRemaining.length];
        
        for (int curRateNum = 0; curRateNum < numItemTypesToTrack; curRateNum++)
        {
            rates[curRateNum] = new ArrayList<Double>();
            itemsRemaining[curRateNum] = inItemsRemaining[curRateNum];
        }
        lastUpdateTime = -1;
        
    }
    
    /**
     * Calculates how much longer (in milliseconds) it will take to 
     * handle the remaining items based on how long it has been taking
     * to handle items.
     * @param itemsHandled
     * @return
     */
    public long getNextEstimate(long [] itemsHandled)
    {
        return getNextEstimate(itemsHandled, System.currentTimeMillis());
    }
    
    protected long getNextEstimate(long [] itemsHandled, long currentTime)
    {
        if (itemsHandled.length != itemsRemaining.length)
            throw new IllegalArgumentException("Number of items in itemsHandled must equal number of rates");
        for (int curItemNum = 0; curItemNum < itemsHandled.length ; curItemNum++)
        {
            itemsRemaining[curItemNum] -= itemsHandled[curItemNum];
        }
        long prevUpdateTime = lastUpdateTime;
        lastUpdateTime = currentTime;
        if (prevUpdateTime >= 0)
        {
            long timeDelta = currentTime - prevUpdateTime;
            long timeRemaining = 0;
            for (int curItemNum = 0; curItemNum < rates.length; curItemNum++)
            {
                double curRate = ((double)itemsHandled[curItemNum])/((double)timeDelta);
                ArrayList<Double> curRateQueue = rates[curItemNum];
                curRateQueue.add(new Double(curRate));
                while (curRateQueue.size() > kRatesToAverage)
                    curRateQueue.remove(0);
                double avgRate = 0.0;
                for (int curRateNum = 0; curRateNum < curRateQueue.size(); curRateNum++)
                {
                    avgRate += ((Double)curRateQueue.get(curRateNum)).doubleValue();
                }
                avgRate = avgRate/curRateQueue.size();
                long checkTimeRemaining = (long)(itemsRemaining[curItemNum]/avgRate);
                if (checkTimeRemaining > timeRemaining)
                    timeRemaining = checkTimeRemaining;
            }
            return timeRemaining;
        }
        else
            return -1;
    }
}
