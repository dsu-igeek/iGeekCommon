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
import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import com.igeekinc.util.FilePath;
import com.igeekinc.util.pauseabort.AbortedException;
import com.igeekinc.util.xmlserial.XMLDocParser;
import com.igeekinc.util.xmlserial.XMLDocSerializer;
import com.igeekinc.util.xmlserial.parsehandlers.FilePathParseHandler;

public class FilePathSerializerHandlerTest extends TestCase
{
    public void testSerializeFilePath() throws IOException, SAXException, AbortedException
    {
        File testFile = File.createTempFile("filepathtest", "test");
        FilePath testPath = FilePath.getFilePath(testFile);
        System.out.println("testFile path = "+testFile.getAbsolutePath());
        System.out.println("testPath path = "+testPath.toString());

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        XMLDocSerializer<FilePath> serializer = new XMLDocSerializer<FilePath>("myRoot", new FilePathSerializeHandler());
        serializer.serialize(outStream, testPath, null);
        outStream.close();
        String docString = outStream.toString("UTF-8");
        System.out.println(docString);
        
        FilePathParseHandler filePathParseHandler = new FilePathParseHandler();
        XMLDocParser<FilePath> parser = new XMLDocParser<FilePath>("myRoot", filePathParseHandler);
        parser.parse(docString, null);
        assertEquals(testPath, filePathParseHandler.getObject());
        assertEquals(testFile.getAbsolutePath(), testPath.getPath());
        assertEquals(testFile.isAbsolute(), testPath.isAbsolute());
        testFile.delete();
    }
}
