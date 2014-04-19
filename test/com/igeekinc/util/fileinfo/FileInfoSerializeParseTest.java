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
 
package com.igeekinc.util.fileinfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import com.igeekinc.util.GenericTuple;
import com.igeekinc.util.pauseabort.AbortedException;
import com.igeekinc.util.rules.ExtensionEqualsRule;
import com.igeekinc.util.rules.Rule;
import com.igeekinc.util.xmlserial.XMLDocParser;
import com.igeekinc.util.xmlserial.XMLDocSerializer;

public class FileInfoSerializeParseTest  extends TestCase
{
    public void testA() throws IOException, SAXException, AbortedException
    {
        FileInfoDB testDB = new FileInfoDB();
        FileGroup excelGroup = FileGroup.getElementForPath("com.microsoft.Excel", true);
        Rule [] excelRules =  new Rule []{new ExtensionEqualsRule("xls", true, false, false)};
        GenericTuple<Locale, String>[] descriptions = new GenericTuple[]{
                new GenericTuple<Locale, String>(new Locale("en"), "Microsoft Excel Workbook")
        };
        FileInfo excelFileInfo = new FileInfo(FileClass.kDocument, excelGroup, excelRules, descriptions);
        testDB.addFileInfo(excelFileInfo);
        
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        XMLDocSerializer<FileInfoDB> serializer = new XMLDocSerializer<FileInfoDB>("FileInfoDB", new FileInfoDBSerializeHandler());
        serializer.serialize(outStream, testDB, null);
        outStream.close();
        String docString = outStream.toString("UTF-8");
        System.out.println(docString);
        
        FileInfoDBParseHandler fileInfoDBParseHandler = new FileInfoDBParseHandler();
        XMLDocParser<FileInfoDB> parser = new XMLDocParser<FileInfoDB>("FileInfoDB", fileInfoDBParseHandler);
        FileInfoDB parsedDB = parser.parse(docString, null);

        assertEquals(testDB.allFileInfo.size(), parsedDB.allFileInfo.size());
    }
}
