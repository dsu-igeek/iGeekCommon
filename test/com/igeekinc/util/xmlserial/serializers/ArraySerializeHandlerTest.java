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
 
package com.igeekinc.util.xmlserial.serializers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import com.igeekinc.util.pauseabort.AbortedException;
import com.igeekinc.util.xmlserial.XMLDocParser;
import com.igeekinc.util.xmlserial.XMLDocSerializer;
import com.igeekinc.util.xmlserial.parsehandlers.ArrayParseHandler;
import com.igeekinc.util.xmlserial.parsehandlers.StringParseHandler;

public class ArraySerializeHandlerTest extends TestCase
{
    public void testA() throws IOException, SAXException, AbortedException
    {
        String [] testStrings = {"string 0", "string 1", "string 2", "string 3"};
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        XMLDocSerializer<String[]> serializer = new XMLDocSerializer<String []>("myRoot", new ArraySerializeHandler<String>("testString", new StringSerializeHandler(true)));
        serializer.serialize(outStream, testStrings, null);
        outStream.close();
        String docString = outStream.toString("UTF-8");
        System.out.println(docString);
        
        ArrayParseHandler<String> arrayParseHandler = new ArrayParseHandler<String>("testString", new StringParseHandler());
        XMLDocParser<String []> parser = new XMLDocParser<String []>("myRoot", arrayParseHandler);
        parser.parse(docString, null);
        String [] readStrings = arrayParseHandler.getValue(new String[0]);
        assertEquals(testStrings.length, readStrings.length);
        for (int curStringNum = 0; curStringNum < testStrings.length; curStringNum++)
        {
            assertEquals(testStrings[curStringNum], readStrings[curStringNum]);
        }
    }
}
