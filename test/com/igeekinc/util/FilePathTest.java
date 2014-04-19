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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.igeekinc.junitext.iGeekTestCase;

public class FilePathTest extends iGeekTestCase 
{
	FilePath getAbsolutePath(String... components) throws IOException
	{
		return getRootPath().getChild(FilePath.getFilePath(makePathString(components)));
	}
	String makePathString(String... components)
	{
		if (components == null || components.length == 0)
			return "";
		StringBuffer pathBuffer = new StringBuffer();
		for (int curComponentNum = 0; curComponentNum < components.length - 1; curComponentNum++)
		{
			String curComponent = components[curComponentNum];
			pathBuffer.append(curComponent);
			pathBuffer.append(File.separatorChar);
		}
		pathBuffer.append(components[components.length - 1]);
		return pathBuffer.toString(); 
	}

    public void testAlpha()
	throws Exception
	{
		File testFile = File.createTempFile("filepathtest", "test");
		FilePath testPath = FilePath.getFilePath(testFile);
		logger.warn("testFile path = "+testFile.getAbsolutePath());
		logger.warn("testPath path = "+testPath.toString());
		assertEquals(testFile.getAbsolutePath(), testPath.getPath());
		assertEquals(testFile.isAbsolute(), testPath.isAbsolute());
        testFile.delete();
	}
	
	public void testBeta()
	throws Exception
	{
		FilePath testPath = FilePath.getFilePath(File.listRoots()[0].getAbsolutePath());
		for (int i=0; i < 100; i++)
		{
			testPath = testPath.getChild(FilePath.getFilePath(Integer.toString(i)));
		}
		logger.warn("long path test " + testPath);
		
		for (int i=0; i<100; i++)
		{
			assertEquals(Integer.toString(i), testPath.getComponent(i+1));
		}
		FilePath curPath = testPath;
		for (int i=99; i >=0; i--)
		{
			assertEquals(Integer.toString(i), curPath.getName());
			assertTrue(testPath.startsWith(curPath));
			curPath = curPath.getParent();
		}
	}
	
	public void testGamma()
	throws Exception
	{
		FilePath testPath = createTempFilePath();
		String [] components = testPath.getComponents();
		for (int i=0; i<components.length; i++)
		{
			assertEquals(components[i], testPath.getComponent(i));
		}
        
	}
	
	public void testDelta()
	throws Exception
	{
		//FilePath testPath = createTempFilePath();
	}

	private FilePath createTempFilePath() throws IOException {
		File testFile = File.createTempFile("filepathtest", "test");
		FilePath testPath = FilePath.getFilePath(testFile);
        testFile.delete();
		return testPath;
	}
	
	/*
	 * Class under test for FilePath getFilePath(String)
	 */
	public void testGetFilePathString() 
	throws Exception
	{
		File testFile = File.createTempFile("filepathtest", "test");
		FilePath testPath = FilePath.getFilePath(testFile.getAbsolutePath());
		assertEquals(testFile.getAbsolutePath(), testPath.toString());
        testFile.delete();
	}

	/*
	 * Class under test for FilePath getFilePath(String, boolean)
	 */
	public void testGetFilePathStringboolean() 
	throws Exception
	{
		// Normalization strips out ".." and associated dirs from the path
		// e.g. a/b/../c -> a/c
		String testPathString = makePathString("a","b","..","c");
		String normalizedPathString = makePathString("a","c");
		
		checkNormalized(testPathString, normalizedPathString);
		
		testPathString = makePathString("a","b","..","c","d","e",
			"f","..","..");
		normalizedPathString = makePathString("a","c","d");
		
		checkNormalized(testPathString, normalizedPathString);
	}

	private void checkNormalized(String testPathString, String normalizedPathString) {
		FilePath testPath = FilePath.getFilePath(testPathString, true);
		assertFalse(testPathString.equals(testPath.toString()));
		assertEquals(normalizedPathString, testPath.toString());
		
		testPath = FilePath.getFilePath(testPathString, false);
		assertFalse(normalizedPathString.equals(testPath.toString()));
		assertEquals(testPathString, testPath.toString());
	}

	/*
	 * Class under test for FilePath getFilePath(FileLike)
	 */
	public void testGetFilePathFileLike() 
	throws Exception
	{
		File testFile = File.createTempFile("filepathtest", "test");
		ClientFile testClientFile = SystemInfo.getSystemInfo().getClientFileForPath(testFile.getAbsolutePath());
		FilePath testPath = FilePath.getFilePath(testClientFile);
		assertEquals(testFile.getAbsolutePath(), testPath.toString());
        testFile.delete();
	}

	/*
	 * Class under test for FilePath getFilePath(File)
	 */
	public void testGetFilePathFile() 
	throws Exception
	{
		File testFile = File.createTempFile("filepathtest", "test");
		FilePath testPath = FilePath.getFilePath(testFile);
		assertEquals(testFile.getAbsolutePath(), testPath.toString());
        testFile.delete();
	}

    public void testGetFilePathComponents() 
    throws Exception
    {
        File testFile = File.createTempFile("filepathtest", "test");
        String testFilePath = testFile.getAbsolutePath();
        ArrayList<String> componentsArray = new ArrayList<String>();
        if (testFilePath.startsWith(File.separator))
            componentsArray.add("");// Add a blank for file paths that don't have a root designator at the beginning
        StringTokenizer tokenizer = new StringTokenizer(testFilePath, File.separator);

        while (tokenizer.hasMoreElements())
        {
            componentsArray.add(tokenizer.nextToken());
        }
        String [] components = new String[componentsArray.size()];
        components = componentsArray.toArray(components);
        FilePath testPath = FilePath.getFilePath(components, true);
        assertEquals(testFile.getAbsolutePath(), testPath.toString());
        testFile.delete();
    }
	/*
	 * Class under test for FilePath getFilePath(ClientFile)
	 */
	public void testGetFilePathClientFile() 
	throws Exception
	{
		File testFile = File.createTempFile("filepathtest", "test");
		ClientFile testClientFile = SystemInfo.getSystemInfo().getClientFileForPath(testFile.getAbsolutePath());
		FilePath testPath = FilePath.getFilePath(testClientFile);
		assertEquals(testFile.getAbsolutePath(), testPath.toString());
        testFile.delete();
	}

	public void testStartsWith() throws IOException 
	{
		FilePath rootPath = getRootPath();

		FilePath testPath = FilePath.getFilePath(makePathString("mytest","test1","abcd","efgh","aznm"));
		FilePath startPath = FilePath.getFilePath("mytest");
		assertTrue(testPath.startsWith(startPath));
		startPath = FilePath.getFilePath(makePathString("mytest","test1","abcd"));
		assertTrue(testPath.startsWith(startPath));
		startPath = FilePath.getFilePath(makePathString("mytest","test2"));
		assertFalse(testPath.startsWith(startPath));
        FilePath startPath2 = rootPath.getChild(FilePath.getFilePath(makePathString("mytest","test1","abcd","efgh","aznm")));
        assertFalse(testPath.startsWith(startPath2));
	}

	public FilePath getRootPath() throws IOException {
		FilePath rootPath = createTempFilePath();
		rootPath = rootPath.removeTrailingComponents(rootPath.getNumComponents() - 1);
		return rootPath;
	}

	public void testGetPathRelativeTo() 
	{
		FilePath testPath = FilePath.getFilePath(makePathString("mytest","test1","abcd","efgh","aznm"));
		FilePath startPath = FilePath.getFilePath("mytest");
		FilePath relPath = testPath.getPathRelativeTo(startPath);
		assertEquals(relPath.getComponent(0), "test1");
		
		startPath = FilePath.getFilePath(makePathString("mytest","test2"));
		relPath = testPath.getPathRelativeTo(startPath);
		assertEquals(relPath.getComponent(0), "mytest");
		assertFalse(relPath.isAbsolute());
	}

	/*
	 * Class under test for void init(String, String, boolean, boolean)
	 */
	public void testInitStringStringbooleanboolean() {
	}

	/*
	 * Class under test for void init(String[], int, int, boolean)
	 */
	public void testInitStringArrayintintboolean() {
	}

	public void testGetParent() throws IOException 
	{
		FilePath testPath = FilePath.getFilePath(makePathString("mytest","test1","abcd","efgh","aznm"));
		FilePath parent = testPath.getParent();
		assertEquals("efgh", parent.getName());
		boolean internalErrorCaught = false;
		try
		{
			assertFalse(parent.isAbsolute());
			while(true)
				parent = parent.getParent();
		}
		catch (InternalError e)
		{
			internalErrorCaught = true;
		}
		
		assertTrue(internalErrorCaught);
		
		File testFile = File.createTempFile("filepathtest", "test");
		ClientFile testClientFile = SystemInfo.getSystemInfo().getClientFileForPath(testFile.getAbsolutePath());
		testPath = FilePath.getFilePath(testClientFile);
		parent = testPath;
		assertTrue(parent.isAbsolute());
		testFile.delete();

		while(parent.getNumComponents() > 1)
			parent = parent.getParent();
		FilePath newParent = parent.getParent();
		assertEquals(parent, newParent);
	}

	public void testGetChild() throws IOException 
	{
		String pathName = makePathString("mytest","test1","abcd","efgh","aznm");
		FilePath testPath = FilePath.getFilePath(pathName);
		FilePath childPath = testPath.getChild("mychild");
		assertEquals("mychild", childPath.getName());
		assertEquals(testPath, childPath.getParent());
		assertFalse(childPath.isAbsolute());
        boolean exceptionThrown = false;
        try
        {
            testPath.getChild(getAbsolutePath("xyzzy").toString());
        }
        catch(IllegalArgumentException e)
        {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
	}

	public void testGetNewFilePath() 
	{
	}

	public void testGetNumComponents() throws IOException 
	{
		String pathName = makePathString("mytest","test1","abcd","efgh","aznm");
		FilePath testPath = FilePath.getFilePath(pathName);
		assertEquals(5, testPath.getNumComponents());
		String upPathName = makePathString("mytest","..","test1","..","abcd","efgh","aznm");
		FilePath upPathNormalized = FilePath.getFilePath(upPathName, true);
		assertEquals(3, upPathNormalized.getNumComponents());
		FilePath upPathNotNormalized = FilePath.getFilePath(upPathName, false);
		assertEquals(7, upPathNotNormalized.getNumComponents());
		
		// Kind of hard to figure out how many components there are in a system-independent
		// way.  So, let's just retrieve all of them and make sure that it works
		File testFile = File.createTempFile("filepathtest", "test");
		ClientFile testClientFile = SystemInfo.getSystemInfo().getClientFileForPath(testFile.getAbsolutePath());
		testPath = FilePath.getFilePath(testClientFile);
		int numComponents = testPath.getNumComponents();
		for (int i = 0; i < numComponents; i++)
			testPath.getComponent(i);
		// Then, get 1 past the end to make sure we get an error
		boolean exceptionCaught = false;
		try
		{
			testPath.getComponent(numComponents);
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			exceptionCaught = true;
		}
		assertTrue(exceptionCaught);
        testFile.delete();
	}

	public void testGetComponent() 
	{
		String pathName = makePathString("mytest","test1","abcd","efgh","aznm");
		FilePath testPath = FilePath.getFilePath(pathName);
		assertEquals("mytest", testPath.getComponent(0));
		assertEquals("aznm", testPath.getComponent(4));
		
		boolean exceptionCaught = false;
		try
		{
			testPath.getComponent(5);
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			exceptionCaught = true;
		}
		assertTrue(exceptionCaught);
		exceptionCaught = false;
		try
		{
			testPath.getComponent(-1);
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			exceptionCaught = true;
		}
	}

	public void testGetName() 
	{
		String pathName = makePathString("mytest","test1","abcd","efgh","aznm");
		FilePath testPath = FilePath.getFilePath(pathName);
		assertEquals("aznm", testPath.getName());
		
	}

	public void testGetComponents() 
	{
		String pathName = makePathString("mytest","test1","abcd","efgh","aznm");
		FilePath testPath = FilePath.getFilePath(pathName);
		String [] components = testPath.getComponents();
		assertEquals(5, components.length);
		assertEquals("mytest", components[0]);
		assertEquals("abcd", components[2]);
		assertEquals("aznm", components[4]);
	}

	/*
	 * Class under test for boolean equals(Object)
	 */
	public void testEqualsObject() throws IOException 
    {
        FilePath testPath1 = getAbsolutePath("tmp","xyzzy","abc");
        
        assertTrue(testPath1.equals(testPath1));
        FilePath testPath2 = getAbsolutePath("tmp","xyzzy","abc");
        FilePath testPath3 = getAbsolutePath("tmp","..","tmp","xyzzy","abc");
        
        assertEquals(testPath1, testPath2);
        assertEquals(testPath1, testPath3);
        
        FilePath testPath4 = FilePath.getFilePath(makePathString("tmp","xyzzy","abc"));
        assertNotSame(testPath1, testPath4);
        
        FilePath testPath5 = getAbsolutePath("tmp","xyzzy","def");
        assertFalse(testPath1.equals(testPath5));
        
        assertFalse(testPath1.equals("/tmp/xyzzy/def"));
	}

	public void testGetPath() {
	}

	/*
	 * Class under test for String toString()
	 */
	public void testToString() {
	}

	public void testToStringBuf() {
	}

	public void testIsAbsolute() throws IOException {
        FilePath testPath1 = getAbsolutePath("tmp","xyzzy","abc");
        assertTrue(testPath1.isAbsolute());
        FilePath testPath2 = FilePath.getFilePath(makePathString("tmp","xyzzy","abc"));
        assertFalse(testPath2.isAbsolute());
	}

	public void testObject() {
	}

	public void testGetClass() {
	}

	public void testHashCode() 
    {
        FilePath testPath = FilePath.getFilePath(SystemInfo.getSystemInfo().getSeparator());
        int firstHash = testPath.hashCode();
        FilePath origPaths[] = new FilePath[100];
        for (int i=0; i < 100; i++)
        {
            testPath = testPath.getChild(FilePath.getFilePath(Integer.toString(i)));
            origPaths[i] = testPath;
        }
        
        testPath = FilePath.getFilePath(SystemInfo.getSystemInfo().getSeparator());
        assertEquals(firstHash, testPath.hashCode());
        for (int i=0; i < 100; i++)
        {
            testPath = testPath.getChild(FilePath.getFilePath(Integer.toString(i)));
            assertEquals(testPath.hashCode(), origPaths[i].hashCode());
            if (i > 0)
                assertFalse(origPaths[i-1].hashCode() == origPaths[i].hashCode());
        }
	}

	/*
	 * Class under test for boolean equals(java.lang.Object)
	 */
	public void testEqualsObject1() {
	}

	public void testClone() {
	}

	/*
	 * Class under test for java.lang.String toString()
	 */
	public void testToString1() {
	}

	public void testNotify() {
	}

	public void testNotifyAll() {
	}

	/*
	 * Class under test for void wait(long)
	 */
	public void testWaitlong() {
	}

	/*
	 * Class under test for void wait(long, int)
	 */
	public void testWaitlongint() {
	}

	/*
	 * Class under test for void wait()
	 */
	public void testWait() {
	}

	public void testFinalize() {
	}
    
    public void testRemoveLeadingComponent() throws IOException
    {
        FilePath testPath = getAbsolutePath("tmp","xyzzy","abc");
        assertTrue(testPath.isAbsolute());
        int numComponents = testPath.getNumComponents();
        assertEquals(4, numComponents);
        testPath = testPath.removeLeadingComponent();
        assertEquals(3, testPath.getNumComponents());
        testPath = testPath.removeLeadingComponent();
        assertEquals(2, testPath.getNumComponents());
        assertEquals(testPath.getComponent(0), "xyzzy");
        assertFalse(testPath.isAbsolute());
        testPath = testPath.removeLeadingComponent();
        assertEquals(testPath.getComponent(0), "abc");
        assertEquals(1, testPath.getNumComponents());
        testPath = testPath.removeLeadingComponent();
        assertEquals(0, testPath.getNumComponents());
    }
    
    public void testRemoveLeadingComponents() throws IOException
    {
        FilePath testPath = getAbsolutePath("tmp","xyzzy","abc", "123");
        assertTrue(testPath.isAbsolute());
        testPath = testPath.removeLeadingComponents(3);
        assertEquals(testPath.getComponent(0), "abc");
        assertEquals(2, testPath.getNumComponents());
        assertFalse(testPath.isAbsolute());
        boolean exceptionThrown = false;
        try
        {
            testPath.removeLeadingComponents(4);
        }
        catch (IllegalArgumentException e)
        {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
    }
    
    public void testGetSuffix() throws IOException
    {
        FilePath testFilePath1 = getAbsolutePath("tmp","xyzzy","abc.doc");
        assertEquals("doc", testFilePath1.getSuffix());
        
        FilePath testFilePath2 = getAbsolutePath("tmp","xyzzy.abc","def");
        assertEquals("", testFilePath2.getSuffix());
        
        FilePath testFilePath3 = getAbsolutePath("tmp","xyzzy","abc.new.one.doc");
        assertEquals("doc", testFilePath3.getSuffix());
    }
    

    public void testAddSuffix() throws IOException
    {
        FilePath testFilePath1 = getAbsolutePath("tmp","xyzzy","abc");
        FilePath testFilePath2 = testFilePath1.addSuffix(".doc");
        assertEquals("abc.doc", testFilePath2.getName());
        
        FilePath subPath = testFilePath1.removeLeadingComponent();
        assertEquals("tmp", subPath.getComponent(0));
        
        FilePath suffixPath = subPath.addSuffix(".ext");
        assertEquals("tmp", suffixPath.getComponent(0));
        assertEquals("abc.ext", suffixPath.getName());
        
        suffixPath = suffixPath.removeTrailingComponent().addSuffix(".new");
        assertEquals("tmp", suffixPath.getComponent(0));
        assertEquals("xyzzy.new", suffixPath.getName());
    }
    
    public void testAddExtension() throws IOException
    {
        FilePath testFilePath1 = getAbsolutePath("tmp","xyzzy","abc");
        FilePath testFilePath2 = testFilePath1.addExtension("txt");
        assertEquals("abc.txt", testFilePath2.getName());
    }
    public void testRemoveExtension() throws IOException
    {
        FilePath testFilePath1 = getAbsolutePath("tmp","xyzzy","abc.doc");
        FilePath testFilePath2 = testFilePath1.removeExtension();
        assertEquals("abc", testFilePath2.getName());
        // Now make sure it works when there is no extension
        FilePath testFilePath3 = testFilePath2.removeExtension();
        assertEquals("abc", testFilePath3.getName());
    }
    
    public void testRemoveSuffix() throws IOException
    {
        FilePath testFilePath1 = getAbsolutePath("tmp","xyzzy","abc.doc");
        FilePath testFilePath2 = testFilePath1.removeSuffix(1);
        assertEquals("abc.do", testFilePath2.getName());
    }
    
    public void testAddRootToPath()
    throws Exception
    {
    	FilePath rootPath = getRootPath();
        FilePath testFilePath1 = getAbsolutePath("tmp","xyzzy","abc.doc");
        FilePath checkPath = rootPath.getChild(testFilePath1);
        assertTrue(checkPath.equals(testFilePath1));
        
    }
}
