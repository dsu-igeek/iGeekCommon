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
 
package com.igeekinc.util.xmlserial;

import java.util.HashMap;

import com.igeekinc.junitext.iGeekTestCase;
import com.igeekinc.util.pauseabort.AbortedException;
import com.igeekinc.util.xmlserial.parsehandlers.IntegerParseHandler;
import com.igeekinc.util.xmlserial.parsehandlers.StringParseHandler;

public class XMLToObjectHandlerTest extends iGeekTestCase
{
    public void testInteger() throws AbortedException
    {
        XMLFieldParseInfo [] fieldMappings = {
                new XMLFieldParseInfo("myInt", new IntegerParseHandler(), false)
        };
        XMLToObjectHandler<HashMap<String, Object>> rootHandler = new ConcreteXMLToObjectHandler(fieldMappings);
        XMLDocParser<HashMap<String, Object>> parser = new XMLDocParser<HashMap<String, Object>>("root", rootHandler);
        HashMap<String, Object> parsedMap = parser.parse("<root><myInt>1234</myInt></root>",  null);
        Object myIntObj = parsedMap.get("myInt");
        assertNotNull(myIntObj);
        assertTrue(myIntObj instanceof Integer);
        assertEquals(1234, ((Integer)myIntObj).intValue());
    }
    
    public void testString() throws AbortedException
    {
        XMLFieldParseInfo [] fieldMappings = {
                new XMLFieldParseInfo("myString", new StringParseHandler(), false)
        };
        XMLToObjectHandler<HashMap<String, Object>> rootHandler = new ConcreteXMLToObjectHandler(fieldMappings);
        XMLDocParser<HashMap<String, Object>> parser = new XMLDocParser<HashMap<String, Object>>("root", rootHandler);
        HashMap<String, Object> parsedMap = parser.parse("<root><myString>abcd</myString></root>", null);
        Object myStringObj = parsedMap.get("myString");
        assertNotNull(myStringObj);
        assertTrue(myStringObj instanceof String);
        assertEquals("abcd", (String)myStringObj);
    }
}
