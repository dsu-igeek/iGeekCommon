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

import java.util.Enumeration;

import junit.runner.ClassPathTestCollector;

public class PackageTestCollector extends ClassPathTestCollector
{
    String packageName;
    static Class junitTest;
    /**
     * Finds test classes for the named package and subpackages.  PackageName is in path
     * form (e.g. /com/igeekinc/util)
     * @param packageName
     */
    public PackageTestCollector(String packageName)
    {
        this.packageName = packageName;
        try
        {
            junitTest = Class.forName("junit.framework.Test");
        } catch (ClassNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new InternalError("Couldn't find junit.framework.Test class");
        }
    }
    protected boolean isTestClass(String classFileName)
    {
        boolean returnVal = false;
        if (classFileName.startsWith(packageName) && classFileName.endsWith(".class"))
        {
            String className;
            className = classFileName.replace('/', '.');
            if (className.charAt(0) == '.')
                className = className.substring(1);
            // whack off the ".class" at the end
            className = className.substring(0, className.length()-6);
            Class checkClass;
            try
            {
                checkClass = Class.forName(className);
                if (junitTest.isAssignableFrom(checkClass))
                    returnVal = true;
            } catch (ClassNotFoundException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return returnVal;
    }
    
    public static void main(String [] argv)
    {
        PackageTestCollector test = new PackageTestCollector("/com/igeekinc/util");
        Enumeration tests = test.collectTests();
        while (tests.hasMoreElements())
        {
            System.out.println(tests.nextElement());
        }
    }
}
