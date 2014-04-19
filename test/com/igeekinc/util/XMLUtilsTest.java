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

import java.io.IOException;
import java.util.Hashtable;

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.igeekinc.junitext.iGeekTestCase;

public class XMLUtilsTest extends iGeekTestCase
{

    private static final String kTest1Val = "test1";
    private static final String kTest1NodeName = "test1Elem";
    private static final String kHasGTNodeName = "hasGT";
    private static final String kHasLTNodeName = "hasLT";
    private static final String kHasNewlineNodeName = "hasNewline";
    private static final String kJapaneseNodeName = "japanese";
    private static final String kReg1NodeName = "reg1";
    private static final String kGT_FILE = "gt>.file";
    private static final String kLT_FILE = "lt<.file";
    private static final String kFIRST_SECOND_TXT = "first\nsecond.txt";
    private static final String kJAPANESE_RTF = "﻿お読みください.rtf";
    private static final String kREG1_TXT = "reg1.txt";

    public void testAppendSingleValElement()
    {
        Document testDocument = new DocumentImpl();
        Element rootElem = testDocument.createElement("Test"); //$NON-NLS-1$
        testDocument.appendChild(rootElem);
        XMLUtils.appendSingleValElement(testDocument, rootElem, kTest1NodeName, kTest1Val);
        String test1Str = XMLUtils.getNodeValue(rootElem, kTest1NodeName);
        assertEquals(kTest1Val, test1Str);
    }

    public void testAppendSingleValElementEncoded()
    {
        Document testDocument = new DocumentImpl();
        Element rootElem = testDocument.createElement("Test"); //$NON-NLS-1$
        testDocument.appendChild(rootElem);
        XMLUtils.appendSingleValElementEncoded(testDocument, rootElem, kReg1NodeName, kREG1_TXT);
        XMLUtils.appendSingleValElementEncoded(testDocument, rootElem, kJapaneseNodeName, kJAPANESE_RTF);
        XMLUtils.appendSingleValElementEncoded(testDocument, rootElem, kHasNewlineNodeName, kFIRST_SECOND_TXT);
        XMLUtils.appendSingleValElementEncoded(testDocument, rootElem, kHasLTNodeName, kLT_FILE);
        XMLUtils.appendSingleValElementEncoded(testDocument, rootElem, kHasGTNodeName, kGT_FILE);
        
        String reg1Str = XMLUtils.getNodeValue(rootElem, kReg1NodeName);
        String japaneseStr = XMLUtils.getNodeValue(rootElem, kJapaneseNodeName);
        String newlineStr = XMLUtils.getNodeValue(rootElem, kHasNewlineNodeName);
        String ltStr = XMLUtils.getNodeValue(rootElem, kHasLTNodeName);
        String gtStr = XMLUtils.getNodeValue(rootElem, kHasGTNodeName);
        
        assertEquals(kREG1_TXT, reg1Str);
        assertEquals(kJAPANESE_RTF, japaneseStr);
        assertEquals(kFIRST_SECOND_TXT, newlineStr);
        assertEquals(kLT_FILE, ltStr);
        assertEquals(kGT_FILE, gtStr);
    }

    public void testOutputTextDoc()
    {
    }

    public void testOutputNode()
    {
    }

    /*
     * Class under test for Document getDocument(String)
     */
    public void testGetDocumentString()
    throws Exception
    {
        Document testDocument = XMLUtils.getDocument("<test><node1>hi there</node1></test>");
        Element root = testDocument.getDocumentElement();
        String node1Str = XMLUtils.getNodeValue(root, "node1");
        assertEquals(node1Str, "hi there");
    }

    /*
     * Class under test for Document getDocument(InputStream)
     */
    public void testGetDocumentInputStream()
    {
    }

    public void testGetNodeValue() throws SAXException, IOException
    {
        Document testDocument = XMLUtils.getDocument("<test><node1>hi there</node1><node2>me me me</node2><node3>no no no</node3></test>");
        Element root = testDocument.getDocumentElement();
        assertEquals("hi there", XMLUtils.getNodeValue(root, "node1"));
    }

    public void testGetElementByName()
    {
    }

    public void testParseDateNode()
    {
    }

    public void testAppendDateNode()
    {
    }

    public void testFillHashtable()
    throws Exception
    {
        Document testDocument = XMLUtils.getDocument("<test><node1>hi there</node1><node2>me me me</node2><node3>no no no</node3></test>");
        Element root = testDocument.getDocumentElement();
        NodeList rootChildren = root.getChildNodes();
        Hashtable testTable = new Hashtable();
        XMLUtils.fillHashtable(rootChildren, testTable);
        assertEquals("hi there", testTable.get("node1"));
        assertEquals("me me me", testTable.get("node2"));
        assertEquals("no no no", testTable.get("node3"));
    }

    public void testSerializeToFile()
    {
    }

    public void testSerializeToStream()
    {
    }

}
