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
 
/*
 * Created on Nov 25, 2003
 *
 * Copyright (C) 2003 iGeek, Inc.
 * All Rights Reserved
 */
package com.igeekinc.util;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.Security;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.igeekinc.junitext.iGeekTestCase;

public class EncryptedFileFormatTest extends iGeekTestCase
{
	String passphrase;
	PBEKeySpec passphraseSpec;
	
	/**
	 * Constructor for EncryptedFileFormatTest.
	 * @param arg0
	 */
	public EncryptedFileFormatTest(String arg0)
	{
		super(arg0);
		//int cryptixPos = Security.insertProviderAt(new cryptix.jce.provider.CryptixCrypto(), 2);
        Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 2);
		passphrase = "abcd";
		passphraseSpec = new PBEKeySpec(passphrase.toCharArray());
	}
	public void testA() throws Exception
	{
		KeyGenerator kgen = KeyGenerator.getInstance("Rijndael");
		kgen.init(128); // 192 and 256 bits may not be available

		// Generate the secret key specs.
		SecretKey skey = kgen.generateKey();
		byte[] raw = skey.getEncoded();

		SecretKeySpec sessionKey = new SecretKeySpec(raw, "Rijndael");
		EncryptionKeys testKeys = new EncryptionKeys(passphraseSpec, false);

		for (int numBytesToWrite = 1; numBytesToWrite < 2048; numBytesToWrite+=27)
		{	
			File testFile = File.createTempFile("encrypt-test", ".enc");
			logger.warn("Test file = "+testFile.getAbsolutePath());
			EncryptedFileFormat testFormat = new EncryptedFileFormat(testFile);
			
			testFormat.setEncryptionKeys(testKeys.getEncryptionKey(), sessionKey);
			OutputStream encryptedOutStream = testFormat.getOutputStream();
			
			/*
			for (int out = 0; out < numBytesToWrite; out++)
			{
				encryptedOutStream.write(out);
			}
			*/
			byte [] outBuf = new byte[numBytesToWrite];
			for (int out = 0; out < numBytesToWrite; out++)
			{
				outBuf[out] = (byte)out;
			}
			encryptedOutStream.write(outBuf);
			encryptedOutStream.flush();
			encryptedOutStream.close();
			//PrintStream printStream = new PrintStream(encryptedOutStream);
			//printStream.println("Now is the time for all good men to come to the aid of their country");
			//printStream.close();
			PBEKeySpec testKeySpec = new PBEKeySpec(passphrase.toCharArray());
			PrivateKey decryptKey = testKeys.decryptDecryptionKey(testKeySpec);
			logger.warn("decryptKey = "+decryptKey);
            logger.warn(testFile.getPath()+" size = "+testFile.length());
			testFormat = new EncryptedFileFormat(testFile);
			InputStream decryptedInputStream = testFormat.getInputStream(decryptKey);
			//BufferedReader readStream = new BufferedReader(new InputStreamReader(decryptedInputStream));
			//String curLine;
			//while((curLine = readStream.readLine())!= null)
			//logger.warn(curLine);
			byte [] checkBuf = new byte[numBytesToWrite];
            int totalBytesRead = 0;
            int bytesRead;
			while((bytesRead = decryptedInputStream.read(checkBuf)) > 0)
                totalBytesRead += bytesRead;
			if (numBytesToWrite !=  totalBytesRead)
			{
				logger.warn("Wrote "+numBytesToWrite+", read back "+totalBytesRead);
			}
			try
            {
                decryptedInputStream.close();
            } catch (RuntimeException e)
            {
                e.printStackTrace();
            }
			testFile.delete();
		}
	}
	
	/*

	public void testB() throws Exception
	{
	    String dataDirName = System.getProperty("com.igeekinc.tests.testdata");
        assertNotNull(dataDirName);
	    File dataDir = new File(dataDirName);
        assertTrue(dataDir.exists());
		File keysFile = new File(dataDirName, "EncryptedFileFormatTestData/.indelible-info/indelible-keys");
		ObjectInputStream keyObjectStream = new ObjectInputStream(new FileInputStream(keysFile));
		EncryptionKeys keys = (EncryptionKeys)keyObjectStream.readObject();
		PBEKeySpec testKeySpec = new PBEKeySpec("abcd".toCharArray());
		PrivateKey decryptKey = keys.decryptDecryptionKey(testKeySpec);
		File testFile = new File(dataDirName, "EncryptedFileFormatTest/CarbonLib_1.6GM_SDK.img.as.enc");
		
		EncryptedFileFormat testFormat = new EncryptedFileFormat(testFile);
		InputStream decryptedInputStream = testFormat.getInputStream(decryptKey);
		
	}
*/
	public void testC() throws Exception
	{
		EncryptionKeys testKeys = new EncryptionKeys(passphraseSpec, false);

	}
}
