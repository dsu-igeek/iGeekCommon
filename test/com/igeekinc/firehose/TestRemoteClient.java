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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.newsclub.net.unix.AFUNIXSocketChannelImpl;

import com.igeekinc.firehose.TestRemoteServer.TestCommand;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.logging.ErrorLogMessage;

public class TestRemoteClient extends FirehoseClient
{
	public TestRemoteClient(InetSocketAddress address) throws IOException
	{
		socketChannel = SocketChannel.open(address);
		socket = socketChannel.socket();
		createResponseLoop();
	}
	
	public TestRemoteClient(InetSocketAddress address, SSLEngine sslEngine) throws IOException
	{
		socketChannel = SocketChannel.open(address);
		socket = socketChannel.socket();
		createResponseLoop(sslEngine);
	}
	
	public TestRemoteClient(AFUNIXSocketAddress address) throws IOException
	{
		socketChannel = AFUNIXSocketChannelImpl.open(address);
		socket = socketChannel.socket();
		createResponseLoop();
	}
	
	public TestRemoteClient(AFUNIXSocketAddress address, SSLEngine sslEngine) throws IOException
	{
		socketChannel = AFUNIXSocketChannelImpl.open(address);
		socket = socketChannel.socket();
		createResponseLoop(sslEngine);
	}
	
	public TestRemoteClient(SocketChannel channel) throws IOException
	{
		socketChannel = channel;
		socket = socketChannel.socket();
		createResponseLoop();
	}
	
	public TestRemoteClient(SocketChannel channel, SSLEngine sslEngine) throws IOException
	{
		socketChannel = channel;
		socket = socketChannel.socket();
		createResponseLoop(sslEngine);
	}
	
	public TestRemoteClient(File localSocketPath) throws IOException
	{
		//AFUNIXSocket socket = AFUNIXSocket.connectTo(addr)
	}
	public int add(int value1, int value2) throws IOException
	{
		AddCommand addCommand = new AddCommand(value1, value2);
		ComboFutureBase<Integer>future = new ComboFutureBase<Integer>();
		sendMessage(addCommand, future);
		try
		{
			return future.get();
		} catch (Exception e)
		{
			throw new IOException("Could not execute");
		}
	}

	public Future<Void> sleep(long timeToSleep) throws IOException
	{
		SleepCommand sleepCommand = new SleepCommand(timeToSleep);
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		sendMessage(sleepCommand, future);
		return future;
	}
	
	public void failWithIOError(int dummy) throws IOException
	{
		FailWithIOErrorCommand failCommand = new FailWithIOErrorCommand(dummy);
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		sendMessage(failCommand, future);
		try
		{
			future.get();
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			throw new InternalError("Unexpected exception");
		}
	}
	public <A>void sleep(long timeToSleep, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		SleepCommand sleepCommand = new SleepCommand(timeToSleep);
		ComboFutureBase<Void>future = new ComboFutureBase<Void>(completionHandler, attachment);
		sendMessage(sleepCommand, future);
	}

	@Override
	protected Class<? extends CommandMessage> getClassForCommandCode(
			int payloadType)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Class<? extends Object> getReturnClassForCommandCode(
			int payloadType)
	{
		switch(TestCommand.getCommandForNum(payloadType))
		{
		case kAddCommand:
			return Integer.class;
		case kSleepCommand:
			return Void.class;
		default:
			throw new IllegalArgumentException();
		}
	}

}
