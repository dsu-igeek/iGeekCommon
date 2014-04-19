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

import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import com.igeekinc.junitext.iGeekTestCase;

public class DateComparatorTest extends iGeekTestCase
{
    public void testBigDiffs()
    {
        Date now = new Date();
        Date oneHrFromNow = new Date(now.getTime() + 3600 * 1000);
        Date bigNext = new Date(now.getTime()+(Integer.MAX_VALUE));
        Date biggerNext = new Date(now.getTime() + ((long)Integer.MAX_VALUE) * 2);
        
        logger.warn("now = "+now);
        logger.warn("oneHrFromNow = "+now);
        logger.warn("bigNext = "+bigNext);
        logger.warn("biggerNext = "+biggerNext);
        
        DateComparator compare = new DateComparator();
        assertTrue(compare.compare(now, oneHrFromNow) < 0);
        assertTrue(compare.compare(oneHrFromNow, now) > 0);
        
        assertTrue(compare.compare(now, bigNext) < 0);
        assertTrue(compare.compare(bigNext, now) > 0);
        
        assertTrue(compare.compare(now, biggerNext) < 0);
        assertTrue(compare.compare(biggerNext, now) > 0);
    }
    
    public void testTreeOrder()
    {
        Date now = new Date();
        Date oneHrFromNow = new Date(now.getTime() + 3600 * 1000);
        Date bigNext = new Date(now.getTime()+(Integer.MAX_VALUE));
        Date biggerNext = new Date(now.getTime() + ((long)Integer.MAX_VALUE) * 2);
        
        Date [] ascending = {now, oneHrFromNow, bigNext, biggerNext};
        int totalPermutations = 4 * 3 * 2;
        Date [][] permutations = new Date[totalPermutations][4];
        int permuteNum = 0;
        for (int firstVal = 0; firstVal < 4; firstVal++)
        {
            for (int secondVal = 0; secondVal < 4; secondVal++)
            {
                if (firstVal == secondVal)
                    continue;
                for (int thirdVal = 0; thirdVal < 4; thirdVal++)
                {
                    if (firstVal == thirdVal || secondVal == thirdVal)
                        continue;
                    for (int fourthVal = 0; fourthVal < 4; fourthVal++)
                    {
                        if (firstVal == fourthVal || secondVal == fourthVal || thirdVal == fourthVal)
                            continue;
                        permutations[permuteNum][0] = ascending[firstVal];
                        permutations[permuteNum][1] = ascending[secondVal];
                        permutations[permuteNum][2] = ascending[thirdVal];
                        permutations[permuteNum][3] = ascending[fourthVal];
                        permuteNum++;
                    }
                }
            }
        }
        logger.warn("now = "+now);
        logger.warn("oneHrFromNow = "+now);
        logger.warn("bigNext = "+bigNext);
        logger.warn("biggerNext = "+biggerNext);
        
        DateComparator compare = new DateComparator();
        TreeMap tree;
        for (int curPermuteNum = 0; curPermuteNum < totalPermutations; curPermuteNum++)
        {
            tree= new TreeMap(compare);
            for (int dateNum = 0; dateNum < 4; dateNum++)
            {
                tree.put(permutations[curPermuteNum][dateNum], permutations[curPermuteNum][dateNum]);
            }
            
            Set keys = tree.keySet();
            Iterator keyIterator = keys.iterator();
            String info = "";
            String permute = "permuteNum = "+curPermuteNum;
            for (int posNum = 0; posNum < 4; posNum++)
            {
                Date curKey = (Date)keyIterator.next();
                permute = permute+posNum+" = "+permutations[curPermuteNum][posNum];
                info = info + posNum+" = "+curKey;
            }
            logger.warn(permute);
            logger.warn("keyOrder = "+info);
            keyIterator = keys.iterator();
            for (int posNum = 0; posNum < 4; posNum++)
            {
                Date curKey = (Date)keyIterator.next();
                assertEquals(curKey, ascending[posNum]);
            }
        }
        
    }
}
