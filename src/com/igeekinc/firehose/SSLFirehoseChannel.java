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
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

/**
 * SSLFirehoseChannel is an SSL encrypted FirehoseChannel
 * 
 * @author David L. Smith-Uchida
 *
 */
public class SSLFirehoseChannel extends FirehoseChannel
{
	private SSLEngine	sslEngine;
	private boolean server;
	private ByteBuffer decryptedInputBuffer;
	public SSLFirehoseChannel(int channelNum, SocketChannel socketChannel, SSLEngine sslEngine, boolean server) throws IOException
	{
		super(channelNum, socketChannel);
		socketChannel.configureBlocking(true);
		this.sslEngine = sslEngine;
		this.server = server;
		decryptedInputBuffer = ByteBuffer.allocate(super.getInputBuffer().capacity() + 64);
		
		ByteBuffer sourceBuffer = ByteBuffer.wrap("Firehose".getBytes());
		ByteBuffer encryptedSourceBuffer = ByteBuffer.allocate(64 * 1024);
		ByteBuffer encryptedReceiveBuffer = ByteBuffer.allocate(64 * 1024);
		ByteBuffer receiveBuffer = ByteBuffer.allocate(64 * 1024);
		
		boolean handshaking = true;
		while(handshaking || sourceBuffer.position() < sourceBuffer.limit())
		{
			encryptedSourceBuffer.position(0);
			encryptedSourceBuffer.limit(encryptedSourceBuffer.capacity());
			SSLEngineResult result = sslEngine.wrap(sourceBuffer, encryptedSourceBuffer);
			logger.debug("wrap status = "+result.getStatus()+" handshake status = "+result.getHandshakeStatus());
			runDelegatedTasks(result);
			encryptedSourceBuffer.flip();
			logger.debug("Sending "+encryptedSourceBuffer);
			socketChannel.write(encryptedSourceBuffer);
			if (result.getHandshakeStatus() != HandshakeStatus.FINISHED)
			{
				if (result.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP)
				{
					encryptedReceiveBuffer.position(0);
					encryptedReceiveBuffer.limit(encryptedReceiveBuffer.capacity());
					logger.debug("Receiving "+encryptedReceiveBuffer);
					socketChannel.read(encryptedReceiveBuffer);
					logger.debug("Received "+encryptedReceiveBuffer);
					encryptedReceiveBuffer.flip();
					SSLEngineResult unwrapResult;
					do
					{
						unwrapResult = sslEngine.unwrap(encryptedReceiveBuffer, receiveBuffer);
						logger.debug("unwrap status = "+unwrapResult.getStatus()+" handshake status = "+unwrapResult.getHandshakeStatus() + "receiveBuffer.position = "+receiveBuffer.position());

						runDelegatedTasks(unwrapResult);
						if (unwrapResult.getHandshakeStatus() == HandshakeStatus.FINISHED)
							handshaking = false;
					} while (encryptedReceiveBuffer.hasRemaining());
				}
			}
			else
			{
				handshaking = false;
			}
		}

		while (receiveBuffer.position() < 8)
		{
			logger.debug("Final receive, receiveBuffer.position() = "+receiveBuffer.position());
			encryptedReceiveBuffer.position(0);
			encryptedReceiveBuffer.limit(encryptedReceiveBuffer.capacity());
			logger.debug("Final receiving "+encryptedReceiveBuffer);
			socketChannel.read(encryptedReceiveBuffer);
			logger.debug("Final received "+encryptedReceiveBuffer);
			encryptedReceiveBuffer.flip();
			SSLEngineResult unwrapResult = sslEngine.unwrap(encryptedReceiveBuffer, receiveBuffer);
			logger.debug("final unwrap status = "+unwrapResult.getStatus()+" handshake status = "+unwrapResult.getHandshakeStatus()+" receiveBuffer.position = "+receiveBuffer.position());
			runDelegatedTasks(unwrapResult);
		}
		
		receiveBuffer.flip();	// I really, really hate ByteBuffers
		receiveBuffer.position(8);	// Skip the initial message
		if (receiveBuffer.hasRemaining())
		{
			logger.debug("At end of init, moving "+receiveBuffer.remaining()+" bytes to decryptedInputBuffer");
			decryptedInputBuffer.put(receiveBuffer);
			decryptedInputBuffer.flip();
		}
		else
		{
			decryptedInputBuffer.limit(0);	// Start empty
		}
		socketChannel.configureBlocking(false);
	}

	public SSLEngine getSSLEngine()
	{
		return sslEngine;
	}

	@Override
	public synchronized int readNoBlock() throws IOException
	{
		logger.debug("Entering SSLFirehoseChannel readNoBlock");
		if (!decryptedInputBuffer.hasRemaining())
		{
			logger.debug("No input remaining in decryptedInputBuffer");
			decryptedInputBuffer.position(0);
			decryptedInputBuffer.limit(decryptedInputBuffer.capacity());
			int bytesAvailable = super.readNoBlock();
			logger.debug(bytesAvailable+" bytes available from parent channel");
			if (bytesAvailable > 0)
			{
				ByteBuffer inputBuffer = super.getInputBuffer();
				boolean keepUnwrapping = true;
				while (inputBuffer.hasRemaining())	// 
				{
					SSLEngineResult engineResult = sslEngine.unwrap(inputBuffer, decryptedInputBuffer);
					runDelegatedTasks(engineResult);
					logger.debug("Decrypted "+decryptedInputBuffer+" engineResult = "+engineResult);
					if (engineResult.getStatus() != Status.OK)
						keepUnwrapping = false;	// Probably a buffer underflow which means we're expecting more real input
				}
				
			}
			decryptedInputBuffer.flip();
		}
		logger.debug("Returning "+decryptedInputBuffer.remaining());
		return decryptedInputBuffer.remaining();
	}

	@Override
	public ByteBuffer getInputBuffer()
	{
		return decryptedInputBuffer;
	}

	@Override
	public synchronized void write(ByteBuffer writeBuf) throws IOException
	{
		write(new ByteBuffer []{writeBuf});
	}

	@Override
	public synchronized void write(ByteBuffer[] writeBufs) throws IOException
	{
		int maxWrite = 64 * 1024;
		for (ByteBuffer checkBuffer:writeBufs)
		{
			if (checkBuffer.limit() > maxWrite)
				maxWrite += checkBuffer.limit();
		}
		ByteBuffer encryptedBuffer = ByteBuffer.allocate(maxWrite + 64);
		for (ByteBuffer writeBuffer:writeBufs)
		{
			while (writeBuffer.hasRemaining())	// There may be some SSL overhead or startup that needs to send 
												// more encrypted data than there is unencrypted data
			{
				SSLEngineResult result = sslEngine.wrap(writeBuffer, encryptedBuffer);
				runDelegatedTasks(result);
				encryptedBuffer.flip();
				super.write(encryptedBuffer);
				encryptedBuffer.position(0);
				encryptedBuffer.limit(encryptedBuffer.capacity());
			}
		}
	}

	private void runDelegatedTasks(SSLEngineResult result)
	{
        if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK)
        {
            Runnable runnable;
            while ((runnable = sslEngine.getDelegatedTask()) != null)
            {
                logger.debug("Running delegated task...");
                runnable.run();
            }
            HandshakeStatus hsStatus = sslEngine.getHandshakeStatus();
            if (hsStatus == HandshakeStatus.NEED_TASK)
            {
                throw new InternalError(
                    "handshake shouldn't need additional tasks");
            }
            logger.debug("New HandshakeStatus: " + hsStatus);
        }
    }

	@Override
	public synchronized void close() throws IOException
	{
		sslEngine.closeOutbound();
		SSLEngineResult.Status status = Status.OK;
		ByteBuffer sourceBuffer = ByteBuffer.allocate(0);
		ByteBuffer encryptedBuffer = ByteBuffer.allocate(64*1024);
		logger.debug("Closing SSLFirehoseChannel");
		while (status != Status.CLOSED)
		{
			SSLEngineResult result = sslEngine.wrap(sourceBuffer, encryptedBuffer);
			encryptedBuffer.flip();
			super.write(encryptedBuffer);
			status = result.getStatus();
		}
		super.close();
		logger.debug("SSLFirehoseChannel closed");
	}

}
