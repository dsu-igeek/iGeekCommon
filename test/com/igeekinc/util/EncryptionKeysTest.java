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
 * Created on Nov 22, 2003
 *
 * Copyright (C) 2003 iGeek, Inc.
 * All Rights Reserved
 */
package com.igeekinc.util;

import java.io.IOException;
import java.io.OptionalDataException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.PBEKeySpec;

import com.igeekinc.junitext.iGeekTestCase;

public class EncryptionKeysTest extends iGeekTestCase
{
    private static final String kBouncyCastleProviderName = "BC";
    private static final String kBouncyCastleRSACipherSig = "RSA/ECB/PKCS1Padding";
    
	String passphrase;
	EncryptionKeys testObject;
	PBEKeySpec passphraseSpec;
	byte [] plainText, encryptedText, decryptedText;
	/**
	 * Constructor for EncryptionKeysTest.
	 * @param arg0
	 */
	public EncryptionKeysTest(String arg0) throws InvalidKeyException, InvalidKeySpecException, IllegalStateException, IllegalBlockSizeException, BadPaddingException, IOException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException
	{
		super(arg0);

		passphrase = "howdydoody";
		passphraseSpec = new PBEKeySpec(passphrase.toCharArray());

	}

	/*
	 * @see TestCase#setUp()
	 */
    protected void setUp() throws Exception 
    {
		super.setUp();
		//int cryptixPos = Security.insertProviderAt(new cryptix.jce.provider.CryptixCrypto(), 2);
		Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 2);
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}

	/*
	 * Test for EncryptedPrivateKeyInfo getDecryptionKey()
	 */
	public void testGetDecryptionKey() throws InvalidKeyException, OptionalDataException, InvalidKeySpecException, IllegalStateException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException, IOException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException
	{
		testObject = new EncryptionKeys(passphraseSpec, true);
		PublicKey encryptKey = testObject.getEncryptionKey();
		Cipher rsa = Cipher.getInstance(kBouncyCastleRSACipherSig, kBouncyCastleProviderName);

		plainText = new byte[64];
		for (int curByteNum = 0; curByteNum < plainText.length; curByteNum++)
		{
			plainText[curByteNum] = (byte)curByteNum;
		}
		rsa.init(Cipher.ENCRYPT_MODE, encryptKey);
		int blockSize = rsa.getBlockSize();

		encryptedText = rsa.doFinal(plainText);
		System.out.println("Text encrypted successfully");
		PrivateKey decryptKey = testObject.decryptDecryptionKey(passphraseSpec);
		rsa.init(Cipher.DECRYPT_MODE, decryptKey);
		decryptedText = rsa.doFinal(encryptedText);
		assertEquals(decryptedText.length, 64);
		for (int curByteNum = 0; curByteNum < decryptedText.length; curByteNum++)
		{
			assertEquals(plainText[curByteNum], decryptedText[curByteNum]);
		}
	}

	/*
	 * Test for PrivateKey getDecryptionKey(PBEKeySpec)
	 */
	public void testGetDecryptionKeyPBEKeySpec()
	{
	}

}
