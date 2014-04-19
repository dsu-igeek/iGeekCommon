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
 
package com.igeekinc.junitext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;

import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.perf4j.log4j.AsyncCoalescingStatisticsAppender;

public class iGeekTestCase extends TestCase
{
    public static boolean isLoggingConfigured() { 
	    Enumeration appenders = Logger.getRootLogger().getAllAppenders(); 
	    if (appenders.hasMoreElements()) { 
	        return true; 
	    } 
	    else { 
	        Enumeration loggers = LogManager.getCurrentLoggers() ; 
	        while (loggers.hasMoreElements()) { 
	            Logger c = (Logger) loggers.nextElement(); 
	            if (c.getAllAppenders().hasMoreElements()) 
	                return true; 
	        } 
	    } 
	    return false; 
	}

	protected Logger logger;
    
    public iGeekTestCase()
    {
        initIGeekTestCase();
    }

	private void initIGeekTestCase() {
		logger = Logger.getLogger(getClass());
        if (!isLoggingConfigured())
        {
            // Looks like we're not configured.  Call BasicConfigurator and then log it
            BasicConfigurator.configure();
            logger.warn("log4j wasn't configured - Configured log4j with BasicConfigurator");
            logger.getRootLogger().setLevel(Level.ERROR);
    		try
    		{
    	    	ConsoleAppender console = new ConsoleAppender();
    	    	FileAppender statsLog = new FileAppender(new PatternLayout("%m%n"), "/tmp/IndelibleFSServerTest.stats");
    			statsLog.activateOptions();
    			FileAppender rawLog = new FileAppender(new PatternLayout("%m%n"), "/tmp/IndelibleFSServerTest.raw");
    			rawLog.activateOptions();
    	    	AsyncCoalescingStatisticsAppender statsAppender = new AsyncCoalescingStatisticsAppender();
    	    	statsAppender.setName("statsAppender");
    	    	statsAppender.setTimeSlice(10000);
    	    	statsAppender.addAppender(statsLog);
    	    	statsAppender.activateOptions();
    	    	Logger timingLogger = Logger.getLogger("org.perf4j.TimingLogger");
    	    	timingLogger.setLevel(Level.INFO);
    	    	timingLogger.setAdditivity(false);
    	    	timingLogger.addAppender(statsAppender);
    	    	timingLogger.addAppender(rawLog);
    	    	Logger.getRootLogger().setLevel(getLoggingLevel());
    	    	Logger.getRootLogger().addAppender(console);
    		} catch (IOException e)
    		{
    			e.printStackTrace();
    		}
        }
    }
    
	public Level getLoggingLevel()
	{
		return Level.INFO;
	}
	
    public iGeekTestCase(String name)
    {
        super(name);
        initIGeekTestCase();
    }
    
    public void waitForUserOK(String message) throws IOException
    {
        System.out.println(message);
        System.out.print("Press return to continue...");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }
}
