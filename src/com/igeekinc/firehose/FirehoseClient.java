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
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;

import com.igeekinc.util.async.ComboFutureBase;
import com.igeekinc.util.logging.ErrorLogMessage;
import com.igeekinc.util.logging.InfoLogMessage;

public abstract class FirehoseClient extends FirehoseBase
{
	private boolean closed = false, keepRunning = true;
	private long commandSequence;
	protected Socket socket;
	protected SocketChannel socketChannel;
	protected FirehoseChannel remoteChannel;
	protected HashMap<Long, CommandBlock> outstandingMessages = new HashMap<Long, CommandBlock>();
	protected Logger logger = Logger.getLogger(getClass());
	protected Thread responseThread;
	public FirehoseClient()
	{

	}
	
	public void createResponseLoop() throws IOException
	{
		createResponseLoop(null);
	}
	
	public void createResponseLoop(SSLEngine sslEngine) throws IOException
	{
		//socketChannel.configureBlocking(false);	// We always run in non-blocking mode because of the way input is handled
		if (sslEngine == null)
			remoteChannel = new FirehoseChannel(0, socketChannel);
		else
			remoteChannel = new SSLFirehoseChannel(0, socketChannel, sslEngine, false);
		remoteChannel.configureBlocking(false);
		responseThread = new Thread(new Runnable(){

			@Override
			public void run()
			{
				responseLoop();
			}
		},"RemoteClient response thread");
		responseThread.start();
	}
	
	protected void sendMessage(CommandMessage message, ComboFutureBase<? extends Object>future) throws IOException
	{
		if (closed)
			throw new IOException("Client closed");
		CommandBlock commandBlock;
		synchronized(this)
		{
			logger.debug("Sending command sequence "+commandSequence);
			commandBlock = new CommandBlock(commandSequence, message, future);
			commandSequence++;
			outstandingMessages.put(commandBlock.getCommandSequence(), commandBlock);
		}
		sendCommandAndPayload(remoteChannel, message, commandBlock);
	}

	
	protected void responseLoop()
	{
		try
		{
			while (keepRunning)
			{
				ReceivedPayload receivedPayload = readCommandAndPayload(remoteChannel);
				switch(receivedPayload.getCommandType())
				{
				case kCommand:
					throw new IllegalArgumentException("Received a Command packet in the client response loop");
				case kCommandReply:
					handleCommandReply(receivedPayload);
					break;
				case kCommandFailed:
					handleCommandFailed(receivedPayload);
					break;
				case kUnsolicited:
					handleUnsolicited(receivedPayload);
					break;
				default:
					throw new IllegalArgumentException("Received unknown command type "+receivedPayload.getCommandType());
				}

			}
		}
		catch (Throwable t)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), t);
			try
			{

				synchronized(outstandingMessages)
				{
					for (Entry<Long, CommandBlock>curEntry:outstandingMessages.entrySet())
					{
						CommandBlock abortBlock = curEntry.getValue();
						if (abortBlock != null)
						{
							abortBlock.getFuture().failed(new IOException("Remote server closed connection"), null);
						}
					}
					outstandingMessages.notifyAll();
				}
				close();
			} catch (IOException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
		}
	}
	
	public void close() throws IOException
	{
		close(0);
	}
	
	public synchronized void close(long timeout) throws IOException
	{
		if (!closed)
		{
			synchronized(this)
			{
				closed = true;
			}
			long startTime = System.currentTimeMillis();

			synchronized(outstandingMessages)
			{
				while ((timeout == 0 || (System.currentTimeMillis() - startTime > timeout)) && outstandingMessages.size() > 0)
				{
					long timeToWait = timeout == 0 ? 0:System.currentTimeMillis() - startTime;
					try
					{
						outstandingMessages.wait(timeToWait);
					} catch (InterruptedException e)
					{
						Logger.getLogger(getClass()).info(new InfoLogMessage("Caught exception"), e);
					}
				}

				// Past the wait timeout

				for (Entry<Long, CommandBlock>curEntry:outstandingMessages.entrySet())
				{
					CommandBlock abortBlock = curEntry.getValue();
					if (abortBlock != null)
					{
						abortBlock.getFuture().failed(new IOException("Command not finished before close timeout"), null);
					}
				}
			}
			sendClose(remoteChannel, commandSequence);

			keepRunning = false;
			synchronized(remoteChannel)
			{
				remoteChannel.close();
			}
		}
	}
	protected void handleCommandReply(ReceivedPayload receivedPayload) throws IOException
	{
		CommandBlock commandBlock;
		synchronized(outstandingMessages)
		{
			logger.debug("Processing command reply for sequence "+receivedPayload.getCommandSequence());
			commandBlock = outstandingMessages.remove(receivedPayload.getCommandSequence());
			outstandingMessages.notifyAll();
		}
		if (commandBlock == null)
		{
			logger.error("Did not find commandBlock for "+receivedPayload.getCommandSequence());
			throw new IllegalArgumentException("Did not find command sequence "+receivedPayload.getCommandSequence());
		}
		Class<? extends Object>replyClass = getReturnClassForCommandCode(commandBlock.getMessage().getCommandCode());
		Object reply;
		if (!replyClass.equals(Void.class))
			reply = packer.read(receivedPayload.getPayload(), replyClass);
		else
			reply = null;
		commandBlock.getFuture().completed(reply, null);
	}
	protected void handleCommandFailed(ReceivedPayload receivedPayload) throws IOException
	{
		CommandBlock commandBlock;
		synchronized(outstandingMessages)
		{
			commandBlock = outstandingMessages.remove(receivedPayload.getCommandSequence());
			outstandingMessages.notifyAll();
		}
		if (commandBlock == null)
			throw new IllegalArgumentException("Did not find command sequence "+receivedPayload.getCommandSequence());
		Throwable failureReason = getThrowableForErrorCode(receivedPayload.getCommandCode());
		commandBlock.getFuture().failed(failureReason, null);
	}
	protected void handleUnsolicited(ReceivedPayload receivedPayload)
	{
		
	}
}
