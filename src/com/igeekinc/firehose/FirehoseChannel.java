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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;
import org.newsclub.net.unix.AFUNIXSelectorProvider;
import org.newsclub.net.unix.AFUNIXSocketChannelImpl;

/**
 * FirehoseChannel provides a subset of java.nio.Channel operations suitable for Firehose operations.  FirehoseChannel
 * exists so that SSLFirehoseChannel can be implemented.
 * 
 * Currently, reads are non-blocking while writes will block
 * @author David L. Smith-Uchida
 *
 */
public class FirehoseChannel
{
	private int channelNum;
	private SocketChannel socketChannel;
	private ByteBuffer inputBuffer;
	private ReadProgress readProgress;
	protected Logger logger = Logger.getLogger(FirehoseChannel.class);
	public FirehoseChannel(int channelNum, SocketChannel socketChannel) throws IOException
	{
		this.channelNum = channelNum;
		this.socketChannel = socketChannel;
		inputBuffer = ByteBuffer.allocate(1024 * 1024);
		inputBuffer.limit(0);	// Set limit to zero so we'll read something
	}
	
	public void configureBlocking(boolean block) throws IOException
	{
		socketChannel.configureBlocking(block);
	}
	
	public Selector openSelector() throws IOException
	{
		if (socketChannel instanceof AFUNIXSocketChannelImpl)
			return AFUNIXSelectorProvider.provider().openSelector();
		else
			return Selector.open();
	}

	public void register(Selector selector, int ops) throws ClosedChannelException
	{
		socketChannel.register(selector, ops);
	}
	
	/**
	 * Will either read more data from the socket or just return if data is available in the input buffer
	 * @return
	 * @throws IOException 
	 */
	public synchronized int readNoBlock() throws IOException
	{
		if (!inputBuffer.hasRemaining())
		{
			inputBuffer.position(0);
			inputBuffer.limit(inputBuffer.capacity());
			int bytesRead = socketChannel.read(inputBuffer);
			if (bytesRead < 0)
				throw new IOException("No more data");
			inputBuffer.flip();
		}
		return inputBuffer.remaining();
	}
	
	public synchronized void write(ByteBuffer [] writeBufs) throws IOException
	{
		socketChannel.write(writeBufs);
	}
	
	public synchronized void write(ByteBuffer writeBuf) throws IOException
	{
		socketChannel.write(new ByteBuffer[]{writeBuf});
	}
	
	public ByteBuffer getInputBuffer()
	{
		return inputBuffer;
	}

	public int getChannelNum()
	{
		return channelNum;
	}

	public ReadProgress getReadProgress()
	{
		return readProgress;
	}

	public void setReadProgress(ReadProgress readProgress)
	{
		this.readProgress = readProgress;
	}
	
	public synchronized void close() throws IOException
	{
		socketChannel.close();
	}
}
