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
 
package com.igeekinc.firehose;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

import javax.net.ssl.SSLContext;

import org.apache.log4j.Logger;

import com.igeekinc.util.logging.ErrorLogMessage;

public class TestRemoteServer extends FirehoseServer
{
	public enum TestCommand
	{
		kAddCommand(1),
		kSleepCommand(2),
		kFailWithIOErrorCommand(3);
		
		int commandNum;
		private TestCommand(int commandNum)
		{
			this.commandNum = commandNum;
		}
		
		public int getCommandNum()
		{
			return commandNum;
		}
		
		public static TestCommand getCommandForNum(int num)
		{
			switch(num)
			{
			case 1:
				return kAddCommand;
			case 2:
				return kSleepCommand;
			case 3:
				return kFailWithIOErrorCommand;
			}
			throw new IllegalArgumentException();
		}
	}
	public TestRemoteServer() throws IOException
	{
		this(null);
	}
	
	public TestRemoteServer(SSLContext sslContext) throws IOException
	{
		this(ServerSocketChannel.open(), sslContext);
	}
	
	public TestRemoteServer(ServerSocketChannel channel, SSLContext sslContext) throws IOException
	{
		this.sslContext = sslContext;
		serverSocketChannel = channel;
		serverSocket = serverSocketChannel.socket();
		if (!serverSocket.isBound())
			serverSocket.bind(new InetSocketAddress(0));
		createSelectLoop();
	}

	@Override
	protected void processCommand(CommandToProcess commandToProcess)
	{
		switch(TestCommand.getCommandForNum(commandToProcess.getCommandToProcess().getCommandCode()))
		{
		case kAddCommand:
		{
			AddCommand addCommand = (AddCommand)commandToProcess.getCommandToProcess();
			Integer addResult = addCommand.value1 + addCommand.value2;
			CommandResult result = new CommandResult(0, addResult);
			commandCompleted(commandToProcess, result);
			break;
		}

		case kSleepCommand:
		{
			SleepCommand sleepCommand = (SleepCommand)commandToProcess.getCommandToProcess();
			try
			{
				logger.warn("Sleep command sleeping for "+sleepCommand.getTimeToSleep()+" ms");
				Thread.sleep(sleepCommand.getTimeToSleep());
				logger.warn("Finished sleeping for "+sleepCommand.getTimeToSleep()+" ms");
				
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
			CommandResult result = new CommandResult(0, null);
			commandCompleted(commandToProcess, result);
			break;
		}
		case kFailWithIOErrorCommand:
		{
			commandFailed(commandToProcess, new IOException());
		}
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	protected Class<? extends CommandMessage> getClassForCommandCode(
			int commandCode)
	{
		switch(TestCommand.getCommandForNum(commandCode))
		{
		case kAddCommand:
			return AddCommand.class;
		case kSleepCommand:
			return SleepCommand.class;
		case kFailWithIOErrorCommand:
			return FailWithIOErrorCommand.class;
		default:
			throw new IllegalArgumentException();
		}
		
	}

	@Override
	public Thread createSelectLoopThread(Runnable selectLoopRunnable)
	{
		return new Thread(selectLoopRunnable, "TestRemoteServerSelect");
	}

	@Override
	protected Class<? extends CommandMessage> getReturnClassForCommandCode(
			int payloadType)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
