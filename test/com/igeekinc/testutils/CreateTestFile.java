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
import java.util.Properties;

import org.apache.log4j.Logger;

import com.igeekinc.util.SHA1HashID;
import com.igeekinc.util.logging.ErrorLogMessage;

public class CreateTestFile
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		CreateTestFile main = new CreateTestFile();
		main.run(args);
	}

	public void run(String [] args)
	{
		LongOpt [] longOptions = {
				new LongOpt("outputFile", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
				new LongOpt("length", LongOpt.REQUIRED_ARGUMENT, null, 'l'),
		};
		Getopt getOpt = new Getopt("CreateTestFile", args, "l:o:", longOptions);

		int opt;
		Properties addCASStoreProperties = new Properties();
		File outputFile = null;
		long outputLength = -1;

		while ((opt = getOpt.getopt()) != -1)
		{
			switch(opt)
			{
			case 'o': 
				String outputFileStr = getOpt.getOptarg();
				outputFile = new File(outputFileStr);
				break;
			case 'l':
				String outputLengthStr = getOpt.getOptarg();
				outputLength = Integer.parseInt(outputLengthStr);
				break;
			}
		}
		if (outputFile == null || outputLength <= 0)
		{
			System.err.println("CreateTestFile --outputFile <output file> --length <length in bytes>");
			System.exit(1);
		}
		try
		{
			SHA1HashID createHash = TestFilesTool.createTestFile(outputFile, outputLength);
			System.out.println(createHash.toString());
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			System.exit(2);
		}
	}
}
