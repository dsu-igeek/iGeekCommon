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

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Enumeration;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.runner.BaseTestRunner;
import junit.textui.ResultPrinter;

import org.apache.log4j.Logger;

public class LoggerResultPrinter extends ResultPrinter 
{
	Logger logger;
	
	public LoggerResultPrinter(PrintStream printer) {
		super(printer);
		this.logger = Logger.getLogger(getClass());
	}

	
	/* Internal methods 
	 */

	protected void printHeader(long runTime) 
	{
		logger.warn("Time: "+elapsedTimeAsString(runTime));
	}
	
	protected void printErrors(TestResult result) {
		printDefects(result.errors(), result.errorCount(), "error");
	}
	
	protected void printFailures(TestResult result) {
		printDefects(result.failures(), result.failureCount(), "failure");
	}
	
	protected void printDefects(Enumeration booBoos, int count, String type) {
		if (count == 0) return;
		if (count == 1)
			logger.warn("There was " + count + " " + type + ":");
		else
			getWriter().println("There were " + count + " " + type + "s:");
		for (int i= 1; booBoos.hasMoreElements(); i++) {
			printDefect((TestFailure) booBoos.nextElement(), i);
		}
	}
	
	public void printDefect(TestFailure booBoo, int count) { // only public for testing purposes
		printDefectHeader(booBoo, count);
		printDefectTrace(booBoo);
	}

	protected void printDefectHeader(TestFailure booBoo, int count) {
		// I feel like making this a println, then adding a line giving the throwable a chance to print something
		// before we get to the stack trace.
		logger.warn(count + ") " + booBoo.failedTest());
	}

	protected void printDefectTrace(TestFailure booBoo) {
		logger.warn(BaseTestRunner.getFilteredTrace(booBoo.trace()));
	}

	protected void printFooter(TestResult result) {
		if (result.wasSuccessful()) {
			logger.warn("OK");
			logger.warn(" (" + result.runCount() + " test" + (result.runCount() == 1 ? "": "s") + ")");

		} else {
			logger.warn("FAILURES!!!");
			logger.warn("Tests run: "+result.runCount()+ 
				         ",  Failures: "+result.failureCount()+
				         ",  Errors: "+result.errorCount());
		}
	}


	/**
	 * Returns the formatted string of the elapsed time.
	 * Duplicated from BaseTestRunner. Fix it.
	 */
	protected String elapsedTimeAsString(long runTime) {
		return NumberFormat.getInstance().format((double)runTime/1000);
	}

	/**
	 * @see junit.framework.TestListener#addError(Test, Throwable)
	 */
	public void addError(Test test, Throwable t) {
		logger.warn("E");
	}

	/**
	 * @see junit.framework.TestListener#addFailure(Test, AssertionFailedError)
	 */
	public void addFailure(Test test, AssertionFailedError t) {
		logger.warn("F");
	}

	/**
	 * @see junit.framework.TestListener#endTest(Test)
	 */
	public void endTest(Test test) {
	}

	/**
	 * @see junit.framework.TestListener#startTest(Test)
	 */
	public void startTest(Test test) {
		logger.warn("Starting test "+test.toString());
	}

}

