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
 
package com.igeekinc.testutils;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.igeekinc.util.SHA1HashID;
import com.igeekinc.util.logging.ErrorLogMessage;

public class VerifyTestFile
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		VerifyTestFile main = new VerifyTestFile();
		main.run(args);
	}

	public void run(String [] args)
	{
		LongOpt [] longOptions = {
				new LongOpt("checkFile", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
				new LongOpt("hash", LongOpt.REQUIRED_ARGUMENT, null, 'h'),
				new LongOpt("length", LongOpt.REQUIRED_ARGUMENT, null, 'l')
		};
		Getopt getOpt = new Getopt("VerifyTestFile", args, "c:h:l:", longOptions);

		int opt;

		File checkFile = null;
		SHA1HashID checkHash = null;
		long checkLength = -1L;
		while ((opt = getOpt.getopt()) != -1)
		{
			switch(opt)
			{
			case 'c': 
				String checkFileStr = getOpt.getOptarg();
				checkFile = new File(checkFileStr);
				break;
			case 'h':
				String checkHashStr = getOpt.getOptarg();
				checkHash = new SHA1HashID(checkHashStr);
				break;
			case 'l':
				String lengthStr = getOpt.getOptarg();
				checkLength = Long.parseLong(lengthStr);
				break;
			}
		}
		if (checkFile == null || checkHash == null)
		{
			System.err.println("CheckTestFile --checkFile <check file> --hash <check hash>");
			System.exit(1);
		}
		try
		{
			if (checkLength < 0)
				checkLength = checkFile.length();
			if (!TestFilesTool.verifyFile(checkFile, checkHash, checkLength))
			{
				System.err.println(checkFile.getAbsolutePath()+" does not verify");
				System.exit(2);
			}
			System.exit(0);
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			System.exit(2);
		}
	}
}
