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

import junit.framework.Test;
import junit.framework.TestSuite;

import com.igeekinc.util.datadescriptor.BasicDataDescriptorTest;
import com.igeekinc.util.datadescriptor.CompositeDataDescriptorTest;
import com.igeekinc.util.formats.splitfile.SplitFileOutputStreamTest;
import com.igeekinc.util.objectcache.LRUQueueTest;
import com.igeekinc.util.rules.IncludeExcludeRuleTest;
import com.igeekinc.util.rules.RulesTest;
import com.igeekinc.util.rules.XMLRuleEncodingDecodingTest;
import com.igeekinc.util.xmlserial.XMLDocSerializerTest;
import com.igeekinc.util.xmlserial.XMLSerializableIntegerTest;
import com.igeekinc.util.xmlserial.XMLToObjectHandlerTest;
import com.igeekinc.util.xmlserial.serializers.ArraySerializeHandlerTest;
import com.igeekinc.util.xmlserial.serializers.FilePathSerializerHandlerTest;


public class AllTests
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("Test for com.igeekinc.util");
        //$JUnit-BEGIN$
        //suite.addTestSuite(SimpleFSTest.class);
        suite.addTestSuite(DateComparatorTest.class);
        suite.addTestSuite(AES.class);
        suite.addTestSuite(XMLUtilsTest.class);
        //suite.addTestSuite(SystemTest.class);
        //suite.addTestSuite(SystemInfoTest.class);
        suite.addTestSuite(EncryptedFileFormatTest.class);
        suite.addTestSuite(EventDeliverySupportTest.class);
        suite.addTestSuite(FileCopyTest.class);
        suite.addTestSuite(BufferQueueTest.class);
        suite.addTestSuite(EncryptionKeysTest.class);
        suite.addTestSuite(FilePathTest.class);
        //suite.addTestSuite(CompletionEstimatorTest.class);
        suite.addTestSuite(SHA1HashIDTest.class);
        suite.addTestSuite(BitTwiddleTest.class);
        suite.addTestSuite(MessageFormatTest.class);
        
        suite.addTestSuite(SplitFileOutputStreamTest.class);
        
        suite.addTestSuite(LRUQueueTest.class);
        
        
        suite.addTestSuite(IncludeExcludeRuleTest.class);
        suite.addTestSuite(RulesTest.class);
        suite.addTestSuite(XMLRuleEncodingDecodingTest.class);
        
        
        suite.addTestSuite(XMLDocSerializerTest.class);
        suite.addTestSuite(XMLSerializableIntegerTest.class);
        suite.addTestSuite(XMLToObjectHandlerTest.class);
        
        
        suite.addTestSuite(ArraySerializeHandlerTest.class);
        suite.addTestSuite(FilePathSerializerHandlerTest.class);
        
        suite.addTestSuite(BasicDataDescriptorTest.class);
        suite.addTestSuite(CompositeDataDescriptorTest.class);
        //$JUnit-END$
        return suite;
    }
}
