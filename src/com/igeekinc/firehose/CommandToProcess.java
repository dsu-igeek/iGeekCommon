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

import java.util.Date;

/**
 * Contains info about a command to be processed by RemoteServer.  This is all rolled up in
 * a single class to make it easier to keep things together during asynchronous processing
 *
 */
public class CommandToProcess
{
	private FirehoseChannel channel;
	private long commandSequence;
	private CommandMessage commandToProcess;
	private long started, finished;
	public CommandToProcess(FirehoseChannel channel, long commandSequence,
			CommandMessage commandToProcess)
	{
		super();
		this.channel = channel;
		this.commandSequence = commandSequence;
		this.commandToProcess = commandToProcess;
		started = -1;
		finished = -1;
	}
	public FirehoseChannel getChannel()
	{
		return channel;
	}
	public long getCommandSequence()
	{
		return commandSequence;
	}
	public CommandMessage getCommandToProcess()
	{
		return commandToProcess;
	}
	
	public long getStarted()
	{
		return started;
	}
	public void setStarted(long started)
	{
		this.started = started;
	}
	public long getFinished()
	{
		return finished;
	}
	public void setFinished(long finished)
	{
		this.finished = finished;
	}
	public String toString()
	{
		StringBuffer returnBuffer = new StringBuffer();
		returnBuffer.append("Channel: ");
		returnBuffer.append(channel.toString());
		returnBuffer.append(" command sequence = ");
		returnBuffer.append(commandSequence);
		returnBuffer.append(" Command = ");
		returnBuffer.append(commandToProcess.toString());
		returnBuffer.append(" Started:");
		if (started < 0)
		{
			returnBuffer.append("<not started>");
		}
		else
		{
			Date startedDate = new Date(started);
			returnBuffer.append(startedDate.toString());
		}
		returnBuffer.append(" Finished:");
		if (finished < 0)
		{
			returnBuffer.append("<not finished>");
		}
		else
		{
			Date finishedDate = new Date(finished);
			returnBuffer.append(finishedDate.toString());
		}
		return returnBuffer.toString();
	}
	
	
}
