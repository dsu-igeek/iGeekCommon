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

import java.nio.ByteBuffer;

class ReadProgress
{
	private ByteBuffer headerByteBuffer, payloadByteBuffer, bulkDataByteBuffer;
	public enum RequestState
	{
		kReadingHeader,
		kReadingPayload,
		kReadingBulkData,
		kProcessing,
		kError;

		@Override
		public String toString()
		{
			switch(this)
			{
			case kError:
				return "Error";
			case kProcessing:
				return "Processing";
			case kReadingBulkData:
				return "ReadingBulkData";
			case kReadingHeader:
				return "ReadingHeader";
			case kReadingPayload:
				return "ReadingPayload";
			default:
				return "???";
			}
		}
		
		
	}
	
	private RequestState requestState = RequestState.kReadingHeader;
	private byte []	curHeader;
	private short	commandType;
	private int		commandCode;
	private long	commandSequence;
	private long	payloadSize;
	private long	bulkDataSize;
	
	public ReadProgress()
	{
		curHeader = new byte[FirehoseBase.kHeaderSize];
		headerByteBuffer = ByteBuffer.wrap(curHeader);
	}
	

	public void setRequestState(RequestState requestState)
	{
		this.requestState = requestState;
	}
	
	public RequestState getRequestState()
	{
		return requestState;
	}

	public ByteBuffer getHeaderByteBuffer()
	{
		return headerByteBuffer;
	}

	public ByteBuffer getPayloadByteBuffer()
	{
		return payloadByteBuffer;
	}

	public void setPayloadByteBuffer(ByteBuffer payloadByteBuffer)
	{
		this.payloadByteBuffer = payloadByteBuffer;
	}
	
	public ByteBuffer getBulkDataByteBuffer()
	{
		return bulkDataByteBuffer;
	}
	
	public void setBulkDataByteBuffer(ByteBuffer bulkDataByteBuffer)
	{
		this.bulkDataByteBuffer = bulkDataByteBuffer;
	}
	
	public short getCommandType()
	{
		return commandType;
	}

	public void setCommandType(short commandType)
	{
		this.commandType = commandType;
	}

	public int getCommandCode()
	{
		return commandCode;
	}
	
	public void setCommandCode(int commandCode)
	{
		this.commandCode = commandCode;
	}
	
	public long getCommandSequence()
	{
		return commandSequence;
	}
	
	public void setCommandSequence(long commandSequence)
	{
		this.commandSequence = commandSequence;
	}

	public long getPayloadSize()
	{
		return payloadSize;
	}

	public void setPayloadSize(long payloadSize)
	{
		this.payloadSize = payloadSize;
	}
	
	public long getBulkDataSize()
	{
		return bulkDataSize;
	}


	public void setBulkDataSize(long bulkDataSize)
	{
		this.bulkDataSize = bulkDataSize;
	}


	public void reset()
	{
		requestState = RequestState.kReadingHeader;
	}
	
	public String toString()
	{
		return "State: "+requestState+" "+"commandType = "+commandType;
	}
}