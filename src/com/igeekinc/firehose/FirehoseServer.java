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
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.perf4j.log4j.Log4JStopWatch;

import com.igeekinc.util.RingBuffer;
import com.igeekinc.util.logging.ErrorLogMessage;
import com.igeekinc.util.logging.InfoLogMessage;

class ChannelServicer implements Runnable
{
	private FirehoseServer<?> parent;
	private FirehoseChannel channelToService;
	
	
	public ChannelServicer(FirehoseServer<?> parent, FirehoseChannel channelToService)
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
	private FirehoseServer<?> parent;
	private FirehoseChannel channel;
	private ReadProgress progress;
	
	public CommandProcessor(FirehoseServer<?> parent, FirehoseChannel channel, ReadProgress progress)
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

public abstract class FirehoseServer<C> extends FirehoseBase
{
	private int nextChannelNum = 0;
	protected Selector selector = null;
	protected ArrayList<FirehoseTarget> targets = new ArrayList<FirehoseTarget>();
	protected HashMap<Integer, FirehoseChannel> serverChannelsMap = new HashMap<Integer, FirehoseChannel>();
	protected HashMap<FirehoseChannel, C> clientInfoMap = new HashMap<FirehoseChannel, C>();
	protected Thread selectLoopThread;
	protected boolean keepRunning = true;
	protected ThreadPoolExecutor inputThreadPool, executeCommandThreadPool;
	protected LinkedBlockingQueue<FirehoseChannel> addChannelQueue = new LinkedBlockingQueue<FirehoseChannel>();
	protected RingBuffer<CommandToProcess>history = new RingBuffer<CommandToProcess>(16);	// Keeps track of the 16 most recent commands executed
	public FirehoseServer()
	{
		inputThreadPool = new ThreadPoolExecutor(2, 16,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),new ThreadFactory()
		{
			
			@Override
			public Thread newThread(Runnable arg0)
			{
				return new Thread(arg0, "RemoteServer IO");
			}
		});
		executeCommandThreadPool = new ThreadPoolExecutor(2, 16,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new ThreadFactory()
		{
			
			@Override
			public Thread newThread(Runnable arg0)
			{
				return new Thread(arg0, "RemoteServer Exec");
			}
		});
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
			while(keepRunning)
			{
				logger.debug("selectLoop - checking keys vs clientInfo");
				FirehoseChannel [] clientInfoKeySet;
				synchronized (clientInfoMap)
				{
					clientInfoKeySet = clientInfoMap.keySet().toArray(
							new FirehoseChannel[clientInfoMap.keySet().size()]);
				}
				Set<SelectionKey>selectorKeys = selector.keys();
				if (logger.isDebugEnabled() || clientInfoKeySet.length != selectorKeys.size())
				{
					if (clientInfoKeySet.length != selectorKeys.size() && addChannelQueue.isEmpty())
						logger.error(new ErrorLogMessage("clientInfoKeySet.size != selectorKeys.size and no queues waiting to be added"));
					for (FirehoseChannel checkChannel:clientInfoKeySet)
					{
						boolean found = false;
						for (SelectionKey curKey:selectorKeys)
						{
							if (curKey.channel() == checkChannel.getSocketChannel())
							{
								found = true;
								break;
							}
						}
						if (!found)
						{
							if (!checkChannel.isConnected())
							{
								logger.error(new ErrorLogMessage("Channel {0} disconnected", new Serializable []{checkChannel.toString()}));
								clientInfoMap.remove(checkChannel);
							}
							else
							{
								logger.error(new ErrorLogMessage("Not selecting for channel {0}", new Serializable []{checkChannel.toString()}));
							}
						}
					}
				}
				
				selector.select(60000);
				Log4JStopWatch selectLoop = new Log4JStopWatch("FirehoseServer.selectLoop");
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				int numReadyChannels = selectedKeys.size();
				if (numReadyChannels > 0)
				{
					Iterator<SelectionKey>selectedKeysIterator = selectedKeys.iterator();
					while(selectedKeysIterator.hasNext())
					{
						SelectionKey curKey = selectedKeysIterator.next();
						try
						{
							if (curKey.isReadable())
							{
								selectedKeysIterator.remove();
								FirehoseChannel curChannel = (FirehoseChannel)curKey.attachment();
								if (curChannel != null)
								{
									synchronized(curChannel)
									{
										logger.debug("FirehoseServer selectLoop reading from "+curChannel);
										try
										{
											curChannel.readNoBlock();
										}
										catch (IOException e)
										{
											// Channel was closed on the remote end - we need to close and deregister interest
											closeAndAbortChannel(curChannel);
											for (SelectionKey curCheckKey:selector.keys())
											{
												if (curCheckKey.channel() == curChannel.getSocketChannel())
												{
													logger.debug("Cancelling curCheckKey = "+((SocketChannel)curCheckKey.channel()).socket().getRemoteSocketAddress()+" curChannel = "+curChannel.getSocketChannel().socket().getRemoteSocketAddress());
													curCheckKey.cancel();
												}
											}
											continue;
										}
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
						catch (CancelledKeyException c)
						{
							// Oh this was handy
						}
					}
				}
				while (!addChannelQueue.isEmpty())
				{
					FirehoseChannel addChannel = addChannelQueue.remove();
					logger.debug("Got addChannel remote addr = "+addChannel.getSocketChannel().socket().getRemoteSocketAddress());
					try
					{
						synchronized(serverChannelsMap)
						{
							serverChannelsMap.put(addChannel.getChannelNum(), addChannel);
							addChannel.register(selector, SelectionKey.OP_READ, addChannel);
							queueInputHandling(addChannel);	// Handle any data that may be hanging out in the channel buffer already (this happens with SSL channels)
						}
						synchronized(clientInfoMap)
						{
							C newClientInfo = createClientInfo(addChannel);
							clientInfoMap.put(addChannel, newClientInfo);
						}
					} catch (Throwable e)
					{
						addChannel.close();
						Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
					}
				}
				selectLoop.stop();
			}
		} catch (Throwable e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		
	}
	
	
	public void shutdown()
	{
		keepRunning = false;
		if (selector != null)
			selector.wakeup();
		try
		{
			if (selectLoopThread != null)
				selectLoopThread.join();
		} catch (InterruptedException e)
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
				logger.debug("Handling command "+progress.getCommandSequence()+" for "+channel);
				handleCommand(channel, progress, payload);
				break;
			case kCommandFailed:
				logger.debug("Handling command failed "+progress.getCommandSequence()+" for "+channel);
				break;
			case kCommandReply:
				logger.debug("Handling command reply "+progress.getCommandSequence()+" for "+channel);
				break;
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
		Log4JStopWatch commandWatch = new Log4JStopWatch("handleCommand");
		try
		{
			Class<? extends CommandMessage>payloadClass = getClassForCommandCode(payload.getCommandCode());
			if (payloadClass == null)
				throw new InternalError("Could not retrieve payloadClass for command "+payload.getCommandCode());
			CommandMessage commandMessage = packer.read(payload.getPayload(), payloadClass);
			CommandToProcess commandToProcess = new CommandToProcess(channel, progress.getCommandSequence(), commandMessage);
			logger.debug("handling command sequence = "+progress.getCommandSequence()+" code = "+progress.getCommandCode()+" thread = "+Thread.currentThread().getName());
			C curClientInfo;
			synchronized(clientInfoMap)
			{
				curClientInfo = clientInfoMap.get(channel);
			}
			try
			{
				commandToProcess.setStarted(System.currentTimeMillis());
				history.add(commandToProcess);
				processCommand(curClientInfo, commandToProcess);
			}
			catch (Throwable t)
			{
				logger.error(new ErrorLogMessage("Command "+commandToProcess+" failed with exception"), t);
				commandFailed(commandToProcess, t);
			}
		}
		finally
		{
			commandWatch.stop();
		}
	}

	protected abstract Class<? extends CommandMessage> getClassForCommandCode(int commandCode);
	
	/**
	 * Creates the per-client info that will be passed to processCommand. (no I/O should happen on the channel in createClientInfo)
	 * @param channel
	 * @return
	 */
	protected abstract C createClientInfo(FirehoseChannel channel);
	
	protected void handleInput(FirehoseChannel channel)
	{
		Log4JStopWatch handleInput = new Log4JStopWatch("FirehoseServer.handleInput");
		synchronized(channel)
		{
			try
			{
				do
				{
					ReadProgress progress = channel.getReadProgress();
					if (progress == null)
					{
						logger.debug("Allocating new ReadProgress for "+channel);
						progress = new ReadProgress();
						channel.setReadProgress(progress);
					}
					if (progress.getRequestState() == ReadProgress.RequestState.kProcessing)
					{
						logger.debug("Already in kProcessing state, returning");
						return;	// Shouldn't be here
					}
					processInput(progress, channel, null);
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
								closeChannel(channel);
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
		handleInput.stop();
	}
	
	protected void closeChannel(FirehoseChannel channel)
	{
		// TODO Auto-generated method stub
		
	}
	protected void closeAndAbortChannel(FirehoseChannel channelToClose)
	{
		synchronized(serverChannelsMap)
		{
			serverChannelsMap.remove(channelToClose.getChannelNum());
		}
		synchronized(clientInfoMap)
		{
			clientInfoMap.remove(channelToClose);
		}
		try
		{
			channelToClose.close();
		} catch (IOException e1)
		{
			// Nobody wants to hear about it
		}
	}
	protected void queueInputHandling(FirehoseChannel channel)
	{
		if (inputThreadPool.getActiveCount() > (inputThreadPool.getMaximumPoolSize() - 3))
			logger.info(new InfoLogMessage("input thread pool at "+ inputThreadPool.getActiveCount()));
		inputThreadPool.execute(new ChannelServicer(this, channel));
	}
	
	protected void queueCommandProcessing(FirehoseChannel channel, ReadProgress progress)
	{
		logger.debug("Queuing command for "+channel+" "+executeCommandThreadPool.getActiveCount()+" commands active");
		if (executeCommandThreadPool.getActiveCount() > (executeCommandThreadPool.getMaximumPoolSize() - 3))
			logger.info(new InfoLogMessage("execute command thread pool at "+ executeCommandThreadPool.getActiveCount()));
		try
		{
			executeCommandThreadPool.execute(new CommandProcessor(this, channel, progress));
		}
		catch(RejectedExecutionException e)
		{
			logger.error("executeCommandThreadPool full with "+executeCommandThreadPool.getActiveCount()+" threads running");
			Map<Thread, StackTraceElement[]> threadSet = Thread.getAllStackTraces();
			for (Map.Entry<Thread, StackTraceElement[]>curThread:threadSet.entrySet())
			{
				logger.error("Thread "+curThread.getKey().getId()+":"+curThread.getKey().getName());
				for (StackTraceElement curElement:curThread.getValue())
				{
					logger.error(curElement.toString());
				}
			}
			throw e;
		}
	}
	
	
	protected abstract void processCommand(C clientInfo, CommandToProcess commandToProcess) throws Exception;
	
	protected void commandCompleted(CommandToProcess processedCommand, CommandResult result)
	{
		processedCommand.setFinished(System.currentTimeMillis());
		try
		{
			synchronized(processedCommand.getChannel())
			{
				sendReplyAndPayload(processedCommand.getChannel(), processedCommand.getCommandToProcess().getCommandCode(), processedCommand.getCommandSequence(), result);
			}
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
	}
	
	protected void commandFailed(CommandToProcess failedCommand, Throwable failureReason)
	{
		failedCommand.setFinished(System.currentTimeMillis());
		logger.debug("Command "+failedCommand.getCommandSequence()+" failed for "+failedCommand.getChannel());
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

	public InetSocketAddress [] getListenAddresses(AddressFilter filter)
	{
		FirehoseTarget [] targetsCopy;
		synchronized(targets)
		{
			targetsCopy = targets.toArray(new FirehoseTarget[targets.size()]);	// No telling how long all this will take so copy the targets list
		}
		ArrayList<InetSocketAddress>returnAddressesList = new ArrayList<InetSocketAddress>();
		for (FirehoseTarget curTarget:targetsCopy)
		{
			InetSocketAddress [] targetAddresses = curTarget.getAddresses();
			for (InetSocketAddress addTargetAddress:targetAddresses)
			{
				if (!returnAddressesList.contains(addTargetAddress))
				{
					if (!addTargetAddress.getAddress().isLoopbackAddress() && filter.add(addTargetAddress))
						returnAddressesList.add(addTargetAddress);
				}
			}
		}
		InetSocketAddress [] returnAddresses = returnAddressesList.toArray(new InetSocketAddress[returnAddressesList.size()]);
		return returnAddresses;
	}

	
	public void addTarget(FirehoseTarget addTarget)
	{
		synchronized(targets)
		{
			targets.add(addTarget);
		}
	}
	
	public boolean removeTarget(FirehoseTarget removeTarget)
	{
		synchronized(targets)
		{
			return targets.remove(removeTarget);
		}
	}
	/**
	 * Add a new channel that services a single client.  Any authentication should be finished by this point
	 * @param newServerChannel
	 * @throws IOException
	 */
	public void addChannel(FirehoseChannel newServerChannel) throws IOException
	{
		logger.debug("Adding new server channel remote addr = "+newServerChannel.getSocketChannel().socket().getRemoteSocketAddress());
		synchronized(serverChannelsMap)
		{
			if (selector == null)
			{
				selector = newServerChannel.openSelector();	// Returns the correct one for AFUnix/TCP sockets.  Don't cross the beams!
				createSelectLoop();
			}
			newServerChannel.setChannelNum(nextChannelNum++);
		}
		newServerChannel.configureBlocking(false);
		addChannelQueue.add(newServerChannel);
		selector.wakeup();	// We'll register it on the select loop - avoids lockups in register when select is waiting
	}
	
	public C removeChannel(FirehoseChannel channelToRemove)
	{
		synchronized(clientInfoMap)
		{
			return clientInfoMap.remove(channelToRemove);
		}
	}
	public int getNumConnections()
	{
		return clientInfoMap.size();
	}
	
	public String dump() 
	{
		StringBuffer returnBuffer = new StringBuffer();
		returnBuffer.append("Targets:\n");
		for (FirehoseTarget curTarget:targets)
		{
			returnBuffer.append(curTarget.dump());
			returnBuffer.append("\n");
		}
		returnBuffer.append("Channels:\n");
		Integer [] channelNums = serverChannelsMap.keySet().toArray(new Integer[0]);
		Arrays.sort(channelNums);
		for (Integer curChannelNum:channelNums)
		{
			returnBuffer.append(curChannelNum);
			returnBuffer.append(": ");
			FirehoseChannel curChannel = serverChannelsMap.get(curChannelNum);
			returnBuffer.append(curChannel.dump());
			returnBuffer.append("\n");
		}
		returnBuffer.append("executeCommandThreadPool: ");
		returnBuffer.append(executeCommandThreadPool.toString());
		returnBuffer.append("\nHistory:\n");
		CommandToProcess[] historyArray;
		historyArray = history.toArray(new CommandToProcess[0]);
		for (CommandToProcess curCommand:historyArray)
		{
			returnBuffer.append(curCommand.toString());
			returnBuffer.append("\n");
		}
		return returnBuffer.toString();
	}
}
