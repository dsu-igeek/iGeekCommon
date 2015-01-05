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
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;

import org.apache.log4j.Logger;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import com.igeekinc.firehose.TestRemoteServer.TestCommand;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.logging.ErrorLogMessage;

public class TestRemoteClient extends FirehoseClient
{
	private SSLContext sslContext;
	public TestRemoteClient(InetSocketAddress address) throws IOException
	{
		FirehoseInitiator.initiateClient(address, this);
	}
	
	public TestRemoteClient(InetSocketAddress address, SSLContext sslContext) throws IOException
	{
		this.sslContext = sslContext;
		FirehoseInitiator.initiateClient(address, this, new SSLSetup()
		{
			
			@Override
			public boolean useSSL()
			{
				return true;
			}
			
			@Override
			public SSLContext getSSLContextForSocket(SocketChannel socket)
			{
				return TestRemoteClient.this.sslContext;
			}
		});

	}
	
	public TestRemoteClient(AFUNIXSocketAddress address) throws IOException
	{
		FirehoseInitiator.initiateClient(address, this);
	}
	
	public TestRemoteClient(AFUNIXSocketAddress address, SSLContext sslContext) throws IOException
	{
		this.sslContext = sslContext;
		FirehoseInitiator.initiateClient(address, this, new SSLSetup()
		{
			
			@Override
			public boolean useSSL()
			{
				return true;
			}
			
			@Override
			public SSLContext getSSLContextForSocket(SocketChannel socket)
			{
				return TestRemoteClient.this.sslContext;
			}
		});
	}
	
	public int add(int value1, int value2) throws IOException
	{
		ComboFutureBase<Integer>future = new ComboFutureBase<Integer>();
		addAsync(value1, value2, future, null);
		try
		{
			return future.get();
		} catch (Exception e)
		{
			Logger.getLogger(getClass()).warn("Got exception", e);
			throw new IOException("Could not execute");
		}
	}

	public <A> void addAsync(int value1, int value2,
			AsyncCompletion<Integer, A> future, A attachment) throws IOException
	{
		AddCommand addCommand = new AddCommand(value1, value2);
		sendMessage(addCommand, future, attachment);
	}

	public Future<Void> sleep(long timeToSleep) throws IOException
	{
		SleepCommand sleepCommand = new SleepCommand(timeToSleep);
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		sendMessage(sleepCommand, future, null);
		return future;
	}
	
	public void failWithIOError(int dummy) throws IOException
	{
		FailWithIOErrorCommand failCommand = new FailWithIOErrorCommand(dummy);
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		sendMessage(failCommand, future, null);
		try
		{
			future.get();
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (ExecutionException e)
		{
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
			throw new InternalError("Unexpected exception");
		}
	}
	public <A>void sleep(long timeToSleep, AsyncCompletion<Void, A>completionHandler, A attachment) throws IOException
	{
		SleepCommand sleepCommand = new SleepCommand(timeToSleep);
		ComboFutureBase<Void>future = new ComboFutureBase<Void>(completionHandler, attachment);
		sendMessage(sleepCommand, future, null);
	}

	public void bulkData(ByteBuffer bulkDataByteBuffer) throws IOException
	{		
		BulkDataCommand bulkDataCommand = new BulkDataCommand(bulkDataByteBuffer.remaining());
		ComboFutureBase<Void>future = new ComboFutureBase<Void>();
		sendMessage(bulkDataCommand, bulkDataByteBuffer, future, null);
		try
		{
			future.get();
		} catch (Exception e)
		{
			throw new IOException("Could not execute");
		}
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
		case kBulkDataCommand:
			return Void.class;
		default:
			throw new IllegalArgumentException();
		}
	}

}
