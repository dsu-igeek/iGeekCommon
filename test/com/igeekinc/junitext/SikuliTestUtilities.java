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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

/**
 * 
 * Utility functions for generating JUnit test cases around Sikuli GUI tests.
 * 
 * To use, generate a new suite class (extends java.lang.Object) and define static Test suite().  suite() should call genSikuliTestSuite with
 * the appropriate Sikuli bundle path and file name (should be a .py file in the bundle, usually named the same as the bundle) and return the
 * generated Test object.  Needs to have jython (included in sikuli-script.jar) and all of the CV native libraries in the java.library.path to
 * work.
 * @author David L. Smith-Uchida
 *
 *iGeekCommon
 *
 * Copyright (C) 2009 iGeek, Inc.  All Rights Reserved
 */
public class SikuliTestUtilities
{
    private static String genSikuliTestClassName(String filename){
        String fname = new File(filename).getName();
        int dot = fname.indexOf(".");
        return fname.substring(0, dot);
     }


    public static Test genSikuliTestSuite(String filename, String bundlePath) throws IOException{
        String className = genSikuliTestClassName(filename);
        TestSuite ret = new TestSuite(className);
        PythonInterpreter interp = new PythonInterpreter();
        String testCode =
           "# coding=utf-8\n"+
           "from __future__ import with_statement\n"+
           "import junit\n"+
           "from junit.framework.Assert import *\n"+
           "from sikuli.Sikuli import *\n"+
           "class "+className+" (junit.framework.TestCase):\n"+
           "\tdef __init__(self, name):\n"+
           "\t\tjunit.framework.TestCase.__init__(self,name)\n"+
           "\t\tself.theTestFunction = getattr(self,name)\n"+
           "\t\tsetBundlePath('"+bundlePath+"')\n"+
           "\tdef runTest(self):\n"+
           "\t\tself.theTestFunction()\n";

        BufferedReader in = new BufferedReader(new FileReader(filename));
        String line;
        //int lineNo = 0;
        //Pattern patDef = Pattern.compile("def\\s+(\\w+)\\s*\\(");
        while( (line = in.readLine()) != null ){
          // lineNo++;
           testCode += "\t" + line + "\n";
           /*
  Matcher matcher = patDef.matcher(line);
  if(matcher.find()){
  String func = matcher.group(1);
  Debug.log("Parsed " + lineNo + ": " + func);
  _lineNoOfTest.put( func, lineNo );
  }
  */
        }
        interp.exec(testCode);
        PyList tests = (PyList)interp.eval(
              "["+className+"(f) for f in dir("+className+") if f.startswith(\"test\")]");
        while( tests.size() > 0 ){
           PyObject t = tests.pop();
           Test t2 = (Test)(t).__tojava__(TestCase.class);
           ret.addTest( t2 );
        }

        return ret;
     }
}
