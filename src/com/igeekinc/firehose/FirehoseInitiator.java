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
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.newsclub.net.unix.AFUNIXSocketChannelImpl;

import com.igeekinc.util.logging.DebugLogMessage;

/**
 * FirehoseInitiator initiates a connection to a FirehoseTarget.  The connection will then be based to a FirehoseClient (typically)
 * but could also be reversed and given to a FirehoseTarget.  This enables a Firehose server to offer services to a client
 * through a firewall (the client's target port must be reachable).
 * @author David L. Smith-Uchida
 *
 */
public class FirehoseInitiator
{
	protected Logger logger = Logger.getLogger(getClass());;
	
	public static void initiateClient(SocketAddress address, FirehoseClient client) throws IOException
	{
		initiateClient(address, client, null);
	}
	
	public static void initiateClient(SocketAddress address, FirehoseClient client, SSLSetup sslSetup) throws IOException
	{
		initiateClient(new SocketAddress[]{address}, client, sslSetup);
	}
	
	public static void initiateClient(SocketAddress addresses[], FirehoseClient client, SSLSetup sslSetup) throws IOException
	{
		FirehoseChannel channel = initiateConnection(addresses, sslSetup);
		startClient(channel, client);
	}
	
	public static void initiateServer(SocketAddress address, FirehoseServer<?> server, SSLSetup sslSetup) throws IOException
	{
		initiateServer(new SocketAddress[]{address}, server, sslSetup);
	}
	
	public static void initiateServer(SocketAddress [] addresses, FirehoseServer<?> server, SSLSetup sslSetup) throws IOException
	{
		FirehoseChannel channel = initiateConnection(addresses, sslSetup);
		startServer(channel, server);
	}

	protected static FirehoseChannel initiateConnection(SocketAddress [] addresses, SSLSetup sslSetup) throws IOException
	{
		Socket initiatorSocket;
		SocketChannel initiatorChannel;
		FirehoseChannel initiatorFirehoseChannel;
		Logger logger = Logger.getLogger(FirehoseChannel.class);
		initiatorChannel = openChannel(addresses, 10000);
		boolean succeeded = false;
		try
		{
			initiatorChannel.configureBlocking(false);
			initiatorSocket = initiatorChannel.socket();

			if (sslSetup != null && sslSetup.useSSL())
			{
				logger.debug("FirehoseInitiator getting SSLContext to "+initiatorSocket.getRemoteSocketAddress());
				SSLContext sslContext = sslSetup.getSSLContextForSocket(initiatorChannel);
				SSLEngine sslEngine = sslContext.createSSLEngine();
				sslEngine.setUseClientMode(true);
				initiatorFirehoseChannel = new SSLFirehoseChannel(0, initiatorChannel, sslEngine, sslSetup, false /* For SSL purposes we are the client */);		
				logger.debug("FirehoseInitiator SSLContext completed to "+initiatorSocket.getRemoteSocketAddress());
			}
			else
			{
				initiatorFirehoseChannel = new FirehoseChannel(0, initiatorChannel);
			}
			initiatorChannel.configureBlocking(false);
			succeeded = true;
			return initiatorFirehoseChannel;
		}
		finally
		{
			if (!succeeded)
				initiatorChannel.close();
		}
	}

	protected static SocketChannel openChannel(SocketAddress [] addresses, long timeout) throws IOException
	{
		SocketChannel returnChannel = null;
		Logger logger = Logger.getLogger(FirehoseChannel.class);
		if (addresses == null || addresses.length == 0)
			throw new IllegalArgumentException("Must specify at least one address to connect to");
		if (addresses.length > 1)
		{
			logger.debug("openChannel for multiple ("+addresses.length+") addresses");
			Selector openSelector = Selector.open();
			SocketChannel [] openChannels = new SocketChannel[addresses.length];
			int curChannelNum = 0;
			for (SocketAddress openAddress:addresses)
			{
				logger.debug("Trying address "+openAddress);
				if (openAddress instanceof AFUNIXSocketAddress)
					throw new IllegalArgumentException("AFUNIXSocketAddress must be specified by itself");
				SocketChannel channel = SocketChannel.open();
				channel.configureBlocking(false);
				channel.register(openSelector, SelectionKey.OP_CONNECT);
				try
				{
					channel.connect(openAddress);
				} catch (IOException e)
				{
					// Well, we don't like that address
					logger.debug("Got exception opening connection to "+openAddress, e);
				}
				openChannels[curChannelNum] = channel;
				curChannelNum++;
				int numSelected = openSelector.select(20);	// Enough time for a quick connect
				if (numSelected > 0)
				{
					logger.debug("Success for address "+openAddress);
					returnChannel = checkSelectorForSuccess(openSelector);
					if (returnChannel != null)	// Someone answered, we can stop opening connections
						break;
				}
			}
			if (returnChannel == null)
				logger.debug("No immediate connection");
			// OK, no one answered quickly, give it a little time
			while (returnChannel == null )
			{
				int numSelected = openSelector.select(timeout);
				if (numSelected == 0)
				{
					break;
				}
				returnChannel = checkSelectorForSuccess(openSelector);
			}
			openSelector.close();

			for (int closeChannelNum = 0; closeChannelNum < openChannels.length; closeChannelNum++)
			{
				if (openChannels[closeChannelNum] != null && openChannels[closeChannelNum] != returnChannel)
					openChannels[closeChannelNum].close();
			}
		}
		else
		{
			logger.debug("openChannel for single address "+addresses[0]);

			if (addresses[0] instanceof AFUNIXSocketAddress)
				returnChannel = AFUNIXSocketChannelImpl.open((AFUNIXSocketAddress)addresses[0]);
			else
				returnChannel = SocketChannel.open(addresses[0]);
		}
		if (returnChannel == null)
		{
			String exceptionMessage = "Could not open path to";
			for (SocketAddress openAddress:addresses)
				exceptionMessage = exceptionMessage+" "+openAddress.toString();
			throw new IOException(exceptionMessage);
		}
		return returnChannel;
	}
	
	private static SocketChannel checkSelectorForSuccess(Selector openSelector)
	{		
		Logger logger = Logger.getLogger(FirehoseChannel.class);
		SocketChannel returnChannel = null;
		// The first one to return may not actually have connected so spool through the list here
		Iterator<SelectionKey> selectedIterator = openSelector.selectedKeys().iterator();
		while(selectedIterator.hasNext())
		{
			SelectionKey cur = selectedIterator.next();
			SocketChannel checkChannel = (SocketChannel) cur.channel();
			//logger.debug("Got connection for "+returnChannel.socket().getRemoteSocketAddress());
			try
			{
				if (checkChannel.finishConnect())
				{
					returnChannel = checkChannel;
					break;
				}
			} catch (IOException e)
			{
				logger.debug(new DebugLogMessage("Failed connection to {0}", new Serializable[]{checkChannel.toString()}));
			}
		}
		return returnChannel;
	}
	public static void startClient(FirehoseChannel channel, FirehoseClient client) throws IOException
	{
		Logger.getLogger(FirehoseInitiator.class).debug("Sending client initByte ("+FirehoseTarget.kInitiatorIsClient+") to "+channel.getSocketChannel().socket().getRemoteSocketAddress());
		channel.configureBlocking(false);
		ByteBuffer writeBuffer = ByteBuffer.wrap(new byte[]{FirehoseTarget.kInitiatorIsClient});
		while (writeBuffer.hasRemaining())
		{
			long bytesWritten = channel.write(writeBuffer);
			if (bytesWritten == 0)
			{
				try
				{
					Thread.sleep(10);
				}
				catch(InterruptedException e)
				{

				}
			}
		}
		client.addChannel(channel);
		Logger.getLogger(FirehoseInitiator.class).debug("Client initByte sent to "+channel.getSocketChannel().socket().getRemoteSocketAddress());
	}
	
	public static void startServer(FirehoseChannel channel, FirehoseServer server) throws IOException
	{
		Logger.getLogger(FirehoseInitiator.class).debug("Sending server initByte ("+FirehoseTarget.kInitiatorIsServer+") to "+channel.getSocketChannel().socket().getRemoteSocketAddress());
		channel.configureBlocking(false);
		ByteBuffer writeBuffer = ByteBuffer.wrap(new byte[]{FirehoseTarget.kInitiatorIsServer});
		/*
		if (channel.write(writeBuffer) != 1)
			throw new IOException("kInitiatorIsServer initiator byte not sent");
			*/
		while (writeBuffer.hasRemaining())
		{
			long bytesWritten = channel.write(writeBuffer);
			if (bytesWritten == 0)
			{
				try
				{
					Thread.sleep(10);
				}
				catch(InterruptedException e)
				{

				}
			}
		}
		server.addChannel(channel);
		Logger.getLogger(FirehoseInitiator.class).debug("Server initByte sent to "+channel.getSocketChannel().socket().getRemoteSocketAddress());
	}
}
