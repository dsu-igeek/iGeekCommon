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
import java.util.Date;

import com.igeekinc.util.async.AsyncCompletion;

/**
 * CommandBlock is used by FirehoseClient to keep track of an outstanding command that has been issued to the server
 * @author David L. Smith-Uchida
 *
 */
public class CommandBlock <A>
{
	private long commandSequence;									// Sequence number
	private CommandMessage message;									// The command that was sent
	private ByteBuffer bulkDataDestination;							// ByteBuffer to deliver bulk data to

	private AsyncCompletion<? extends Object, A>completionHandler;	// Future to notify when the command completes
	private A attachment;											// Attachment to send with the completion
	private long createdAt = System.currentTimeMillis();
	
	/**
	 * 
	 * @param commandSequence - Sequence number
	 * @param message - The command that was sent
	 * @param bulkDataDestination - ByteBuffer to deliver bulk data to (may be null)
	 * @param completionHandler - Future to notify when the command completes
	 * @param attachment - // Attachment to send with the notification
	 */
	public CommandBlock(long commandSequence, CommandMessage message, ByteBuffer bulkDataDestination, AsyncCompletion<? extends Object, A>completionHandler,
			A attachment)
	{
		this.commandSequence = commandSequence;
		this.message = message;
		this.completionHandler = completionHandler;
		this.attachment = attachment;
		this.bulkDataDestination = bulkDataDestination;
	}
	
	/**
	 * This is for msgpack - don't call this!
	 */
	public CommandBlock()
	{
		
	}
	public long getCommandSequence()
	{
		return commandSequence;
	}
	public CommandMessage getMessage()
	{
		return message;
	}

	public AsyncCompletion<? extends Object, A> getFuture()
	{
		return completionHandler;
	}
	
	public A getAttachment()
	{
		return attachment;
	}

	public ByteBuffer getBulkData()
	{
		return bulkDataDestination;
	}
	
	public String toString()
	{
		StringBuffer returnBuffer = new StringBuffer();
		returnBuffer.append("CommandBlock: ");
		returnBuffer.append(message.toString());
		returnBuffer.append(" ");
		long elapsed = System.currentTimeMillis() - createdAt;
		returnBuffer.append("Created at "+new Date(createdAt)+" "+elapsed+" ms ago");
		return returnBuffer.toString();
	}
}
