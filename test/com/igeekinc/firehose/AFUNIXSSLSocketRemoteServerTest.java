/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * @Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.@
 */
 
package com.igeekinc.firehose;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.ServerSocketChannel;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Future;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.newsclub.net.unix.AFUNIXServerSocketChannelImpl;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.perf4j.log4j.Log4JStopWatch;

import com.igeekinc.junitext.iGeekTestCase;
import com.igeekinc.util.async.AsyncCompletion;
import com.igeekinc.util.logging.ErrorLogMessage;

public class AFUNIXSSLSocketRemoteServerTest extends iGeekTestCase
{
	TestRemoteServer server;
	static KeyPair keyPair;
	static KeyStore tempKeyStore;
	static KeyManagerFactory keyManagerFactory;
	static TrustManagerFactory trustFactory;
    public static final String kCertificateSignatureAlg = "SHA1withRSA";
    private static final String kDefaultKeyStorePassword = "idb301$";
	static
	{
		try
		{
			Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 2);
			
			KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");

			kpGen.initialize(1024, new SecureRandom());
			keyPair = kpGen.generateKeyPair();
			tempKeyStore = KeyStore.getInstance("JKS");
			tempKeyStore.load(null, null);
			Date startDate = new Date();              // time from which certificate is valid
	        Date expiryDate = new Date(startDate.getTime() + (10L * 365L * 24L * 60L * 60L * 1000L));             // time after which certificate is not valid
	        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());     // serial number for certificate
	        X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
	        X500Principal              dnName = new X500Principal("CN=Indelible FS Auto-generated Root, UID=test");

	        certGen.setSerialNumber(serialNumber);
	        certGen.setIssuerDN(dnName);
	        certGen.setNotBefore(startDate);
	        certGen.setNotAfter(expiryDate);
	        certGen.setSubjectDN(dnName);                       // note: same as issuer
	        certGen.setPublicKey(keyPair.getPublic());
	        certGen.setSignatureAlgorithm(kCertificateSignatureAlg);

	        X509Certificate cert = certGen.generate(keyPair.getPrivate(), "BC");
	        
	        tempKeyStore.setCertificateEntry("Certificate", cert);
	        tempKeyStore.setKeyEntry("CertificatePrivateKey", keyPair.getPrivate(), kDefaultKeyStorePassword.toCharArray(), new Certificate [] {cert});

	        keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(tempKeyStore, kDefaultKeyStorePassword.toCharArray());
            
            trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustFactory.init(tempKeyStore);
		} catch (InvalidKeyException e)
		{
			Logger.getLogger(SSLRemoteServerTest.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (KeyStoreException e)
		{
			Logger.getLogger(SSLRemoteServerTest.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (NoSuchAlgorithmException e)
		{
			Logger.getLogger(SSLRemoteServerTest.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (NoSuchProviderException e)
		{
			Logger.getLogger(SSLRemoteServerTest.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (SignatureException e)
		{
			Logger.getLogger(SSLRemoteServerTest.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (IllegalStateException e)
		{
			Logger.getLogger(SSLRemoteServerTest.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (CertificateException e)
		{
			Logger.getLogger(SSLRemoteServerTest.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (UnrecoverableKeyException e)
		{
			Logger.getLogger(SSLRemoteServerTest.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (IOException e)
		{
			Logger.getLogger(SSLRemoteServerTest.class).error(new ErrorLogMessage("Caught exception"), e);
		}
	}
	
	@Override
	public Level getLoggingLevel()
	{
		return Level.INFO;
	}

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		ServerSocketChannel serverChannel = AFUNIXServerSocketChannelImpl.open(getConnectAddress());
		server = new TestRemoteServer(serverChannel, getServerSSLContext());
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
		server.shutdown();
	}
	public AFUNIXSocketAddress getConnectAddress() throws IOException
	{
		AFUNIXSocketAddress returnAddress = new AFUNIXSocketAddress(new File("/tmp/af-firehose-test"));
		return returnAddress;
	}
	
	public SSLEngine getClientSSLEngine() throws NoSuchAlgorithmException, KeyManagementException
	{
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, trustFactory.getTrustManagers(), new SecureRandom());
		SSLEngine returnEngine = sslContext.createSSLEngine();
		returnEngine.setUseClientMode(true);
		return returnEngine;
	}
	
	public SSLContext getServerSSLContext() throws NoSuchAlgorithmException, KeyManagementException
	{
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(keyManagerFactory.getKeyManagers(), trustFactory.getTrustManagers(), new SecureRandom());
		return sslContext;
	}
	
	public void testBasic()
	throws Exception
	{
		TestRemoteClient client = new TestRemoteClient(getConnectAddress(), getClientSSLEngine());
		assertEquals(3, client.add(1, 2));
		Future<Void>sleepFuture = client.sleep(10);
		sleepFuture.get();
		client.close();
	}
	
	public static final int kNumRepeatRuns = 10000;
	public void testRepeated() throws Exception
	{
		TestRemoteClient client = new TestRemoteClient(getConnectAddress(), getClientSSLEngine());
		Log4JStopWatch stopWatch = new Log4JStopWatch("testRepeated");
		for (int curRunNum = 0; curRunNum < kNumRepeatRuns; curRunNum++)
		{
			assertEquals(curRunNum + (curRunNum * 2), client.add(curRunNum, curRunNum * 2));
		}
		stopWatch.stop();
		System.out.println(kNumRepeatRuns + " runs in "+stopWatch.getElapsedTime()+" ms "+
				((double)kNumRepeatRuns/(double)stopWatch.getElapsedTime())+" runs/ms");
		client.close();
	}
	
	class SleepCompletionMonitor implements AsyncCompletion<Void, Integer>
	{
		private ArrayList<Integer>completionList = new ArrayList<Integer>();
		
		@Override
		public synchronized void completed(Void result, Integer attachment)
		{
			completionList.add(attachment);
			notifyAll();
		}

		@Override
		public void failed(Throwable exc, Integer attachment)
		{
			// TODO Auto-generated method stub
			
		}
		
		public synchronized Integer [] getCompletion()
		{
			return completionList.toArray(new Integer[completionList.size()]);
		}
		
		public synchronized void waitForCompletions(int numExpected) throws InterruptedException
		{
			while(completionList.size() < numExpected)
				wait();
		}
	}
	public void testOutOfOrderCompletion() throws Exception
	{
		SleepCompletionMonitor monitor = new SleepCompletionMonitor();
		TestRemoteClient client = new TestRemoteClient(getConnectAddress(), getClientSSLEngine());
		client.sleep(10000, monitor, 2);
		client.sleep(5000, monitor, 1);
		client.sleep(1000, monitor, 0);
		client.sleep(15000, monitor, 3);
		monitor.waitForCompletions(4);
		Integer [] completionList = monitor.getCompletion();
		for (int completionNum = 0; completionNum < completionList.length; completionNum++)
			assertEquals(completionNum, (int)completionList[completionNum]);
		client.close();
	}
	
	public void testError() throws Exception
	{
		TestRemoteClient client = new TestRemoteClient(getConnectAddress(), getClientSSLEngine());
		boolean caught = false;
		try
		{
			client.failWithIOError(42);
		}
		catch (IOException e)
		{
			caught = true;
		}
		assertTrue(caught);
	}
}
