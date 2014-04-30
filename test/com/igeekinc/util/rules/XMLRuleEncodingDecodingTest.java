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

package com.igeekinc.util.rules;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import com.igeekinc.junitext.iGeekTestCase;

public class XMLRuleEncodingDecodingTest extends iGeekTestCase
{
	public void testBasic()
	throws Exception
	{
		DateAfterRule daRule = new DateAfterRule(new Date(), DateAfterRule.kCreatedTime);
		File tempFile = File.createTempFile("dar",".xml");
		logger.warn("Writing to "+tempFile.getAbsolutePath());
		FileOutputStream foStream = new FileOutputStream(tempFile);
		//XMLEncoder darEncoder = new XMLEncoder(foStream);
		//darEncoder.writeObject(daRule);
		//darEncoder.close();
		
	}
}
