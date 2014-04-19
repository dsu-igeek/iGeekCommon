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

import java.util.EventObject;

import com.igeekinc.junitext.iGeekTestCase;

public class EventDeliverySupportTest extends iGeekTestCase
{
    boolean eventReceived;
    public void testA()
    {
        EventDeliverySupport testSupport = new EventDeliverySupport(null);
        eventReceived = false;
        EventHandler testHandler = new EventHandler(){
            public void handleEvent(EventObject eventToHandle) {
                eventReceived = true; 
             }
            };
        testSupport.addEventHandler(EventObject.class, testHandler);
        
        EventObject testEvent = new EventObject(this);
        testSupport.sendEvent(testEvent);
        assertTrue(eventReceived);
        testSupport.removeEventHandler(EventObject.class, testHandler);
        eventReceived = false;
        testSupport.sendEvent(testEvent);
        assertFalse(eventReceived);
    }
    
    class Event1 extends EventObject
    {
        public Event1(Object source)
        {
            super(source);
        }
    }    
    
    class Event2 extends EventObject
    {
        public Event2(Object source)
        {
            super(source);
        }
    }
    
    boolean event1Received, event2Received;
    public void testB()
    {
        EventDeliverySupport testSupport = new EventDeliverySupport(null);
        event1Received = false;
        event2Received = false;
        EventHandler testHandler1 = new EventHandler(){
            public void handleEvent(EventObject eventToHandle) {
                event1Received = true; 
            }
        };
        
        EventHandler testHandler2 = new EventHandler(){
            public void handleEvent(EventObject eventToHandle) {
                event2Received = true; 
            }
        };
        
        testSupport.addEventHandler(Event1.class, testHandler1);
        testSupport.addEventHandler(Event2.class, testHandler2);
        Event1 testEvent1 = new Event1(this);
        
        testSupport.sendEvent(testEvent1);
        assertTrue(event1Received);
        assertFalse(event2Received);

        event1Received = false;
        event2Received = false;
        Event2 testEvent2 = new Event2(this);
        
        testSupport.sendEvent(testEvent2);
        assertFalse(event1Received);
        assertTrue(event2Received);
    }
}
