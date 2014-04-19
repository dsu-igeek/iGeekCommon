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
 
package com.igeekinc.firehose;

public class CommandResult
{
	private int resultCode;
	private Object resultData;	// This should be an object containing all of the result data suitable for serialization via msgpack
	
	public CommandResult(int resultCode, Object resultData)
	{
		super();
		this.resultCode = resultCode;
		this.resultData = resultData;
	}

	public int getResultCode()
	{
		return resultCode;
	}

	public Object getResultData()
	{
		return resultData;
	}
	
}
