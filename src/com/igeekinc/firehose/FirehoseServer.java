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
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;
import org.newsclub.net.unix.AFUNIXSelectorProvider;
import org.newsclub.net.unix.AFUNIXServerSocketChannelImpl;

import com.igeekinc.util.logging.ErrorLogMessage;

class ChannelServicer implements Runnable
{
	private FirehoseServer parent;
	private FirehoseChannel channelToService;
	
	
	public ChannelServicer(FirehoseServer parent, FirehoseChannel channelToService)
	{
		this.parent = parent;
		this.channelToService = channelToService;
	}

	@Override
	public void run()
	{
		parent.handleInput(channelToService);
	}
}

class CommandProcessor implements Runnable
{
	private FirehoseServer parent;
	private FirehoseChannel channel;
	private ReadProgress progress;
	
	public CommandProcessor(FirehoseServer parent, FirehoseChannel channel, ReadProgress progress)
	{
		super();
		this.parent = parent;
		this.channel = channel;
		this.progress = progress;
	}

	@Override
	public void run()
	{
		parent.processProgress(channel, progress);
	}
}

public abstract class FirehoseServer extends FirehoseBase
{
	private int nextChannelNum = 0;
	protected Selector selector = null;
	protected ServerSocket serverSocket;
	protected ServerSocketChannel serverSocketChannel;
	protected HashMap<Integer, FirehoseChannel> serverChannels = new HashMap<Integer, FirehoseChannel>();
	protected Thread selectLoopThread;
	protected SSLContext sslContext;
	protected boolean keepRunning = true;
	
	protected ExecutorService inputThreadPool = Executors.newCachedThreadPool(new ThreadFactory()
	{
		
		@Override
		public Thread newThread(Runnable arg0)
		{
			return new Thread(arg0, "RemoteServer IO");
		}
	});
	protected ExecutorService executeCommandThreadPool = Executors.newCachedThreadPool(new ThreadFactory()
	{
		
		@Override
		public Thread newThread(Runnable arg0)
		{
			return new Thread(arg0, "RemoteServer IO");
		}
	});
	
	public FirehoseServer()
	{
		
	}
	public void createSelectLoop()
	{
		selectLoopThread = createSelectLoopThread(new Runnable(){

			@Override
			public void run()
			{
				selectLoop();
			}
			
		});
		selectLoopThread.start();
	}
	
	public abstract Thread createSelectLoopThread(Runnable selectLoopRunnable);
	
	protected void selectLoop()
	{
		try
		{
			if (selector != null)
			{
				throw new IllegalStateException("Cannot run two selectLoops");
			}
			if (serverSocketChannel instanceof AFUNIXServerSocketChannelImpl)
				selector = AFUNIXSelectorProvider.provider().openSelector();
			else
				selector = Selector.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			while(keepRunning)
			{
				selector.select();
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				int numReadyChannels = selectedKeys.size();
				if (numReadyChannels > 0)
				{
					Iterator<SelectionKey>selectedKeysIterator = selectedKeys.iterator();
					while(selectedKeysIterator.hasNext())
					{
						SelectionKey curKey = selectedKeysIterator.next();
						if (curKey.isAcceptable())
						{
							if (curKey.channel().equals(serverSocketChannel))
							{
								selectedKeysIterator.remove();
								acceptConnection();
							}
						}
						else
						{
							if (curKey.isReadable())
							{
								selectedKeysIterator.remove();
								FirehoseChannel curChannel = (FirehoseChannel)curKey.attachment();
								if (curChannel != null)
								{
									// If we're not busy, go ahead and do the work on this thread and avoid
									// the wakeup and thread-switch overhead
									if (numReadyChannels > 1)
										queueInputHandling(curChannel);
									else
										handleInput(curChannel);
									
								}
							}
						}
					}
				}
			}
		} catch (Throwable e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		
	}
	
	
	public void shutdown()
	{
		keepRunning = false;
		selector.wakeup();
		try
		{
			selectLoopThread.join();
			serverSocketChannel.close();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
	}
	
	
	public void processProgress(FirehoseChannel channel, ReadProgress progress)
	{
		try
		{
			ReceivedPayload payload = getReceivedPayload(progress);
			progress.reset();   // Get ready for the next input
			switch(payload.getCommandType())
			{
			case kCommand:
				handleCommand(channel, progress, payload);
				break;
			case kCommandFailed:
			case kCommandReply:
			}
		} catch (Throwable e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			unexpectedException(channel, e);
		}
	}
	
	protected void handleCommand(FirehoseChannel channel, ReadProgress progress,
			ReceivedPayload payload) throws IOException
	{
		Class<? extends CommandMessage>payloadClass = getClassForCommandCode(payload.getCommandCode());
		CommandMessage commandMessage = packer.read(payload.getPayload(), payloadClass);
		CommandToProcess commandToProcess = new CommandToProcess(channel, progress.getCommandSequence(), commandMessage);
		logger.debug("handling command sequence = "+progress.getCommandSequence()+" code = "+progress.getCommandCode()+" thread = "+Thread.currentThread().getName());
		try
		{
			processCommand(commandToProcess);
		}
		catch (Throwable t)
		{
			commandFailed(commandToProcess, t);
		}
	}

	protected void acceptConnection() throws IOException
	{
		SocketChannel newChannel = serverSocketChannel.accept();
		if (newChannel != null)
		{
			FirehoseChannel newServerChannel;
			if (sslContext != null)
			{
				SSLEngine sslEngine = sslContext.createSSLEngine();
				sslEngine.setUseClientMode(false);
				newServerChannel = new SSLFirehoseChannel(nextChannelNum++, newChannel, sslEngine, true);
			}
			else
			{
				newServerChannel = new FirehoseChannel(nextChannelNum++, newChannel);
			}
			synchronized(serverChannels)
			{
				serverChannels.put(newServerChannel.getChannelNum(), newServerChannel);
			}
			newServerChannel.configureBlocking(false);
			newChannel.register(selector, SelectionKey.OP_READ, newServerChannel);
		}
	}
	
	protected void handleInput(FirehoseChannel channel)
	{
		synchronized(channel)
		{
			try
			{
				do
				{
					ReadProgress progress = channel.getReadProgress();
					if (progress == null)
					{
						logger.debug("Allocating new ReadProgress");
						progress = new ReadProgress();
						channel.setReadProgress(progress);
					}
					if (progress.getRequestState() == ReadProgress.RequestState.kProcessing)
					{
						logger.debug("Already in kProcessing state, returning");
						return;	// Shouldn't be here
					}
					processInput(progress, channel);
					if (progress.getRequestState() == ReadProgress.RequestState.kProcessing)
					{
						if (progress.getCommandType() == kCommand)
						{
							// Time to start processing
							channel.setReadProgress(null);
							queueCommandProcessing(channel, progress);
						}
						else
						{
							if (progress.getCommandType() == kClose)
							{
								// Do an orderly close
								
							}
							else
							{
								// Bad command type - just abort everything
								channel.close();
							}

						}
					}
				} while (channel.getInputBuffer().hasRemaining());
			} catch (Throwable e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
				closeAndAbortChannel(channel);
			}
		}
	}
	
	protected void closeAndAbortChannel(FirehoseChannel channelToClose)
	{
		synchronized(serverChannels)
		{
			serverChannels.remove(channelToClose.getChannelNum());
		}
		try
		{
			channelToClose.close();
		} catch (IOException e1)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e1);
		}
	}
	protected void queueInputHandling(FirehoseChannel channel)
	{
		inputThreadPool.execute(new ChannelServicer(this, channel));
	}
	
	protected void queueCommandProcessing(FirehoseChannel channel, ReadProgress progress)
	{
		executeCommandThreadPool.execute(new CommandProcessor(this, channel, progress));
	}
	
	
	protected abstract void processCommand(CommandToProcess commandToProcess) throws Exception;
	
	protected void commandCompleted(CommandToProcess processedCommand, CommandResult result)
	{
		try
		{
			sendReplyAndPayload(processedCommand.getChannel(), processedCommand.getCommandToProcess().getCommandCode(), processedCommand.getCommandSequence(), result);
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
	}
	
	protected void commandFailed(CommandToProcess failedCommand, Throwable failureReason)
	{
		short errorCode = (short)getErrorCodeForThrowable(failureReason);
		try
		{
			sendErrorReply(failedCommand.getChannel(), errorCode, failedCommand.getCommandSequence());
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
	}
	protected void unexpectedException(FirehoseChannel channel, Throwable t)
	{
		try
		{
			channel.close();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
	}
	public int getServerPort()
	{
		return serverSocket.getLocalPort();
	}
	

	public SocketAddress getAddress()
	{
		return serverSocket.getLocalSocketAddress();
	}

}
