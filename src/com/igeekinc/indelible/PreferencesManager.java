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
 
package com.igeekinc.indelible;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.igeekinc.util.CheckCorrectDispatchThread;
import com.igeekinc.util.MonitoredProperties;
import com.igeekinc.util.logging.ErrorLogMessage;

public abstract class PreferencesManager
{
    public static final String	kPreferencesDirPropertyName	= "com.igeekinc.indelible.preferencesDir";
	protected MonitoredProperties properties;
    protected static PreferencesManager singleton;
    protected ArrayList<File>additionalFiles = new ArrayList<File>();
    public static MonitoredProperties getProperties()
    {
    	if (singleton != null)
    		return singleton.getPropertiesInternal();
    	return null;
    }
    
    public PreferencesManager(CheckCorrectDispatchThread dispatcher) throws IOException
    {
    	if (singleton != null)
    	{
    		Logger.getLogger(getClass()).error(new ErrorLogMessage("Attempting to set second PreferencesManager, ignored"));
    		return;
    	}
    	singleton = this;
    	initPreferencesInternal(dispatcher);
    }
    
	public static File getPreferencesFile()
	{
		return singleton.getPreferencesFileInternal();
	}

	public static File getPreferencesDir()
	{
		return singleton.getPreferencesDirInternal();
	}
	
	public static void addPreferencesFile(File preferencesFile) throws IOException
	{
		singleton.addPreferencesFileInternal(preferencesFile);
	}

	protected void addPreferencesFileInternal(File preferencesFile) throws IOException
	{
		if (!additionalFiles.contains(preferencesFile))
			additionalFiles.add(preferencesFile);
		reloadPropertiesInternal();
	}
	protected MonitoredProperties getPropertiesInternal()
	{
	    if (properties == null)
	        throw new InternalError("IndelibleServerProperties not initialized"); //$NON-NLS-1$
	    return properties;
	}

	public static void setIfNotSet(String propertyName, String defaultValue)
	{
		singleton.setIfNotSetInternal(propertyName, defaultValue);
	}
	
	protected void setIfNotSetInternal(String propertyName, String defaultValue)
	{
	    if (properties.get(propertyName) == null)
	    {
	        properties.put(propertyName, defaultValue);
	    }
	}
	
	protected abstract File getPreferencesFileInternal();
	protected abstract File getPreferencesDirInternal();
	protected abstract void initPreferencesInternal(CheckCorrectDispatchThread dispatcher) throws IOException;

	public static void storeProperties() throws IOException
	{
		singleton.storePropertiesInternal();
	}
	
	public void storePropertiesInternal() throws IOException
	{
		if (additionalFiles.size() > 0)
			throw new IllegalArgumentException("Cannot store properties when additional property files have been added");
	    File preferencesDir = getPreferencesDir();
	    if (!preferencesDir.exists())
	        preferencesDir.mkdirs();
	    File propertiesFile = getPreferencesFile(); //$NON-NLS-1$
	    FileOutputStream propertiesStream = new FileOutputStream(propertiesFile);
	    properties.store(propertiesStream, propertiesFile.getName()); //$NON-NLS-1$
	    propertiesStream.close();
	}

	
	public static void reloadProperties() throws IOException
	{
		singleton.reloadPropertiesInternal();
	}
	
	public void reloadPropertiesInternal() throws IOException
	{
	    File preferencesDir = getPreferencesDir();
	    File propertiesFile = getPreferencesFile(); //$NON-NLS-1$
	    Properties newProperties = new Properties();
	    FileInputStream propertiesIS = new FileInputStream(propertiesFile);
	    newProperties.load(propertiesIS);
	    propertiesIS.close();
	    for (File additionalFile:additionalFiles)
	    {
	    	if (additionalFile.exists())
	    	{
	    		Properties addProperties = new Properties();
	    		FileInputStream additonalIS = new FileInputStream(additionalFile);
	    		addProperties.load(additonalIS);
	    		for (Entry<Object, Object> curEntry:addProperties.entrySet())
	    		{
	    			newProperties.setProperty((String)curEntry.getKey(), (String)curEntry.getValue());
	    		}
	    	}
	    }
	    properties.replaceProperties(newProperties);
	}
}
