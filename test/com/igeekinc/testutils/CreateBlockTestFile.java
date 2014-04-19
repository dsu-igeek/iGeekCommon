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

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.igeekinc.util.logging.ErrorLogMessage;

public class CreateBlockTestFile
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		CreateBlockTestFile main = new CreateBlockTestFile();
		main.run(args);
	}

	public void run(String [] args)
	{
		LongOpt [] longOptions = {
				new LongOpt("outputFile", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
				new LongOpt("length", LongOpt.REQUIRED_ARGUMENT, null, 'l'),
				new LongOpt("passNum", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
				new LongOpt("blockSize", LongOpt.REQUIRED_ARGUMENT, null, 'b'),
				new LongOpt("random", LongOpt.NO_ARGUMENT, null, 'r'),
				
		};
		Getopt getOpt = new Getopt("CreateTestFile", args, "l:o:", longOptions);
		BasicConfigurator.configure();
		int opt;
		Properties addCASStoreProperties = new Properties();
		File outputFile = null;
		long outputLength = -1;
		int passNum = 0;
		int blockSize = 4096;
		boolean writeRandomly = false;
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
			case 'p':
				String passNumStr = getOpt.getOptarg();
				passNum = Integer.parseInt(passNumStr);
				break;
			case 'b':
				String blockSizeStr = getOpt.getOptarg();
				blockSize = Integer.parseInt(blockSizeStr);
				break;
			case 'r':
				writeRandomly = true;
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
			if (writeRandomly)
				TestFilesTool.createBlockFileRandomly(outputFile, outputLength, blockSize, passNum);
			else
				TestFilesTool.createBlockFileSequential(outputFile, outputLength, blockSize, passNum);
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			System.exit(2);
		}
	}
}
