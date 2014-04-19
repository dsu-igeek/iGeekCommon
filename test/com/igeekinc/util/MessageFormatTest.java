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

import java.text.MessageFormat;
import java.util.Date;

import com.igeekinc.junitext.iGeekTestCase;

public class MessageFormatTest extends iGeekTestCase
{
	public void testA()
	{
		String result = MessageFormat.format("{0}\nFinished at {1, date}\n{2} files copied, {3} megabytes copied\n{4, choice, 4#|4<{4, number} errors}",
				new Object[]{"Hi there", new Date(), new Long(10), new Long(100), new Integer(5)});
		logger.warn("Result = "+result);
	}
	
	public void testB()
	{
		//String formatString = Messages.getString("MainView.SyncCompletedWithErrorsMessage");
		//String formatString = "{0}\nFinished at {1, date}\n{2} files synchronized, {3} megabytes copied\n{4, choice, 4#|4<{4, number} errors}";
		String formatString = "{0}\nFinished at {1, date}\n{2} files copied, {3} megabytes copied\n{4, choice, 0#|0<{4, number} errors}";
		logger.warn("Format string = "+formatString);
		String result = MessageFormat.format(formatString, new Object[]{
				"Test", new Date(), new Long(2), new Long(3), new Integer(1)});
		logger.warn("Result = "+result);
		String result2 = MessageFormat.format(formatString,
				new Object[]{"Hi there", new Date(), new Long(10), new Long(100), new Integer(0)});
		logger.warn("Result2 = "+result2);
	}
}
