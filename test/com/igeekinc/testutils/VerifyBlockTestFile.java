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

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.igeekinc.util.SHA1HashID;
import com.igeekinc.util.logging.ErrorLogMessage;

public class VerifyBlockTestFile
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		VerifyBlockTestFile main = new VerifyBlockTestFile();
		main.run(args);
	}

	public void run(String [] args)
	{
		BasicConfigurator.configure();
		LongOpt [] longOptions = {
				new LongOpt("checkFile", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
				new LongOpt("length", LongOpt.REQUIRED_ARGUMENT, null, 'l'),
				new LongOpt("passNum", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
				new LongOpt("blockSize", LongOpt.REQUIRED_ARGUMENT, null, 'b')
		};
		Getopt getOpt = new Getopt("VerifyBlockTestFile", args, "c:h:", longOptions);

		int opt;

		File checkFile = null;
		long checkLength = -1L;
		int passNum = 0, blockSize = 4096;
		while ((opt = getOpt.getopt()) != -1)
		{
			switch(opt)
			{
			case 'c': 
				String checkFileStr = getOpt.getOptarg();
				checkFile = new File(checkFileStr);
				break;
			case 'l':
				String outputLengthStr = getOpt.getOptarg();
				checkLength = Integer.parseInt(outputLengthStr);
				break;
			case 'p':
				String passNumStr = getOpt.getOptarg();
				passNum = Integer.parseInt(passNumStr);
				break;
			case 'b':
				String blockSizeStr = getOpt.getOptarg();
				blockSize = Integer.parseInt(blockSizeStr);
				break;
			}
		}
		if (checkFile == null || checkLength <= 0)
		{
			System.err.println("CreateTestFile --outputFile <output file> --length <length in bytes>");
			System.exit(1);
		}
		try
		{
			if (!TestFilesTool.verifyBlockFileSequential(checkFile, checkLength, blockSize, passNum))
				System.exit(1);	// Verify failed, exit with an eerror
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
	}
}
