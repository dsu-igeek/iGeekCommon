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
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
	protected Logger logger = Logger.getLogger(getClass());
	protected Lock readLock = new ReentrantLock(), writeLock = new ReentrantLock();

	public FirehoseChannel(int channelNum, SocketChannel socketChannel) throws IOException
	{
		this.channelNum = channelNum;
		this.socketChannel = socketChannel;
		inputBuffer = ByteBuffer.allocate(128 * 1024);
		inputBuffer.limit(0);	// Set limit to zero so we'll read something
	}
	
	public void configureBlocking(boolean block) throws IOException
	{
		readLock.lock();
		try
		{
			socketChannel.configureBlocking(block);
		}
		finally
		{
			readLock.unlock();
		}
	}
	
	public Selector openSelector() throws IOException
	{
		if (socketChannel instanceof AFUNIXSocketChannelImpl)
			return AFUNIXSelectorProvider.provider().openSelector();
		else
			return Selector.open();
	}

	public void register(Selector selector, int ops, Object attachment) throws ClosedChannelException
	{
		socketChannel.register(selector, ops, attachment);
	}

	public int readNoBlock() throws IOException
	{
		return readNoBlock(0);	// We don't have a minimum
	}
	/**
	 * Will either read more data from the socket or just return if data is available in the input buffer
	 * @param forceReadAmount If forceReadAmount is more than
	 *  the number of bytes in the inputBuffer, a read will be forced.  May return more or less than the number of bytes requested
	 * @return
	 * @throws IOException 
	 */
	public int readNoBlock(int forceReadAmount) throws IOException
	{
		if (socketChannel.isBlocking())
			throw new IllegalBlockingModeException();
		readLock.lock();
		try
		{
			if (!inputBuffer.hasRemaining() || inputBuffer.remaining() < forceReadAmount)
			{
				inputBuffer.compact();
				int bytesRead = socketChannel.read(inputBuffer);
				if (bytesRead < 0)
					throw new IOException("No more data");
				inputBuffer.flip();
			}
			return inputBuffer.remaining();
		}
		finally
		{
			readLock.unlock();
		}
	}
	
	public int readNoBlock(ByteBuffer buffer) throws IOException
	{
		readLock.lock();
		try
		{
			int bytesRead = 0;
			if (buffer.remaining() > 0 && inputBuffer.remaining() > 0)
			{
				ByteBuffer readBuffer;
				if (inputBuffer.remaining() > buffer.remaining())
				{
					readBuffer = inputBuffer.duplicate();
					readBuffer.limit(readBuffer.position() + buffer.remaining());
					inputBuffer.position(inputBuffer.position() + buffer.remaining());
				}
				else
				{
					readBuffer = inputBuffer;
				}
				bytesRead = readBuffer.remaining();
				buffer.put(readBuffer);
			}
			if (buffer.remaining() > 0)
			{
				bytesRead += socketChannel.read(buffer);
			}
			return bytesRead;
		}
		finally
		{
			readLock.unlock();
		}
	}
	
	/*
	 * Reads the full buffer or fails with IOException.
	 */
	public int read(ByteBuffer buffer) throws IOException
	{
		int bytesRead = buffer.remaining();	// We'll either read it all or throw an IOException
		readLock.lock();
		try
		{
			boolean blocking = socketChannel.isBlocking();
			if (blocking)
				socketChannel.configureBlocking(false);
			while (buffer.remaining() > 0)
			{
				int bytesReadNoBlock = readNoBlock(buffer);
				if (bytesReadNoBlock == 0)
				{
					try
					{
						Thread.sleep(10);
					}
					catch (InterruptedException e)
					{
						
					}
				}
			}
			if (blocking)
				socketChannel.configureBlocking(true);
		}
		finally
		{
			readLock.unlock();
		}
		return bytesRead;
	}
	
	public long write(ByteBuffer [] writeBufs) throws IOException
	{
		writeLock.lock();
		try
		{
			return socketChannel.write(writeBufs);
		}
		finally
		{
			writeLock.unlock();
		}
	}
	
	public long write(ByteBuffer writeBuf) throws IOException
	{
		writeLock.lock();
		try
		{
			return socketChannel.write(new ByteBuffer[]{writeBuf});
		}
		finally
		{
			writeLock.unlock();
		}
	}
	
	public ByteBuffer getInputBuffer()
	{
		return inputBuffer;
	}

	protected void setChannelNum(int channelNum)
	{
		this.channelNum = channelNum;
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
	
	public void close() throws IOException
	{
		readLock.lock();
		try
		{
			socketChannel.close();
		}
		finally
		{
			readLock.unlock();
		}
	}
	
	public SocketChannel getSocketChannel()
	{
		return socketChannel;
	}
	
	public String toString()
	{
		return "#"+channelNum+" = "+socketChannel.socket().getRemoteSocketAddress()+"\n";
	}
	
	public String dump()
	{
		StringBuffer returnBuffer = new StringBuffer();
		returnBuffer.append(toString());
		ReadProgress saveProgress = readProgress;
		if (saveProgress != null)
		{
			returnBuffer.append("  readProgress:");
			returnBuffer.append(saveProgress.toString());
			returnBuffer.append("\n");
		}
		return returnBuffer.toString();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + channelNum;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FirehoseChannel other = (FirehoseChannel) obj;
		if (channelNum != other.channelNum)
			return false;
		return true;
	}
	
	public boolean isConnected()
	{
		return socketChannel.socket().isClosed();
	}
}
