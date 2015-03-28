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
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.msgpack.MessagePack;
import org.perf4j.log4j.Log4JStopWatch;

import com.igeekinc.firehose.ReadProgress.RequestState;
import com.igeekinc.util.BitTwiddle;
import com.igeekinc.util.datadescriptor.DataDescriptor;
import com.igeekinc.util.logging.DebugLogMessage;

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
	
	public static final int kHeaderSize = 2 /* header type (2 bytes) */ + 4 /* command code (4 bytes) */+ 
			8 /* command sequence (8 bytes) */ + 8 /* payload size (8 bytes) */ + 8 /* bulk data size (8 bytes) */;
	
	public static final int kTypeOffset = 0;
	public static final int kCommandCodeOffset = 2;
	public static final int kCommandSequenceOffset = kCommandCodeOffset + 4;
	public static final int kPayloadSizeOffset = kCommandSequenceOffset + 8;
	public static final int kBulkDataSizeOffset = kPayloadSizeOffset + 8;
	
	public static final int kMaxPayloadSize = 10*1024*1024;
	
	protected MessagePack packer = new MessagePack();
	protected Logger logger = Logger.getLogger(getClass());
	private ReentrantLock readLock = new ReentrantLock();
	private ReentrantLock writeLock = new ReentrantLock();
	
	protected void sendCommandAndPayload(FirehoseChannel remoteChannel, CommandMessage message,
			CommandBlock<?> commandBlock) throws IOException
	{
		byte [] messageBuf = packer.write(message);
		ByteBuffer [] messageBufs = new ByteBuffer[2];
		byte [] header = new byte[kHeaderSize];
		BitTwiddle.shortToByteArray(kCommand, header, kTypeOffset, BitTwiddle.kBigEndian);
		BitTwiddle.intToByteArray(message.getCommandCode(), header, kCommandCodeOffset, BitTwiddle.kBigEndian);
		BitTwiddle.longToByteArray(commandBlock.getCommandSequence(), header, kCommandSequenceOffset, BitTwiddle.kBigEndian);
		BitTwiddle.longToByteArray(messageBuf.length, header, kPayloadSizeOffset, BitTwiddle.kBigEndian);
		BitTwiddle.longToByteArray(0, header, kBulkDataSizeOffset, BitTwiddle.kBigEndian);
		messageBufs[0] = ByteBuffer.wrap(header);
		messageBufs[1] = ByteBuffer.wrap(messageBuf);
		checkAndLockWriteLock();
		try
		{
			while (messageBufs[0].hasRemaining() || messageBufs[1].hasRemaining())
			{
				remoteChannel.write(messageBufs);
			}
		}
		finally
		{
			writeLock.unlock();
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
		checkAndLockWriteLock();
		try
		{
			while (messageBuf.hasRemaining())
				remoteChannel.write(messageBuf);
		}
		finally
		{
			writeLock.unlock();
		}
	}

    public static final int kCopyBufSize = 16*1024;

    private void checkAndLockWriteLock()
    {
    	if (writeLock.isLocked() && !writeLock.isHeldByCurrentThread())
    		logger.debug(new DebugLogMessage("write lock in use!"));
    	writeLock.lock();
    }
    
    private void checkAndLockReadLock()
    {
    	if (readLock.isLocked() && !readLock.isHeldByCurrentThread())
    		logger.debug(new DebugLogMessage("read lock in use!"));
    	readLock.lock();
    }
    
	protected void sendReplyAndPayload(FirehoseChannel remoteChannel, int processedCommandCode, long commandSequence, 
			CommandResult result) throws IOException
	{
		checkAndLockWriteLock();
		try
		{
			logger.debug("Sending reply for command sequence "+commandSequence);
			byte [] resultBuf;
			if (result.getResultData() != null)
				resultBuf = packer.write(result.getResultData());
			else
				resultBuf = new byte[0];
			DataDescriptor bulkData = result.getBulkData();
			ByteBuffer [] messageBufs = new ByteBuffer[2];

			byte [] header = new byte[kHeaderSize];
			BitTwiddle.shortToByteArray(kCommandReply, header, kTypeOffset, BitTwiddle.kBigEndian);
			BitTwiddle.intToByteArray(processedCommandCode, header, kCommandCodeOffset, BitTwiddle.kBigEndian);
			BitTwiddle.longToByteArray(commandSequence, header, kCommandSequenceOffset, BitTwiddle.kBigEndian);
			BitTwiddle.longToByteArray(resultBuf.length, header, kPayloadSizeOffset, BitTwiddle.kBigEndian);
			BitTwiddle.longToByteArray(result.getBulkDataLength(), header, kBulkDataSizeOffset, BitTwiddle.kBigEndian);
			messageBufs[0] = ByteBuffer.wrap(header);
			messageBufs[1] = ByteBuffer.wrap(resultBuf);
			long bytesToWrite = messageBufs[0].remaining() + messageBufs[1].remaining();
			while (bytesToWrite > 0)
			{
				long bytesWritten = remoteChannel.write(messageBufs);
				bytesToWrite -= bytesWritten;
				if (bytesWritten == 0)
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
			if (result.getBulkDataLength() > 0)
			{
				ByteBuffer copyBuf = ByteBuffer.allocate(kCopyBufSize);
				long bytesRemaining = result.getBulkDataLength();
				long srcOffset = result.getBulkDataOffset();
				while (bytesRemaining > 0)
				{
					copyBuf.clear();
					int curBytesToCopy;
					if (bytesRemaining > kCopyBufSize)
						curBytesToCopy = kCopyBufSize;
					else
						curBytesToCopy = (int)bytesRemaining;
					boolean release = false;
					if (curBytesToCopy == bytesRemaining)
						release = true;
					Log4JStopWatch requestSendDataWatch = new Log4JStopWatch("DataMoverSource.serverLoop.data");
					bulkData.getData(copyBuf, srcOffset, curBytesToCopy, release);
					copyBuf.flip();		// God I hate the ByteBuffer interface
					long bytesWritten = writeFully(remoteChannel, copyBuf);
					requestSendDataWatch.stop();
					srcOffset += bytesWritten;
					bytesRemaining -= bytesWritten;
				}
			}
		}
		finally
		{
			writeLock.unlock();
		}
	}

	protected long writeFully(FirehoseChannel remoteChannel, ByteBuffer [] copyBufs) throws IOException
	{
		long bytesWritten = 0;
		while (hasRemaining(copyBufs))
		{
			long bytesWrittenThisPass = remoteChannel.write(copyBufs);
			if (bytesWrittenThisPass == 0)
			{
				// We could try putting the socket into blocking mode but we'll try this for now.
				try
				{
					Thread.sleep(10);
				} catch (InterruptedException e)
				{

				}
			}
			if (hasRemaining(copyBufs))
				logger.debug(new DebugLogMessage("Didn't write all complete buffer"));
			bytesWritten += bytesWrittenThisPass;
		}
		return bytesWritten;
	}
	
	private boolean hasRemaining(ByteBuffer [] checkBufs)
	{
		for (ByteBuffer checkBuf:checkBufs)
		{
			if (checkBuf.hasRemaining())
				return true;
		}
		return false;
	}
	
	protected long writeFully(FirehoseChannel remoteChannel, ByteBuffer copyBuf) throws IOException
	{
		long bytesWritten = 0;
		while (copyBuf.hasRemaining())
		{
			long bytesWrittenThisPass = remoteChannel.write(copyBuf);
			if (bytesWrittenThisPass == 0)
			{
				// We could try putting the socket into blocking mode but we'll try this for now.
				try
				{
					Thread.sleep(10);
				} catch (InterruptedException e)
				{

				}
			}
			if (copyBuf.hasRemaining())
				logger.debug(new DebugLogMessage("Didn't write all complete buffer"));
			bytesWritten += bytesWrittenThisPass;
		}
		return bytesWritten;
	}
	
	protected void sendErrorReply(FirehoseChannel remoteChannel, int errorCode, long commandSequence) throws IOException
	{
		checkAndLockWriteLock();
		try
		{
			byte [] header = new byte[kHeaderSize];
			BitTwiddle.shortToByteArray(kCommandFailed, header, kTypeOffset, BitTwiddle.kBigEndian);
			BitTwiddle.intToByteArray(errorCode, header, kCommandCodeOffset, BitTwiddle.kBigEndian);
			BitTwiddle.longToByteArray(commandSequence, header, kCommandSequenceOffset, BitTwiddle.kBigEndian);
			BitTwiddle.longToByteArray(0, header, kPayloadSizeOffset, BitTwiddle.kBigEndian);
			ByteBuffer headerBuf = ByteBuffer.wrap(header);
			writeFully(remoteChannel, headerBuf);
		}
		finally
		{
			writeLock.unlock();
		}
	}
	
	protected ReceivedPayload readCommandAndPayload(FirehoseChannel remoteChannel, HashMap<Long, CommandBlock<?>> sequenceToCommandBlockMap) throws IOException
	{
		ReadProgress readProgress = new ReadProgress();
		readHeaderAndPayload(readProgress, remoteChannel, sequenceToCommandBlockMap);
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
	
	protected boolean readHeader(ReadProgress progress, ByteBuffer channelBytes) throws IOException
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
				progress.setBulkDataSize(BitTwiddle.byteArrayToLong(curHeader, FirehoseBase.kBulkDataSizeOffset, BitTwiddle.kBigEndian));
				if (logger.isDebugEnabled())
				{
					logger.debug("Received header commandType = "+progress.getCommandType() + " command code = "+progress.getCommandCode()+
							" sequence = "+progress.getCommandSequence()+" payload size = "+progress.getPayloadSize()+" bulkData size = "+progress.getBulkDataSize());
				}
				if (progress.getPayloadSize() > FirehoseBase.kMaxPayloadSize || progress.getPayloadSize() < 0)
				{
					throw new IllegalArgumentException("Payload size "+progress.getPayloadSize()+" > max payload size "+FirehoseBase.kMaxPayloadSize);
				}
						
				ByteBuffer payloadByteBuffer = ByteBuffer.allocate((int)progress.getPayloadSize());
				progress.setPayloadByteBuffer(payloadByteBuffer);
				if (progress.getPayloadSize() > 0)
					progress.setRequestState(RequestState.kReadingPayload);
				else
				{
					// No payload to read, so we'll skip the reading payload stage and go direct
					if (progress.getBulkDataSize() > 0)
						progress.setRequestState(RequestState.kReadingBulkData);
					else
						progress.setRequestState(RequestState.kProcessing);		
				}
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

	protected boolean readPayload(ReadProgress progress, ByteBuffer channelBytes) throws IOException
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
				if (progress.getBulkDataSize() == 0)
				{
					progress.setRequestState(RequestState.kProcessing);
				}
				else
				{					
					progress.setRequestState(RequestState.kReadingBulkData);
				}
				return true;
			}
		}
		throw new IOException("Socket closed unexpectedly");
	}
	
	/**
	 * This will read information from the channel and advance through the header and payload.
	 * If called on a non-blocking channel, this will return immediately, even if progress was not made.
	 * @param sequenceToCommandBlockMap 
	 * 
	 * @param channelToRead
	 * @return true when header and payload have been completely read
	 * @throws IOException
	 */
	protected void processInput(ReadProgress progress, FirehoseChannel serverChannel, HashMap<Long, CommandBlock<?>> sequenceToCommandBlockMap) throws IOException
	{
		checkAndLockReadLock();
		try
		{
			if (progress.getRequestState() != RequestState.kReadingBulkData)
			{
				serverChannel.readNoBlock();
				ByteBuffer inputBuffer = serverChannel.getInputBuffer();

				doneWithHeaderAndPayload:
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
							break doneWithHeaderAndPayload;
						case kError:
							throw new IOException("Error occurred");
						case kReadingBulkData:
							break doneWithHeaderAndPayload;		// We'll come back later
						}
					}
			}

			if (progress.getRequestState() == RequestState.kReadingBulkData)
			{
				if (progress.getBulkDataByteBuffer() == null)
				{
					ByteBuffer bulkDataByteBuffer;
					if (sequenceToCommandBlockMap != null)
					{
						CommandBlock<?> commandBlock = sequenceToCommandBlockMap.get(progress.getCommandSequence());
						if (commandBlock == null)
							throw new IOException("Could not get command block for command sequence "+progress.getCommandSequence());
						bulkDataByteBuffer = commandBlock.getBulkData();
						if (bulkDataByteBuffer == null)
							throw new IOException("Could not get bulk data buffer for command sequence "+progress.getCommandSequence());
						if (bulkDataByteBuffer.remaining() < progress.getBulkDataSize())
							throw new IOException("Received "+progress.getBulkDataSize()+" bytes bulk data, expecting "+bulkDataByteBuffer.remaining()+" bytes");
						if (bulkDataByteBuffer.remaining() > progress.getBulkDataSize())
						{
							long delta = bulkDataByteBuffer.remaining() - progress.getBulkDataSize();
							bulkDataByteBuffer.limit(bulkDataByteBuffer.limit() - (int)delta);
						}
					}
					else
					{
						bulkDataByteBuffer = ByteBuffer.allocate((int)progress.getBulkDataSize());
					}
					progress.setBulkDataByteBuffer(bulkDataByteBuffer);
				}
				logger.debug("Reading bulk data");
				int bytesRead = serverChannel.readNoBlock(progress.getBulkDataByteBuffer());
				logger.debug("Read "+bytesRead+" of bulk data");
				if (progress.getBulkDataByteBuffer().remaining() == 0)
					progress.setRequestState(RequestState.kProcessing);
			}
		}
		finally
		{
			readLock.unlock();
		}
	}
	
	/**
	 * This will read the header and payload from a channel.  channelToRead must be in non-blocking mode
	 * or an IllegalBlockingModeException will be thrown.  readHeaderAndPayload, however, will block waiting for
	 * input
	 * @param sequenceToCommandBlockMap 
	 * @param channelToRead
	 * @throws IOException
	 */
	protected void readHeaderAndPayload(ReadProgress progress, FirehoseChannel remoteChannel, HashMap<Long, CommandBlock<?>> sequenceToCommandBlockMap) throws IOException
	{
		checkAndLockReadLock();
		try
		{
			boolean dataWaiting = remoteChannel.getInputBuffer().hasRemaining();
			Selector selector = remoteChannel.openSelector();
			remoteChannel.register(selector, SelectionKey.OP_READ, null);
			while(progress.getRequestState() != RequestState.kProcessing)
			{
				int numSelected = 0;
				//logger.debug("Entering select");
				if (dataWaiting || (numSelected = selector.select(1000)) >= 0)
				{
					//logger.debug("Processing input");
					/*if (!dataWaiting && numSelected <= 0)
						logger.debug("Timed out");*/
					selector.selectedKeys().clear();
					processInput(progress, remoteChannel, sequenceToCommandBlockMap);
					dataWaiting = remoteChannel.getInputBuffer().hasRemaining();
					/*if (dataWaiting)
						logger.debug("More data to process");*/
				}
			}
			selector.close();
		}
		finally
		{
			readLock.unlock();
		}
	}
	
	protected abstract Class<? extends Object> getReturnClassForCommandCode(int commandCode);
	
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
	public static final int kIllegalArgumentException = 12;
	public static final int kArrayIndexOutOfBoundsException = 13;
	
	public static final int kExtendedErrorStart = 1000;
	
	public int getErrorCodeForThrowable(Throwable t)
	{
		int errorCode = getBaseErrorCodeForThrowable(t);
		if (errorCode < 0)
			errorCode = getExtendedErrorCodeForThrowable(t);
		if (errorCode < 0)
			logger.error("Could not find error code for "+t.getMessage(), t);
		return errorCode;
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
		if (throwableClass.equals(IllegalArgumentException.class))
			return kIllegalArgumentException;
		if (throwableClass.equals(ArrayIndexOutOfBoundsException.class))
			return kArrayIndexOutOfBoundsException;
		return -1;
	}
	
	/**
	 * Override this to extend the exceptions that can be sent.  Extended error codes start at 1000
	 * @param t
	 * @return
	 */
	public int getExtendedErrorCodeForThrowable(Throwable t)
	{
		return -1;
	}
	public Throwable getThrowableForErrorCode(int errorCode)
	{
		Throwable t = getBaseThrowableForErrorCode(errorCode);
		if (t == null)
		{
			t= getExtendedThrowableForErrorCode(errorCode);
			if (t == null)
				t = new InternalError();
		}
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
		case kIllegalArgumentException:
			return new IllegalArgumentException();
		case kArrayIndexOutOfBoundsException:
			return new ArrayIndexOutOfBoundsException();
		}
		return null;
	}
	
	/**
	 * Override this to extend the exceptions that can be sent.  Extended error codes start at 1000
	 * @param errorCode
	 * @return
	 */
	public Throwable getExtendedThrowableForErrorCode(int errorCode)
	{
		return null;
	}
}
