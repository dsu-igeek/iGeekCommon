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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;
import org.newsclub.net.unix.AFUNIXSelectorProvider;
import org.newsclub.net.unix.AFUNIXServerSocketChannelImpl;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import com.igeekinc.util.logging.ErrorLogMessage;

/*
 * A FirehoseTarget listens on a single socket (socket may be bound to the wildcard address so may be listening on 1 or more 
 * addresses, always on the same port).  A FirehoseTarget may have a FirehoseServer and/or FirehoseClient bound to it.  FirehoseTargets are
 * connected to by a FirehoseInitator.  The FirehoseInitiator may initiate a reverse connection, where the FirehoseInitiator has
 * the server and the client is connected to the FirehoseTarget.  This allows for traversal of NAT'd firewalls where there
 * are no open incoming ports.
 */
public class FirehoseTarget
{
	public static final char	kInitiatorIsServer	= 'S';
	public static final char	kInitiatorIsClient	= 'C';
	protected InetSocketAddress serverAddress;
	protected SSLSetup sslSetup;
	protected Selector selector = null;
	protected ServerSocket targetSocket;
	protected ServerSocketChannel targetChannel;
	protected FirehoseServer<?> server;
	protected ReverseClientManager reverseClientManager;
	protected Thread targetSelectThread;
	protected boolean keepRunning;
	protected int nextChannelNum;
	protected ThreadPoolExecutor acceptThreadPool;
	protected Logger logger = Logger.getLogger(getClass());
	
	public FirehoseTarget(InetSocketAddress serverAddress, FirehoseServer<?> server, ReverseClientManager client, SSLSetup sslSetup) throws IOException
	{
		this.serverAddress = serverAddress;
		this.sslSetup = sslSetup;
		if (serverAddress instanceof AFUNIXSocketAddress)
		{
			targetChannel = AFUNIXServerSocketChannelImpl.open((AFUNIXSocketAddress)serverAddress);
			targetSocket = targetChannel.socket();
		}
		else
		{
			targetChannel = ServerSocketChannel.open();
			targetSocket = targetChannel.socket();
			targetSocket.bind(serverAddress);
		}
		this.server = server;
		this.reverseClientManager = client;
		if (server != null)
			server.addTarget(this);
		acceptThreadPool = new ThreadPoolExecutor(2, 16,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new ThreadFactory()
		{
			
			@Override
			public Thread newThread(Runnable arg0)
			{
				return new Thread(arg0, "FirehoseTarget Accept "+FirehoseTarget.this.serverAddress);
			}
		});
		acceptThreadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		createSelectLoop();
		String serverClassName;
		if (server != null)
			serverClassName = server.getClass().toString();
		else
			serverClassName = "<No server>";
		String clientClassName;
		if (client != null)
			clientClassName = client.getClass().toString();
		else
			clientClassName = "<No client>";
		logger.warn("Created FirehostTarget on address "+serverAddress+" server= "+serverClassName+" client = "+clientClassName);
	}

	public void createSelectLoop()
	{
		targetSelectThread = createSelectLoopThread(new Runnable(){

			@Override
			public void run()
			{
				selectLoop();
			}
			
		});
		keepRunning = true;
		targetSelectThread.start();
	}
	
	public Thread createSelectLoopThread(Runnable selectLoopRunnable)
	{
		return new Thread(selectLoopRunnable, "FirehoseTarget accept select "+serverAddress);
	}

	protected void selectLoop()
	{
		try
		{
			if (selector != null)
			{
				throw new IllegalStateException("Cannot run two selectLoops");
			}
			if (targetChannel instanceof AFUNIXServerSocketChannelImpl)
				selector = AFUNIXSelectorProvider.provider().openSelector();
			else
				selector = Selector.open();
			targetChannel.configureBlocking(false);
			targetChannel.register(selector, SelectionKey.OP_ACCEPT);

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
						try
						{
							if (curKey.isAcceptable())
							{
								if (curKey.channel().equals(targetChannel))
								{
									selectedKeysIterator.remove();
									acceptConnection();
								}
							}

						}
						catch (CancelledKeyException c)
						{
							// Oh this was handy
						}
					}
				}
			}
			targetChannel.close();
		} catch (Throwable e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			shutdown();
			if (targetChannel != null)
			{
				try
				{
					targetChannel.close();
				} catch (IOException e1)
				{
					// Well, that was useful
				}
			}
		}
		
	}
	
	class InitializeConnectionRunnable implements Runnable
	{
		SocketChannel channel;
		
		InitializeConnectionRunnable(SocketChannel channel)
		{
			this.channel = channel;
		}
		
		@Override
		public void run()
		{
			initializeConnection(channel);
		}
		
	}
	protected void acceptConnection() throws IOException
	{
		SocketChannel newChannel = targetChannel.accept();
		acceptThreadPool.execute(new InitializeConnectionRunnable(newChannel));
	}
	
	protected void initializeConnection(SocketChannel newChannel)
	{
		try
		{
			if (newChannel != null)
			{
				logger.debug("initializeConnection from "+newChannel.socket().getRemoteSocketAddress());
				FirehoseChannel newTargetChannel;
				newChannel.configureBlocking(false);
				if (sslSetup.useSSL())
				{
					logger.debug("getSSLContextForSocket for "+newChannel.socket().getRemoteSocketAddress());
					SSLContext sslContext = sslSetup.getSSLContextForSocket(newChannel);
					SSLEngine sslEngine = sslContext.createSSLEngine();
					sslEngine.setUseClientMode(false);
					sslEngine.setWantClientAuth(true);
					newTargetChannel = new SSLFirehoseChannel(nextChannelNum++, newChannel, sslEngine, sslSetup, true);
					logger.debug("SSL initialized for "+newChannel.socket().getRemoteSocketAddress());
				}
				else
				{
					newTargetChannel = new FirehoseChannel(nextChannelNum++, newChannel);
				}
				newTargetChannel.configureBlocking(false);
				logger.debug("Getting initByte for "+newChannel.socket().getRemoteSocketAddress());
				ByteBuffer readBuffer = ByteBuffer.allocate(1);
				newTargetChannel.read(readBuffer);
				int initByte = readBuffer.get(0);
				boolean initialized = false;
				if (initByte == kInitiatorIsClient)	// Initiator wants to be a client
				{
					logger.debug("Got kInitiatorIsClient for "+newChannel.socket().getRemoteSocketAddress());
					if (server != null)
					{
						server.addChannel(newTargetChannel);
					}
					else
					{
						newChannel.close();
					}
					initialized = true;
				}
				if (initByte == kInitiatorIsServer)	// Initiator wants to be a server
				{
					logger.debug("Got kInitiatorIsServer for "+newChannel.socket().getRemoteSocketAddress());
					if (reverseClientManager != null)
					{
						reverseClientManager.addClientChannel(newTargetChannel);
					}
					else
					{
						newChannel.close();
					}
					initialized = true;
				}
				if (!initialized)
				{
					throw new IOException("Connection not initialized.  Received initByte = "+initByte);
				}
			}
		} catch (Throwable t)
		{
			logger.error(new ErrorLogMessage("initializeConnection failed"), t);
			try
			{
				newChannel.close();
			}
			catch (IOException e)
			{
				// Whatever
			}
		}
	}

	public void shutdown()
	{
		if (server != null)
			server.removeTarget(this);
		if (reverseClientManager != null)
			reverseClientManager.shutdownClientManager();
		keepRunning = false;
		if (selector != null)
			selector.wakeup();
		try
		{
			if (targetSelectThread != Thread.currentThread())
				targetSelectThread.join();
		} catch (InterruptedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
	}
	
	public InetSocketAddress [] getAddresses()
	{
		ArrayList<InetSocketAddress>returnAddressesList = new ArrayList<InetSocketAddress>();
		InetSocketAddress baseAddress = (InetSocketAddress)targetSocket.getLocalSocketAddress();
		if (!(baseAddress instanceof AFUNIXSocketAddress) && baseAddress.getAddress().isAnyLocalAddress())
		{
			int basePort = baseAddress.getPort();
			try
			{
				Enumeration<NetworkInterface>interfaces = NetworkInterface.getNetworkInterfaces();
				while (interfaces.hasMoreElements())
				{
					NetworkInterface curInterface = interfaces.nextElement();
					if (isUp(curInterface))
					{
						Enumeration<InetAddress> interfaceAddresses = curInterface.getInetAddresses();
						while (interfaceAddresses.hasMoreElements())
							returnAddressesList.add(new InetSocketAddress(interfaceAddresses.nextElement(), basePort));
					}
				}
			} catch (SocketException e)
			{
				Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			}
		}
		else
		{
			returnAddressesList.add(baseAddress);	// Just a plain old IP address - we may not be listening on all addresses
		}

		InetSocketAddress [] returnAddresses = returnAddressesList.toArray(new InetSocketAddress[returnAddressesList.size()]);
		return returnAddresses;
	}

	/*
	 * isUp is a good thing to check but 1.6+ only and we need to support 1.5 for older Macs
	 */
	private boolean isUp(NetworkInterface checkInterface)
	{
		try
		{
			Method isUpMethod = NetworkInterface.class.getMethod("isUp");
			return (Boolean)isUpMethod.invoke(checkInterface);
		} catch (NoSuchMethodException e)
		{
			return true;	// 1.5 or lower - all interfaces are always up
		} catch (SecurityException e)
		{
		} catch (IllegalAccessException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (IllegalArgumentException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		} catch (InvocationTargetException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		}
		throw new InternalError("problem checking isUp");
	}
	
	public int getNumServerConnections()
	{
		return server.getNumConnections();
	}
	
	public String toString()
	{
		return (getClass()+" on "+targetSocket.getLocalSocketAddress());
	}
	
	public String dump()
	{
		return toString();
	}
}
