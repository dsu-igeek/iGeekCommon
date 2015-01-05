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
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.Future;

import org.apache.log4j.Level;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.perf4j.log4j.Log4JStopWatch;

import com.igeekinc.junitext.iGeekTestCase;
import com.igeekinc.util.async.AsyncCompletion;

public class RemoteServerTest extends iGeekTestCase
{
	TestRemoteServer server;
	
	@Override
	public Level getLoggingLevel()
	{
		return Level.INFO;
	}

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		server = new TestRemoteServer();
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		server.shutdown();
		super.tearDown();
	}
	
	public InetSocketAddress getConnectAddress() throws UnknownHostException
	{
		InetSocketAddress returnAddress = server.getListenAddresses(new AddressFilter()
		{
			
			@Override
			public boolean add(InetSocketAddress checkAddress)
			{
				return (!(checkAddress instanceof AFUNIXSocketAddress));
			}
		})[0];
		return returnAddress;
	}
	
	public void testBasic()
	throws Exception
	{
		TestRemoteClient client = new TestRemoteClient(getConnectAddress());
		assertEquals(3, client.add(1, 2));
		Future<Void>sleepFuture = client.sleep(10);
		sleepFuture.get();
		client.close();
	}
	
	public static final int kNumRepeatRuns = 10000;
	public void testRepeated() throws Exception
	{
		TestRemoteClient client = new TestRemoteClient(getConnectAddress());
		Log4JStopWatch stopWatch = new Log4JStopWatch("testRepeated");
		for (int curRunNum = 0; curRunNum < kNumRepeatRuns; curRunNum++)
		{
			assertEquals(curRunNum + (curRunNum * 2), client.add(curRunNum, curRunNum * 2));
		}
		stopWatch.stop();
		System.out.println(kNumRepeatRuns + " runs in "+stopWatch.getElapsedTime()+" ms "+
				((double)kNumRepeatRuns/(double)stopWatch.getElapsedTime())+" runs/ms");
		client.close();
	}
	
	public void testReconnect() throws Exception
	{
		TestRemoteClient client = new TestRemoteClient(getConnectAddress());
		assertEquals(3, client.add(1, 2));
		Future<Void>sleepFuture = client.sleep(10);
		sleepFuture.get();
		client.close();
		
		client = new TestRemoteClient(getConnectAddress());
		assertEquals(3, client.add(1, 2));
		sleepFuture = client.sleep(10);
		sleepFuture.get();
		client.close();
	}
	class SleepCompletionMonitor implements AsyncCompletion<Void, Integer>
	{
		private ArrayList<Integer>completionList = new ArrayList<Integer>();
		
		@Override
		public synchronized void completed(Void result, Integer attachment)
		{
			completionList.add(attachment);
			notifyAll();
		}

		@Override
		public void failed(Throwable exc, Integer attachment)
		{
			// TODO Auto-generated method stub
			
		}
		
		public synchronized Integer [] getCompletion()
		{
			return completionList.toArray(new Integer[completionList.size()]);
		}
		
		public synchronized void waitForCompletions(int numExpected) throws InterruptedException
		{
			while(completionList.size() < numExpected)
				wait();
		}
	}
	public void testOutOfOrderCompletion() throws Exception
	{
		SleepCompletionMonitor monitor = new SleepCompletionMonitor();
		TestRemoteClient client = new TestRemoteClient(getConnectAddress());
		client.sleep(10000, monitor, 2);
		client.sleep(5000, monitor, 1);
		client.sleep(1000, monitor, 0);
		client.sleep(15000, monitor, 3);
		monitor.waitForCompletions(4);
		Integer [] completionList = monitor.getCompletion();
		for (int completionNum = 0; completionNum < completionList.length; completionNum++)
			assertEquals(completionNum, (int)completionList[completionNum]);
		client.close();
	}
	
	public void testError() throws Exception
	{
		TestRemoteClient client = new TestRemoteClient(getConnectAddress());
		boolean caught = false;
		try
		{
			client.failWithIOError(42);
		}
		catch (IOException e)
		{
			caught = true;
		}
		assertTrue(caught);
	}
	
	public void testBulkData() throws Exception
	{
		TestRemoteClient client = new TestRemoteClient(getConnectAddress());
		byte [] bulkData = new byte[1024*1024];
		ByteBuffer bulkDataByteBuffer = ByteBuffer.wrap(bulkData);
		client.bulkData(bulkDataByteBuffer);
		for (int curCheckByteNum = 0; curCheckByteNum < bulkData.length; curCheckByteNum++)
		{
			assertEquals('A', bulkData[curCheckByteNum]);
		}
	}
	public static final int kPerfRuns = 10000;
	public void testBasicPerfAsync() throws Exception
	{
		TestRemoteClient client = new TestRemoteClient(getConnectAddress());
		AsyncCompletion<Integer, Void>addCompletion = new AsyncCompletion<Integer, Void>()
		{

			@Override
			public void completed(Integer result, Void attachment)
			{
				assertEquals(3, (int)result);
			}

			@Override
			public void failed(Throwable exc, Void attachment)
			{
				// TODO Auto-generated method stub
				
			}
		};
		
		Log4JStopWatch syncLoopWatch = new Log4JStopWatch("basicPerfTestSync");
		for (int runNum = 0; runNum < kPerfRuns; runNum++)
		{
			client.addAsync(1, 2, addCompletion, null);
		}
		syncLoopWatch.stop();
		System.out.println("Executed "+kPerfRuns+" in "+syncLoopWatch.getElapsedTime()+" ms, "+
				((double)kPerfRuns/((double)syncLoopWatch.getElapsedTime()/1000.0))+" calls/sec (async)");
		client.close();
	}
}
