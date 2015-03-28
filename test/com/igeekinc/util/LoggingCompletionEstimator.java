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
package com.igeekinc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.igeekinc.util.logging.ErrorLogMessage;

public class LoggingCompletionEstimator extends CompletionEstimator
{

    private PrintWriter logWriter;

	public LoggingCompletionEstimator(long [] totalItems)
    {
		super(totalItems);
		File logWriterFile = new File("/tmp/lce-"+System.currentTimeMillis());
		PrintWriter writer;
		try
		{
			writer = new PrintWriter(logWriterFile);
		} catch (FileNotFoundException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InternalError("Could not create log file "+logWriterFile);
		}
		setLogWriter(writer);
    }
	
	public void close()
	{
		logWriter.close();
	}
    
	class ItemsHandledElement
	{
		long time;
		long [] itemsHandled;
	}
	
	public LoggingCompletionEstimator(File importLog) throws IOException
	{
		BufferedReader logReader = new BufferedReader(new FileReader(importLog));
		String totalsString = logReader.readLine();
		StringTokenizer tokenizer = new StringTokenizer(totalsString, " ");
		int numTotalItems = tokenizer.countTokens();
		long [] totalItems = new long[numTotalItems];

		int curTotalItemNum = 0;
		while (tokenizer.hasMoreElements())
		{
			String totalItemStr = tokenizer.nextToken();
			Long totalItem = Long.parseLong(totalItemStr);
			totalItems[curTotalItemNum] = totalItem;
			curTotalItemNum++;
		}

		init(totalItems);
		
		int percentDeltas[] = new int[21];
		for (int deltaNum = 0; deltaNum < percentDeltas.length; deltaNum++)
			percentDeltas[deltaNum] = 0;
		
		ArrayList<ItemsHandledElement>itemsList = new ArrayList<ItemsHandledElement>();
		String curLine;
		while((curLine = logReader.readLine()) != null)
		{
			tokenizer = new StringTokenizer(curLine, " ");
			int numItemsHandled = tokenizer.countTokens() - 1;
			String currentTimeString = tokenizer.nextToken();
			long currentTime = Long.parseLong(currentTimeString);
			long [] itemsHandled = new long[numItemsHandled];
			for (int curItemNum = 0; curItemNum < numItemsHandled; curItemNum++)
			{
				String curItemString = tokenizer.nextToken();
				itemsHandled[curItemNum] = Long.parseLong(curItemString);
			}
			//getNextEstimate(itemsHandled, currentTime);
			ItemsHandledElement curElement = new ItemsHandledElement();
			curElement.time = currentTime;
			curElement.itemsHandled = itemsHandled;
			itemsList.add(curElement);
		}
		logReader.close();
		long startTime = itemsList.get(0).time;
		long finishedTime = itemsList.get(itemsList.size() - 1).time;
		long trueElapsed = finishedTime - startTime;
		for (int curElementNum = 0; curElementNum < itemsList.size(); curElementNum++)
		{
			ItemsHandledElement curElement = itemsList.get(curElementNum);

			long currentTime = curElement.time;
			long curEstimatedTime = getNextEstimate(curElement.itemsHandled, currentTime);
			
			long actualRemainingTime = finishedTime - currentTime;
			long delta = curEstimatedTime - actualRemainingTime;
			double percentDelta;
			if (actualRemainingTime > 0)
				percentDelta = (double)delta/(double)actualRemainingTime;
			else
				percentDelta = 1;
			int percentDeltaInt = (int)(10 * percentDelta);	// We normalize to -10 to +10
			if (percentDeltaInt < -10)
				percentDeltaInt = -10;
			if (percentDeltaInt > 10)
				percentDeltaInt = 10;
			int index = percentDeltaInt + 10;
			percentDeltas[index] ++;
		}
		System.out.println("Delta samples:");
		for (int deltaNum = 0; deltaNum < percentDeltas.length; deltaNum++)
		{
			System.out.println(((deltaNum - 10)*10)+"% :"+percentDeltas[deltaNum]);
		}
	}
	
    public void setLogWriter(PrintWriter logWriter)
    {
    	this.logWriter = logWriter;
    	String totalsString = "";
    	for (int curTotalNum = 0; curTotalNum < totalItems.length; curTotalNum++)
    	{
    		totalsString += Long.toString(totalItems[curTotalNum]);
    		totalsString += " ";
    	}
    	logWriter.println(totalsString);
    }

	@Override
	protected long getNextEstimate(long[] itemsHandled, long currentTime)
	{
    	if (logWriter != null)
    	{
    		String itemsString = Long.toString(currentTime);
    		for (int curItemNum = 0; curItemNum < itemsHandled.length; curItemNum++)
    		{
    			itemsString += " "+Long.toString(itemsHandled[curItemNum]);
    		}
    		logWriter.println(itemsString);
    	}
		return super.getNextEstimate(itemsHandled, currentTime);
	}
    
    public static void main(String [] args) throws IOException
    {
    	File logFile = new File(args[0]);
    	LoggingCompletionEstimator checkEstimator = new LoggingCompletionEstimator(logFile);
    }
}
