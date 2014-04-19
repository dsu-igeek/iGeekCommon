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
 
package com.igeekinc.indelible.oid;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.igeekinc.util.xmlserial.XMLObjectParseHandler;
import com.igeekinc.util.xmlserial.exceptions.UnexpectedSubElementError;

public class OIDXMLParseHandler implements XMLObjectParseHandler
{

    private StringBuffer charInfo;
    private String elementName;
    private boolean encoded;
    private String charSet;
    
    public OIDXMLParseHandler()
    {

    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException
    {
        throw new UnexpectedSubElementError("Got a sub element for an Integer "+qName+"("+namespaceURI+":"+localName+")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException
    {
    }

    public void characters(char[] ch, int start, int length) throws SAXException
    {
        charInfo.append(ch, start, length);
    }
    
    public Object getObject()
    {
        return getValue();
    }
    
    public ObjectID getValue()
    {
        if (charInfo == null)
            return null;
        String idStr = charInfo.toString();
        ObjectID returnID = ObjectIDFactory.reconstituteFromString(idStr);
        return returnID;
    }

    public void init(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException
    {
        this.elementName = qName;
        charInfo = new StringBuffer(256);     
        String encVal = atts.getValue("enc"); //$NON-NLS-1$
        charSet = atts.getValue("charSet"); //$NON-NLS-1$
        if (encVal != null &&encVal.equals("t"))  //$NON-NLS-1$
            encoded = true;
    }

    public String getCharSet()
    {
        return charSet;
    }

    public String getElementName()
    {
        return elementName;
    }
}
