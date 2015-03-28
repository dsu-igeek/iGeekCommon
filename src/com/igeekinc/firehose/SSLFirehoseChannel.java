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
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SocketChannel;
import java.security.cert.Certificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.log4j.Logger;
import org.perf4j.log4j.Log4JStopWatch;

import com.igeekinc.util.logging.ErrorLogMessage;

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
	private ByteBuffer encryptedBuffer = ByteBuffer.allocate(kSSLMaxWrite + 64);
	private SSLSetup sslSetup;	// We hang onto this for clients to use
	public SSLFirehoseChannel(int channelNum, SocketChannel socketChannel, SSLEngine sslEngine, SSLSetup sslSetup, boolean server) throws IOException
	{
		super(channelNum, socketChannel);
		boolean wasBlocking = false;
		if (socketChannel.isBlocking())
		{
			socketChannel.configureBlocking(false);
			wasBlocking = true;
		}
		this.sslEngine = sslEngine;
		this.sslSetup = sslSetup;
		this.server = server;
		decryptedInputBuffer = ByteBuffer.allocate(super.getInputBuffer().capacity() + 64);
		
		ByteBuffer sendBuffer = ByteBuffer.wrap("Firehose".getBytes());
		ByteBuffer encryptedSendBuffer = ByteBuffer.allocate(64 * 1024);
		ByteBuffer receiveBuffer = ByteBuffer.allocate(64 * 1024);
		
		handshaking = true;
		while(handshaking || sendBuffer.position() < sendBuffer.limit())
		{
			encryptedSendBuffer.position(0);
			encryptedSendBuffer.limit(encryptedSendBuffer.capacity());
			SSLEngineResult result = sslEngine.wrap(sendBuffer, encryptedSendBuffer);
			logger.debug("wrap status = "+result.getStatus()+" handshake status = "+result.getHandshakeStatus());
			runDelegatedTasks(result);
			encryptedSendBuffer.flip();
			logger.debug("Sending "+encryptedSendBuffer);
			int minimumRead = 0;
			while(encryptedSendBuffer.hasRemaining())
			{
				long bytesWritten = super.write(encryptedSendBuffer);
				if (bytesWritten == 0)
				{
					try
					{
						Thread.sleep(10);	// No room in the socket output...odd but we need to deal with it
					}
					catch (InterruptedException e)
					{
						
					}
				}
			}
			if (result.getHandshakeStatus() != HandshakeStatus.FINISHED)
			{
				if (result.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP)
				{
					int timesRead = 0;
					boolean readMore = false;
					do
					{
						timesRead++;
						logger.debug("Receiving SSL handshake data");
						int bytesAvailable = 0;
						while (bytesAvailable == 0)
						{
							bytesAvailable = super.readNoBlock(minimumRead);
							if (bytesAvailable == 0)
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
						logger.debug(bytesAvailable+" bytes available from parent channel");
						ByteBuffer encryptedProcessBuffer = super.getInputBuffer();
						SSLEngineResult unwrapResult;
						do
						{
							try
							{
								unwrapResult = sslEngine.unwrap(encryptedProcessBuffer, receiveBuffer);
							} catch (SSLException e)
							{
								Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
								throw e;
							}
							logger.debug("unwrap status = "+unwrapResult.getStatus()+" handshake status = "+unwrapResult.getHandshakeStatus() + "receiveBuffer.position = "+receiveBuffer.position());

							runDelegatedTasks(unwrapResult);
							if (unwrapResult.getHandshakeStatus() == HandshakeStatus.FINISHED)
								handshaking = false;
							if (unwrapResult.getStatus() == Status.BUFFER_UNDERFLOW)
							{
								readMore = true;
								minimumRead = encryptedProcessBuffer.remaining() + 1;
								break;
							}
							else
							{
								minimumRead = 0;
								readMore = false;
							}
						} while (encryptedProcessBuffer.hasRemaining());
					} while (readMore);
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
			int bytesAvailable = 0;
			while (bytesAvailable == 0)
			{
				bytesAvailable = super.readNoBlock(sslEngine.getSession().getPacketBufferSize());
				if (bytesAvailable == 0)
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
			ByteBuffer encryptedProcessBuffer = super.getInputBuffer();
			SSLEngineResult unwrapResult = sslEngine.unwrap(encryptedProcessBuffer, receiveBuffer);
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
		if (wasBlocking)
			socketChannel.configureBlocking(true);	// Return it the way we found it
		if (super.getInputBuffer().remaining() > 0)
			logger.debug("more decrypted data waiting");
	}

	public SSLEngine getSSLEngine()
	{
		return sslEngine;
	}

	@Override
	public int readNoBlock(int forceReadAmount) throws IOException
	{
		if (getSocketChannel().isBlocking())
			throw new IllegalBlockingModeException();
		readLock.lock();
		try
		{
			logger.debug("Entering SSLFirehoseChannel readNoBlock");
			if (!decryptedInputBuffer.hasRemaining() || decryptedInputBuffer.remaining() < forceReadAmount)
			{
				logger.debug("No input remaining in decryptedInputBuffer");
				decryptedInputBuffer.compact();
				if (getSocketChannel().isBlocking())
					logger.debug("readNoBlock called in blocking mode");
				int bytesAvailable = super.readNoBlock(sslEngine.getSession().getPacketBufferSize());
				logger.debug(bytesAvailable+" bytes available from parent channel");
				if (bytesAvailable > 0)
				{
					ByteBuffer inputBuffer = super.getInputBuffer();
					while (inputBuffer.hasRemaining())	// 
					{
						SSLEngineResult engineResult = sslEngine.unwrap(inputBuffer, decryptedInputBuffer);
						runDelegatedTasks(engineResult);
						logger.debug("Decrypted "+decryptedInputBuffer+" engineResult = "+engineResult);
						if (engineResult.getStatus() == Status.BUFFER_UNDERFLOW)
						{
							logger.debug("SSL buffer underflow, exiting with decrypted data");
							break;
						}					
						if (engineResult.getStatus() == Status.BUFFER_OVERFLOW)
						{
							logger.debug("SSL buffer overflow, exiting with decrypted data");
							break;
						}
						if (engineResult.getStatus() == Status.CLOSED)
							throw new IOException("SSL connection closed");
					}

				}
				decryptedInputBuffer.flip();
			}
			logger.debug("Returning "+decryptedInputBuffer.remaining()+" "+super.getInputBuffer().remaining()+" bytes remaing in encrypted buffer");
			return decryptedInputBuffer.remaining();
		}
		finally
		{
			readLock.unlock();
		}
	}

	@Override
	public int readNoBlock(ByteBuffer buffer) throws IOException
	{
		if (!handshaking)
		{
			readLock.lock();
			try
			{
				// Hmmm...well, no way to do an unbuffered read
				readNoBlock();
				ByteBuffer sourceBuffer = decryptedInputBuffer;
				if (sourceBuffer.remaining() > buffer.remaining())
				{
					sourceBuffer = sourceBuffer.duplicate();
					int delta = sourceBuffer.remaining() - buffer.remaining();
					sourceBuffer.limit(sourceBuffer.limit() - delta);
					decryptedInputBuffer.position(sourceBuffer.limit());
				}
				int startPos = buffer.position();
				buffer.put(sourceBuffer);
				int bytesCopied = buffer.position() - startPos;
				return bytesCopied;
			}
			finally
			{
				readLock.unlock();
			}
		}
		else
		{
			return super.readNoBlock(buffer);
		}
	}

	@Override
	public ByteBuffer getInputBuffer()
	{
		return decryptedInputBuffer;
	}

	@Override
	public long write(ByteBuffer writeBuf) throws IOException
	{
		return write(new ByteBuffer []{writeBuf});
	}

	public static final int kSSLMaxWrite = 64 * 1024;
	private boolean	handshaking;


	@Override
	public long write(ByteBuffer[] writeBufs) throws IOException
	{
		writeLock.lock();
		try
		{
			Log4JStopWatch writeWatch = new Log4JStopWatch("SSLFirehoseChannel.write");
			encryptedBuffer.clear();
			long bytesWritten = 0;
			for (ByteBuffer writeBuffer:writeBufs)
			{
				while (writeBuffer.hasRemaining())	// There may be some SSL overhead or startup that needs to send 
					// more encrypted data than there is unencrypted data
				{
					int bytesRequested = writeBuffer.remaining();
					SSLEngineResult result = sslEngine.wrap(writeBuffer, encryptedBuffer);
					if (result.getStatus() != Status.OK)
					{
						if (result.getStatus() == Status.BUFFER_OVERFLOW)
							writeAndResetBuffer(encryptedBuffer);
						else
							throw new IOException("SSL status = "+result.getStatus()+" cannot write");
					}
					int bytesEncrypted = bytesRequested - writeBuffer.remaining();
					bytesWritten += bytesEncrypted;		// We'll send it all or die trying
					runDelegatedTasks(result);
				}
			}
			writeAndResetBuffer(encryptedBuffer);
			writeWatch.stop();
			return bytesWritten;
		}
		finally
		{
			writeLock.unlock();
		}
	}

	private void writeAndResetBuffer(ByteBuffer writeBuffer) throws IOException
	{
		writeBuffer.flip();
		while (writeBuffer.remaining() > 0)
		{
			long encryptedBytesWritten = super.write(writeBuffer);
			if (encryptedBytesWritten == 0)
			{
				// We could try putting the socket into blocking mode but we'll try this for now.
				try
				{
					Thread.sleep(10);
				} catch (InterruptedException e)
				{
					
				}
			}
		}
		writeBuffer.position(0);
		writeBuffer.limit(writeBuffer.capacity());
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
	public void close() throws IOException
	{
		writeLock.lock();
		try
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
		finally
		{
			writeLock.unlock();
		}
	}

	public Certificate [] getPeerCertificates() throws SSLPeerUnverifiedException
	{
		return sslEngine.getSession().getPeerCertificates();
	}

	public Certificate[] getLocalCertificates()
	{
		return sslEngine.getSession().getLocalCertificates();
	}

	public SSLSetup getSSLSetup()
	{
		return sslSetup;
	}
}
