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
package com.igeekinc.testutils;

import java.net.URL;
import java.util.Set;
import java.util.regex.Pattern;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

public class AllTestsBase
{

	public static <T> void buildSuite(TestSuite suite, String[] excludeTests, String packageName,
			Class<T> baseClass)
	{
		Pattern [] excludePatterns = new Pattern[excludeTests.length];
		for(int curExcludeNum = 0; curExcludeNum < excludeTests.length; curExcludeNum++)
		{
			excludePatterns[curExcludeNum] = Pattern.compile(excludeTests[curExcludeNum]);
		}
		Set<URL> packageURLs = ClasspathHelper.forPackage(packageName);
		Reflections reflections = new Reflections(new ConfigurationBuilder()
		.setUrls(packageURLs)
		.setScanners(new SubTypesScanner()));
		Set<Class<? extends T>> testCases = reflections.getSubTypesOf(baseClass);
		
		for (Class<? extends T> curClass:testCases)
		{
			boolean exclude = false;
			if (!curClass.getName().startsWith(packageName))
			{
				exclude = true;
			}
			else
			{
				String curClassName = curClass.getName();
				for (Pattern curExcludePattern:excludePatterns)
				{
					if (curExcludePattern.matcher(curClassName).matches())
					{
						exclude = true;
						break;
					}
				}
			}
			if (!exclude)
			{
				if (TestCase.class.isAssignableFrom(curClass))
				{
					suite.addTestSuite(curClass);
				}
			}
		}
	}

	public static <T> void buildSuiteIncludeOnly(TestSuite suite, String[] includeTests, String packageName,
			Class<T> baseClass)
	{
		Pattern [] includePatterns = new Pattern[includeTests.length];
		for(int curIncludeNum = 0; curIncludeNum < includeTests.length; curIncludeNum++)
		{
			includePatterns[curIncludeNum] = Pattern.compile(includeTests[curIncludeNum]);
		}
		Set<URL> packageURLs = ClasspathHelper.forPackage(packageName);
		Reflections reflections = new Reflections(new ConfigurationBuilder()
		.setUrls(packageURLs)
		.setScanners(new SubTypesScanner()));
		Set<Class<? extends T>> testCases = reflections.getSubTypesOf(baseClass);
		
		for (Class<? extends T> curClass:testCases)
		{
			boolean include = false;
			for (Pattern curIncludePattern:includePatterns)
			{
				if (curIncludePattern.matcher(curClass.getName()).matches())
				{
					include = true;
					break;
				}
			}
			
			if (include)
			{
				if (TestCase.class.isAssignableFrom(curClass))
				{
					suite.addTestSuite(curClass);
				}
			}
		}
	}
	
	public static <T> void buildSuite(TestSuite suite, String[] includeTests, String [] excludeTests, String packageName,
			Class<T> baseClass)
	{
		Pattern [] includePatterns = new Pattern[includeTests.length];
		for(int curIncludeNum = 0; curIncludeNum < includeTests.length; curIncludeNum++)
		{
			includePatterns[curIncludeNum] = Pattern.compile(includeTests[curIncludeNum]);
		}
		
		Pattern [] excludePatterns = new Pattern[excludeTests.length];
		for(int curExcludeNum = 0; curExcludeNum < excludeTests.length; curExcludeNum++)
		{
			excludePatterns[curExcludeNum] = Pattern.compile(excludeTests[curExcludeNum]);
		}
		Set<URL> packageURLs = ClasspathHelper.forPackage(packageName);
		Reflections reflections = new Reflections(new ConfigurationBuilder()
		.setUrls(packageURLs)
		.setScanners(new SubTypesScanner()));
		Set<Class<? extends T>> testCases = reflections.getSubTypesOf(baseClass);
		
		for (Class<? extends T> curClass:testCases)
		{
			boolean include = false;
			for (Pattern curIncludePattern:includePatterns)
			{
				if (curIncludePattern.matcher(curClass.getName()).matches())
				{
					include = true;
					break;
				}
			}
			if (include)
			{
				for (Pattern curExcludePattern:excludePatterns)
				{
					if (curExcludePattern.matcher(curClass.getName()).matches())
					{
						include = false;
						break;
					}
				}
				if (include)
				{
					if (TestCase.class.isAssignableFrom(curClass))
					{
						suite.addTestSuite(curClass);
					}
				}
			}
		}
	}
	public AllTestsBase()
	{
		// TODO Auto-generated constructor stub
	}

}
