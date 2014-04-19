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
 
package com.igeekinc.util.objectcache;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.igeekinc.junitext.iGeekTestCase;

public class LRUQueueTest extends iGeekTestCase
{
    private static final int kInversionCount = 100;
    private static final int kBasicCount = 100;
    public void testBasic()
    {
        LRUQueue testQueue = new LRUQueue(kBasicCount);
        for (int testNum = 0; testNum < kBasicCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            testQueue.put(curTestInt, curTestInt.toString());
        }
        // At this point the queue should be full but no objects discarded
        // yet.  Retrieve the all of the values and make sure they are stil there
        
        for (int testNum = 0; testNum < kBasicCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            String curTestStr = (String)testQueue.get(curTestInt);
            assertNotNull(curTestStr);  // Make sure it was there  
            assertEquals(curTestInt.toString(), curTestStr);    // Make sure we got the right value back
        }
        
        // Now, add one more and make sure that the right things happen
        Integer oneMoreInt = new Integer(kBasicCount);
        testQueue.put(oneMoreInt, oneMoreInt.toString());
        
        Integer notPresentInt = new Integer(0);
        assertNull(testQueue.get(notPresentInt));
        
        // Now, make sure that the queue contains what we expect
        for (int testNum = 1; testNum < kBasicCount+1; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            String curTestStr = (String)testQueue.get(curTestInt);
            assertNotNull(curTestStr);  // Make sure it was there  
            assertEquals(curTestInt.toString(), curTestStr);    // Make sure we got the right value back
        }
    }
    
    public void testInversion()
    {
        LRUQueue testQueue = new LRUQueue(kInversionCount);
        for (int testNum = 0; testNum < kInversionCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            testQueue.put(curTestInt, curTestInt.toString());
        }
        
        // Now, go through and re-insert in inverse order
        for (int testNum = kInversionCount-1; testNum >= 0; testNum--)
        {
            Integer curKey = new Integer(testNum);
            String curVal = (String)testQueue.get(curKey);
            assertNotNull(curVal);
            testQueue.put(curKey, curVal);
        }
        
        // At this point the queue should be full but no objects discarded
        // yet.  Retrieve the all of the values and make sure they are stil there
        
        for (int testNum = 0; testNum < kInversionCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            String curTestStr = (String)testQueue.get(curTestInt);
            assertNotNull(curTestStr);  // Make sure it was there  
            assertEquals(curTestInt.toString(), curTestStr);    // Make sure we got the right value back
        }
        
// Now, add one more and make sure that the right things happen
        Integer oneMoreInt = new Integer(kInversionCount);
        testQueue.put(oneMoreInt, oneMoreInt.toString());
        
        Integer notPresentInt = new Integer(kInversionCount-1);
        assertNull(testQueue.get(notPresentInt));
        
        // Now, make sure that the queue contains what we expect
        for (int testNum = 0; testNum < kInversionCount+1; testNum++)
        {

            Integer curTestInt = new Integer(testNum);
            String curTestStr = (String)testQueue.get(curTestInt);
            if (testNum == (kInversionCount-1))
            {
                assertNull(curTestStr);
            }
            else
            {
                assertNotNull(curTestStr);  // Make sure it was there  
                assertEquals(curTestInt.toString(), curTestStr);    // Make sure we got the right value back
            }
        }
        
    }
    
    static final int kFlushCount = 100;
    public void testFlush()
    {
        LRUQueue testQueue = new LRUQueue(kFlushCount);
        for (int testNum = 0; testNum < kFlushCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            testQueue.put(curTestInt, curTestInt.toString());
        }
        // At this point the queue should be full but no objects discarded
        // yet.  Retrieve the all of the values and make sure they are stil there
        
        for (int testNum = 0; testNum < kFlushCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            String curTestStr = (String)testQueue.get(curTestInt);
            assertNotNull(curTestStr);  // Make sure it was there  
            assertEquals(curTestInt.toString(), curTestStr);    // Make sure we got the right value back
        }
        
        // Now add some more values and flush it out
        for (int testNum = kFlushCount; testNum < kFlushCount*2; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            testQueue.put(curTestInt, curTestInt.toString());
        }
        
        // Make sure nothing is there from the first batch
        // At this point the queue should be full but no objects discarded
        // yet.  Retrieve the all of the values and make sure they are stil there
        
        for (int testNum = 0; testNum < kFlushCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            String curTestStr = (String)testQueue.get(curTestInt);
            assertNull(curTestStr);  // Make sure it was there  
        }
        
        // Now, make sure that the queue contains what we expect
        for (int testNum = kFlushCount; testNum < kFlushCount * 2; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            String curTestStr = (String)testQueue.get(curTestInt);
            assertNotNull(curTestStr);  // Make sure it was there  
            assertEquals(curTestInt.toString(), curTestStr);    // Make sure we got the right value back
        }
    }
    
    public void testInversionAndFlush()
    {

        LRUQueue testQueue = new LRUQueue(kFlushCount);
        for (int testNum = 0; testNum < kFlushCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            testQueue.put(curTestInt, curTestInt.toString());
        }
        
        // Now, go through and re-insert in inverse order
        for (int testNum = kFlushCount-1; testNum >= 0; testNum--)
        {
            Integer curKey = new Integer(testNum);
            String curVal = (String)testQueue.get(curKey);
            assertNotNull(curVal);
            testQueue.put(curKey, curVal);
        }
        
        // At this point the queue should be full but no objects discarded
        // yet.  Retrieve the all of the values and make sure they are stil there
        
        for (int testNum = 0; testNum < kFlushCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            String curTestStr = (String)testQueue.get(curTestInt);
            assertNotNull(curTestStr);  // Make sure it was there  
            assertEquals(curTestInt.toString(), curTestStr);    // Make sure we got the right value back
        }
        
        // Now add some more values and flush it out
        for (int testNum = kFlushCount; testNum < kFlushCount*2; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            testQueue.put(curTestInt, curTestInt.toString());
        }
        
        // Make sure nothing is there from the first batch
        // At this point the queue should be full but no objects discarded
        // yet.  Retrieve the all of the values and make sure they are stil there
        
        for (int testNum = 0; testNum < kFlushCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            String curTestStr = (String)testQueue.get(curTestInt);
            assertNull(curTestStr);  // Make sure it was there  
        }
        
        // Now, make sure that the queue contains what we expect
        for (int testNum = kFlushCount; testNum < kFlushCount * 2; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            String curTestStr = (String)testQueue.get(curTestInt);
            assertNotNull(curTestStr);  // Make sure it was there  
            assertEquals(curTestInt.toString(), curTestStr);    // Make sure we got the right value back
        }
    }
    
    public void testClear()
    {

        LRUQueue testQueue = new LRUQueue(kBasicCount);
        for (int testNum = 0; testNum < kBasicCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            testQueue.put(curTestInt, curTestInt.toString());
        }
        assertEquals(kBasicCount, testQueue.size() );
        testQueue.clear();
        assertEquals(0, testQueue.size());
        assertTrue(testQueue.isEmpty());
        for (int testNum = 0; testNum < kBasicCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            String curTestStr = (String)testQueue.get(curTestInt);
            assertNull(curTestStr);  // Make sure it was there  
        }
    }
    
    public void testEntrySet()
    {

        LRUQueue testQueue = new LRUQueue(kBasicCount);
        for (int testNum = 0; testNum < kBasicCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            testQueue.put(curTestInt, curTestInt.toString());
        }
        assertEquals(kBasicCount, testQueue.size() );
        Set entrySet = testQueue.entrySet();
        int marks[] = new int[kBasicCount];
        Iterator entriesIterator = entrySet.iterator();
        while (entriesIterator.hasNext())
        {
            Map.Entry curEntry = (Map.Entry)entriesIterator.next();
            Integer key;
            String value;
            key = (Integer)curEntry.getKey();
            value = (String)curEntry.getValue();
            assertEquals(key.toString(), value);
            marks[key.intValue()]++;
        }
        
        for (int curCheckNum = 0; curCheckNum < kBasicCount; curCheckNum++)
            assertEquals(1, marks[curCheckNum]);    // Should be one and only one entry for each key
    }
    
    public void testSetAll()
    {
        LRUQueue testQueue = new LRUQueue(kBasicCount);
        TreeMap testMap = new TreeMap();
        for (int testNum = 0; testNum < kBasicCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            testMap.put(curTestInt, curTestInt.toString());
        }
        
        testQueue.putAll(testMap);
        
        // At this point the queue should be full but no objects discarded
        // yet.  Retrieve the all of the values and make sure they are stil there
        
        for (int testNum = 0; testNum < kBasicCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            String curTestStr = (String)testQueue.get(curTestInt);
            assertNotNull(curTestStr);  // Make sure it was there  
            assertEquals(curTestInt.toString(), curTestStr);    // Make sure we got the right value back
        }
    }
    
    public void testSetAllWithOverflow()
    {
        LRUQueue testQueue = new LRUQueue(kBasicCount);
        TreeMap testMap = new TreeMap();
        for (int testNum = 0; testNum < kBasicCount*2; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            testMap.put(curTestInt, curTestInt.toString());
        }
        
        testQueue.putAll(testMap);
         
        // We're loading twice as many values as the queue can hold,
        // in ascending order.  So 0 thru kBasicCount-1 should have been
        // discarded and kBasicCount thru kBasicCount * 2 - 1 should be set
        for (int testNum = 0; testNum < kBasicCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            String curTestStr = (String)testQueue.get(curTestInt);
            assertNull(curTestStr);  // Make sure it was not there
        }
        
        for (int testNum = kBasicCount; testNum < kBasicCount * 2; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            String curTestStr = (String)testQueue.get(curTestInt);
            assertNotNull(curTestStr);  // Make sure it was there  
            assertEquals(curTestInt.toString(), curTestStr);    // Make sure we got the right value back
        }
    }
    
    public void testGetEntries()
    {
        
        LRUQueue testQueue = new LRUQueue(kBasicCount);
        for (int testNum = 0; testNum < kBasicCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            testQueue.put(curTestInt, curTestInt.toString());
        }
        
        Collection values = testQueue.values();
        // At this point the queue should be full but no objects discarded
        // yet.  Retrieve the all of the values and make sure they are stil there
        
        for (int testNum = 0; testNum < kBasicCount; testNum++)
        {
            Integer curTestInt = new Integer(testNum);
            assertTrue(values.contains(curTestInt.toString()));
        }
    }
}
