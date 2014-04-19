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
	public CommandToProcess(FirehoseChannel channel, long commandSequence,
			CommandMessage commandToProcess)
	{
		super();
		this.channel = channel;
		this.commandSequence = commandSequence;
		this.commandToProcess = commandToProcess;
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
}
