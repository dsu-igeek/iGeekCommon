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

import com.igeekinc.util.datadescriptor.DataDescriptor;

/**
 * CommandResult is returned from the command processing code in the server to the Firehose framework.  It contains
 * all of the information to be returned to the client
 * @author David L. Smith-Uchida
 *
 */
public class CommandResult
{
	private int resultCode;
	private Object resultData;	// This should be an object containing all of the result data suitable for serialization via msgpack
	private DataDescriptor bulkData;
	private long bulkDataOffset, bulkDataLength;
	
	public CommandResult(int resultCode, Object resultData)
	{
		this(resultCode, resultData, null,0 ,0);
	}
	
	public CommandResult(int resultCode, Object resultData, DataDescriptor bulkData)
	{
		this(resultCode, resultData, bulkData, 0, bulkData.getLength());
	}
	
	public CommandResult(int resultCode, Object resultData, DataDescriptor bulkData, long bulkDataOffset, long bulkDataLength)
	{
		super();
		this.resultCode = resultCode;
		this.resultData = resultData;
		this.bulkData = bulkData;
		this.bulkDataOffset = bulkDataOffset;
		this.bulkDataLength = bulkDataLength;
	}

	public int getResultCode()
	{
		return resultCode;
	}

	public Object getResultData()
	{
		return resultData;
	}
	
	public DataDescriptor getBulkData()
	{
		return bulkData;
	}

	public long getBulkDataOffset()
	{
		return bulkDataOffset;
	}

	public long getBulkDataLength()
	{
		return bulkDataLength;
	}
}
