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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;

import org.apache.log4j.Logger;
import org.msgpack.MessagePack;

import com.igeekinc.firehose.ReadProgress.RequestState;
import com.igeekinc.util.BitTwiddle;

/**
 * Firehose is a streaming RPC system.  All RPC's are, at the network level, asynchronous.
 * FirehoseBase contains common methods and constants for FirehoseClient and FirehoseServer
 * @author David L. Smith-Uchida
 *
 */
public abstract class FirehoseBase
{
	public static final short kCommand = 0x0001;
	public static final short kCommandReply = 0x0002;
	public static final short kCommandFailed = 0x0003;
	public static final short kUnsolicited = 0x0004;
	public static final short kClose = 0x0005;
	
	public static final int kHeaderSize = 2 + 4 + 8 + 8;	// header type (2 bytes), command code 4 bytes) command sequence (8 bytes), payload size (8 bytes)
	
	public static final int kTypeOffset = 0;
	public static final int kCommandCodeOffset = 2;
	public static final int kCommandSequenceOffset = kCommandCodeOffset + 4;
	public static final int kPayloadSizeOffset = kCommandSequenceOffset + 8;
	
	public static final int kMaxPayloadSize = 64*1024;
	
	protected MessagePack packer = new MessagePack();
	protected Logger logger = Logger.getLogger(getClass());
	protected void sendCommandAndPayload(FirehoseChannel remoteChannel, CommandMessage message,
			CommandBlock commandBlock) throws IOException
	{
		byte [] messageBuf = packer.write(message);
		ByteBuffer [] messageBufs = new ByteBuffer[2];
		byte [] header = new byte[kHeaderSize];
		BitTwiddle.shortToByteArray(kCommand, header, kTypeOffset, BitTwiddle.kBigEndian);
		BitTwiddle.intToByteArray(message.getCommandCode(), header, kCommandCodeOffset, BitTwiddle.kBigEndian);
		BitTwiddle.longToByteArray(commandBlock.getCommandSequence(), header, kCommandSequenceOffset, BitTwiddle.kBigEndian);
		BitTwiddle.longToByteArray(messageBuf.length, header, kPayloadSizeOffset, BitTwiddle.kBigEndian);
		messageBufs[0] = ByteBuffer.wrap(header);
		messageBufs[1] = ByteBuffer.wrap(messageBuf);
		synchronized(remoteChannel)
		{
			remoteChannel.write(messageBufs);
		}
	}
	
	protected void sendClose(FirehoseChannel remoteChannel, long commandSequence) throws IOException
	{
		byte [] header = new byte[kHeaderSize];
		ByteBuffer messageBuf = ByteBuffer.wrap(header);
		BitTwiddle.shortToByteArray(kClose, header, kTypeOffset, BitTwiddle.kBigEndian);
		BitTwiddle.intToByteArray(0, header, kCommandCodeOffset, BitTwiddle.kBigEndian);
		BitTwiddle.longToByteArray(commandSequence, header, kCommandSequenceOffset, BitTwiddle.kBigEndian);
		BitTwiddle.longToByteArray(0, header, kPayloadSizeOffset, BitTwiddle.kBigEndian);
		synchronized(remoteChannel)
		{
			remoteChannel.write(messageBuf);
		}
	}
	protected void sendReplyAndPayload(FirehoseChannel remoteChannel, int processedCommandCode, long commandSequence, CommandResult result) throws IOException
	{
		byte [] resultBuf;
		if (result.getResultData() != null)
			resultBuf = packer.write(result.getResultData());
		else
			resultBuf = new byte[0];
		ByteBuffer [] messageBufs = new ByteBuffer[2];
		byte [] header = new byte[kHeaderSize];
		BitTwiddle.shortToByteArray(kCommandReply, header, kTypeOffset, BitTwiddle.kBigEndian);
		BitTwiddle.intToByteArray(processedCommandCode, header, kCommandCodeOffset, BitTwiddle.kBigEndian);
		BitTwiddle.longToByteArray(commandSequence, header, kCommandSequenceOffset, BitTwiddle.kBigEndian);
		BitTwiddle.longToByteArray(resultBuf.length, header, kPayloadSizeOffset, BitTwiddle.kBigEndian);
		messageBufs[0] = ByteBuffer.wrap(header);
		messageBufs[1] = ByteBuffer.wrap(resultBuf);
		remoteChannel.write(messageBufs);
	}
	
	protected void sendErrorReply(FirehoseChannel remoteChannel, int errorCode, long commandSequence) throws IOException
	{
		byte [] header = new byte[kHeaderSize];
		BitTwiddle.shortToByteArray(kCommandFailed, header, kTypeOffset, BitTwiddle.kBigEndian);
		BitTwiddle.intToByteArray(errorCode, header, kCommandCodeOffset, BitTwiddle.kBigEndian);
		BitTwiddle.longToByteArray(commandSequence, header, kCommandSequenceOffset, BitTwiddle.kBigEndian);
		BitTwiddle.longToByteArray(0, header, kPayloadSizeOffset, BitTwiddle.kBigEndian);
		ByteBuffer headerBuf = ByteBuffer.wrap(header);
		remoteChannel.write(headerBuf);
	}
	
	protected ReceivedPayload readCommandAndPayload(FirehoseChannel socketChannel) throws IOException
	{
		ReadProgress readProgress = new ReadProgress();
		readHeaderAndPayload(readProgress, socketChannel);
		ReceivedPayload returnReceivedPayload = getReceivedPayload(readProgress);
		return returnReceivedPayload;
	}
	
	protected ReceivedPayload getReceivedPayload(ReadProgress readProgress)
			throws IOException
	{
		ByteBuffer payloadByteBuffer = readProgress.getPayloadByteBuffer();
		payloadByteBuffer.position(0);
		ReceivedPayload returnReceivedPayload = new ReceivedPayload(readProgress.getCommandType(), 
				readProgress.getCommandCode(), readProgress.getCommandSequence(), payloadByteBuffer);
		return returnReceivedPayload;
	}
	 
	protected CommandResult getReceivedResult(ReadProgress readProgress) throws IOException
	{
		
		Class<? extends Object>resultClass = getReturnClassForCommandCode(readProgress.getCommandCode());
		ByteBuffer payloadByteBuffer = readProgress.getPayloadByteBuffer();
		payloadByteBuffer.position(0);
		Object result = packer.read(payloadByteBuffer, resultClass);
		CommandResult returnCommandResult = new CommandResult(readProgress.getCommandCode(), result);
		return returnCommandResult;
	}
	
	public boolean readHeader(ReadProgress progress, ByteBuffer channelBytes) throws IOException
	{
		if (progress.getRequestState() != RequestState.kReadingHeader)
			throw new IllegalStateException();
		int bytesRead = 0;
		while(channelBytes.hasRemaining() && progress.getHeaderByteBuffer().hasRemaining())
		{
			progress.getHeaderByteBuffer().put(channelBytes.get());
			bytesRead++;
		}
		if (bytesRead >= 0)
		{
			if (progress.getHeaderByteBuffer().remaining() == 0)
			{
				byte [] curHeader = progress.getHeaderByteBuffer().array();
				progress.setCommandType(BitTwiddle.byteArrayToShort(curHeader, FirehoseBase.kTypeOffset, BitTwiddle.kBigEndian));
				progress.setCommandCode(BitTwiddle.byteArrayToInt(curHeader, FirehoseBase.kCommandCodeOffset, BitTwiddle.kBigEndian));
				progress.setCommandSequence(BitTwiddle.byteArrayToLong(curHeader, FirehoseBase.kCommandSequenceOffset, BitTwiddle.kBigEndian));
				progress.setPayloadSize(BitTwiddle.byteArrayToLong(curHeader, FirehoseBase.kPayloadSizeOffset, BitTwiddle.kBigEndian));
				
				logger.debug("Received header commandType = "+progress.getCommandType() + " command code = "+progress.getCommandCode()+
						" sequence = "+progress.getCommandSequence()+" payload size = "+progress.getPayloadSize());
				
				if (progress.getPayloadSize() > FirehoseBase.kMaxPayloadSize)
				{
					throw new IllegalArgumentException("Payload size "+progress.getPayloadSize()+" > max payload size "+FirehoseBase.kMaxPayloadSize);
				}
						
				ByteBuffer payloadByteBuffer = ByteBuffer.allocate((int)progress.getPayloadSize());
				progress.setPayloadByteBuffer(payloadByteBuffer);
				if (progress.getPayloadSize() > 0)
					progress.setRequestState(RequestState.kReadingPayload);
				else
					progress.setRequestState(RequestState.kProcessing);		// No payload to read, let's jump to processing
				return true;
			}
			else
			{
				progress.setRequestState(RequestState.kReadingHeader);
				return false;
			}
		}
		progress.setRequestState(RequestState.kError);
		throw new IOException("Socket closed unexpectedly");
	}

	public boolean readPayload(ReadProgress progress, ByteBuffer channelBytes) throws IOException
	{
		if (progress.getRequestState() != RequestState.kReadingPayload)
			throw new IllegalStateException();
		if (channelBytes.hasRemaining())
		{
			ByteBuffer sourceBuffer = channelBytes.duplicate();
			ByteBuffer payloadByteBuffer = progress.getPayloadByteBuffer();
			int maxTransfer = Math.min(channelBytes.remaining(), payloadByteBuffer.remaining());
			sourceBuffer.limit(sourceBuffer.position() + maxTransfer);
			payloadByteBuffer.put(sourceBuffer);
			channelBytes.position(sourceBuffer.position());	// Change the position of the channel bytes buffer
			if (payloadByteBuffer.remaining() > 0)
			{
				return false;
			}
			else
			{
				progress.setRequestState(RequestState.kProcessing);
				return true;
			}
		}
		throw new IOException("Socket closed unexpectedly");
	}
	
	/**
	 * This will read information from the channel and advance through the header and payload.
	 * If called on a non-blocking channel, this will return immediately, even if progress was not made.
	 * 
	 * @param channelToRead
	 * @return true when header and payload have been completely read
	 * @throws IOException
	 */
	public void processInput(ReadProgress progress, FirehoseChannel serverChannel) throws IOException
	{
		serverChannel.readNoBlock();
		ByteBuffer inputBuffer = serverChannel.getInputBuffer();
		while (inputBuffer.hasRemaining() && progress.getRequestState() != RequestState.kProcessing)
		{
			switch(progress.getRequestState())
			{
			case kReadingHeader:
				readHeader(progress, inputBuffer);
				break;
			case kReadingPayload:
				readPayload(progress, inputBuffer);
				break;
			case kProcessing:
				break;
			case kError:
				throw new IOException("Error occurred");
			}
		}
	}
	
	/**
	 * This will read the header and payload from a channel.  channelToRead must be in non-blocking mode
	 * or an IllegalBlockingModeException will be thrown.  readHeaderAndPayload, however, will block waiting for
	 * input
	 * @param channelToRead
	 * @throws IOException
	 */
	public void readHeaderAndPayload(ReadProgress progress, FirehoseChannel remoteChannel) throws IOException
	{
		Selector selector = remoteChannel.openSelector();
		remoteChannel.register(selector, SelectionKey.OP_READ);
		while(progress.getRequestState() != RequestState.kProcessing)
		{
			if (selector.select() > 0)
			{
				selector.selectedKeys().clear();
				processInput(progress, remoteChannel);
			}
		}
		selector.close();
	}
	
	protected abstract Class<? extends CommandMessage> getClassForCommandCode(int payloadType);
	protected abstract Class<? extends Object> getReturnClassForCommandCode(int payloadType);
	
	public static final int kIOError = 1;
	public static final int kInvalidKeyException = 2;
	public static final int kCertificateException = 3;
	public static final int kNoSuchAlgorithmException = 4;
	public static final int kNoSuchProviderException = 5;
	public static final int kSignatureException = 6;
	public static final int kKeyStoreException = 7;
	public static final int kCertificateEncodingException = 8;
	public static final int kIllegalStateException = 9;
	public static final int kUnrecoverableKeyException = 10;
	public static final int kCertificateParsingException = 11;
	
	public static final int kUserDefinedErrorStart = 1000;
	
	public int getErrorCodeForThrowable(Throwable t)
	{
		return getBaseErrorCodeForThrowable(t);
	}
	
	public int getBaseErrorCodeForThrowable(Throwable t)
	{
		Class<? extends Throwable> throwableClass = t.getClass();
		if (throwableClass.equals(IOException.class))
			return kIOError;
		if (throwableClass.equals(InvalidKeyException.class))
			return kInvalidKeyException;
		if (throwableClass.equals(CertificateException.class))
			return kCertificateException;
		if (throwableClass.equals(NoSuchAlgorithmException.class))
			return kNoSuchAlgorithmException;
		if (throwableClass.equals(NoSuchProviderException.class))
			return kNoSuchProviderException;
		if (throwableClass.equals(SignatureException.class))
			return kSignatureException;
		if (throwableClass.equals(KeyStoreException.class))
			return kKeyStoreException;
		if (throwableClass.equals(CertificateEncodingException.class))
			return kCertificateEncodingException;
		if (throwableClass.equals(CertificateEncodingException.class))
			return kCertificateEncodingException;
		if (throwableClass.equals(IllegalStateException.class))
			return kIllegalStateException;
		if (throwableClass.equals(UnrecoverableKeyException.class))
			return kUnrecoverableKeyException;
		if (throwableClass.equals(CertificateParsingException.class))
			return kCertificateParsingException;
		return -1;
	}
	
	public Throwable getThrowableForErrorCode(int errorCode)
	{
		Throwable t = getBaseThrowableForErrorCode(errorCode);
		if (t == null)
			t = new InternalError();
		return t;
	}
	
	public Throwable getBaseThrowableForErrorCode(int errorCode)
	{
		switch(errorCode)
		{
		case kIOError:
			return new IOException();
		case kInvalidKeyException:
			return new InvalidKeyException();
		case kCertificateException:
			return new CertificateException();
		case kNoSuchAlgorithmException:
			return new NoSuchAlgorithmException();
		case kNoSuchProviderException:
			return new NoSuchProviderException();
		case kSignatureException:
			return new SignatureException();
		case kKeyStoreException:
			return new KeyStoreException();
		case kCertificateEncodingException:
			return new CertificateEncodingException();
		case kIllegalStateException:
			return new IllegalStateException();
		case kUnrecoverableKeyException:
			return new UnrecoverableKeyException();
		case kCertificateParsingException:
			return new CertificateParsingException();
		}
		return null;
	}
}
