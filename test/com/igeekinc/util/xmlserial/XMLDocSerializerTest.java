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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.xml.sax.SAXException;

import com.igeekinc.junitext.iGeekTestCase;
import com.igeekinc.util.pauseabort.AbortedException;
import com.igeekinc.util.xmlserial.parsehandlers.IntegerParseHandler;
import com.igeekinc.util.xmlserial.serializers.IntegerSerializeHandler;

public class XMLDocSerializerTest extends iGeekTestCase
{
    public void testSerializeInteger() throws IOException, SAXException, AbortedException
    {
        Integer serializeInt = new Integer(1234);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        XMLDocSerializer<Integer> serializer = new XMLDocSerializer<Integer>("myRoot", new IntegerSerializeHandler());
        serializer.serialize(outStream, serializeInt, null);
        outStream.close();
        String docString = outStream.toString("UTF-8");
        logger.warn(docString);
        
        IntegerParseHandler integerParseHandler = new IntegerParseHandler();
        XMLDocParser<Integer> parser = new XMLDocParser<Integer>("myRoot", integerParseHandler);
        parser.parse(docString, null);
        assertEquals(serializeInt, integerParseHandler.getObject());
    }
}
