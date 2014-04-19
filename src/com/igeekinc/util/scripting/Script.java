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
 
package com.igeekinc.util.scripting;

import java.io.Serializable;
import java.util.Hashtable;

import com.igeekinc.util.ClientFile;

public class Script implements Serializable
{
    private static final long serialVersionUID = 2274604003736006362L;
    protected String name;
    protected ClientFile scriptFile;
    protected long maxExecutionTime;
    protected boolean abortOnError;
    protected Hashtable<String, Serializable> properties;
    public static final String kArgumentsPropertyName = "arguments";
    public Script(String inName, ClientFile inScriptFile)
    {
        name = inName;
        scriptFile = inScriptFile;
        maxExecutionTime = 0;
        properties = new Hashtable<String, Serializable>();
    }

    public String getName()
    {
        return name;
    }
    
    public ClientFile getScriptFile()
    {
        return scriptFile;
    }
    
    public void setMaxExecutionTime(long newMax)
    {
        maxExecutionTime = newMax;
    }
    
    public long getMaxExecutionTime()
    {
        return maxExecutionTime;
    }

    /**
     * Indicates that execution of scripts should continue in the
     * event that this script fails (return code != 0)
     * @return
     */
    public boolean isAbortOnError()
    {
        return abortOnError;
    }

    public void setAbortOnError(boolean continueOnError)
    {
        this.abortOnError = continueOnError;
    }
    
    public Serializable setProperty(String propertyName, Serializable value)
    {
        return (Serializable)properties.put(propertyName, value);
    }
    
    public Serializable getProperty(String propertyName)
    {
        return (Serializable)properties.get(propertyName);
    }
}
