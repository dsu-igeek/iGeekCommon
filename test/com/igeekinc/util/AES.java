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
 * Created on Nov 23, 2003
 *
 * Copyright (C) 2003 iGeek, Inc.
 * All Rights Reserved
 */
package com.igeekinc.util;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Iterator;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.igeekinc.junitext.iGeekTestCase;

/**
* This program generates a AES key, retrieves its raw bytes, and
* then reinstantiates a AES key from the key bytes.
* The reinstantiated key is used to initialize a AES cipher for
* encryption and decryption.
*/

public class AES extends iGeekTestCase
{

	public AES()
	{
		/*int cryptixPos =
			Security.insertProviderAt(
				new cryptix.jce.provider.CryptixCrypto(),
				2);
        logger.warn("CryptixPos = "+cryptixPos);*/
	}
	/**
	* Turns array of bytes into string
	*
	* @param buf	Array of bytes to convert to hex string
	* @return	Generated hex string
	*/
	public static String asHex(byte buf[])
	{
		StringBuffer strbuf = new StringBuffer(buf.length * 2);
		int i;

		for (i = 0; i < buf.length; i++)
		{
			if (((int) buf[i] & 0xff) < 0x10)
				strbuf.append("0");

			strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
		}

		return strbuf.toString();
	}

	public void testAES() throws Exception
	{
		try
		{
		  Provider[] providers = Security.getProviders();
		  for( int i=0; i<providers.length; i++ )
		  {
		   logger.warn( "Provider: " + providers[ i ].getName() + ", " + providers[ i ].getInfo() );
		   for( Iterator itr = providers[ i ].keySet().iterator(); 
		itr.hasNext(); )
		   {
			 String key = ( String )itr.next();
			 String value = ( String )providers[ i ].get( key );
			 logger.warn( "\t" + key + " = " + value );
		   }
		  }
		}
		catch( Exception e )
		{
		  e.printStackTrace();
		}
		String cipherAlgorithm="Rijndael";

		// Get the KeyGenerator

		KeyGenerator kgen = KeyGenerator.getInstance(cipherAlgorithm);
		kgen.init(128); // 192 and 256 bits may not be available

		// Generate the secret key specs.
		SecretKey skey = kgen.generateKey();
		byte[] raw = skey.getEncoded();

		SecretKeySpec skeySpec = new SecretKeySpec(raw, "Rijndael");

		// Instantiate the cipher

		Cipher cipher = Cipher.getInstance(cipherAlgorithm);

		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		int blockSize = cipher.getBlockSize();
		String plainString = "This is just an example";
		int plLen = plainString.length() + (blockSize - (plainString.length() % blockSize));
		byte [] plainText = new byte[plLen];
		plainString.getBytes(0, plainString.length(), plainText, 0);
		byte[] encrypted = cipher.doFinal(plainText);
		System.out.println("encrypted string: " + asHex(encrypted));
		//byte[] initVector = cipher.getIV();
		//cipher = Cipher.getInstance("Rijndael/CBC/PKCS5Padding");

		cipher.init(Cipher.DECRYPT_MODE, skeySpec);
		byte[] original = cipher.doFinal(encrypted);
		String originalString = new String(original);
		logger.warn(
			"Original string: " + originalString + " " + asHex(original));

		cipher = Cipher.getInstance(cipherAlgorithm);
		//IvParameterSpec spec = new IvParameterSpec(initVector);
		//cipher.init(Cipher.DECRYPT_MODE, skeySpec, spec);
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);
		original = cipher.doFinal(encrypted);
		originalString = new String(original);
		logger.warn(
			"Original string: " + originalString + " " + asHex(original));
	}
	
	public void testKeyGenPerformance() throws NoSuchAlgorithmException
	{		
		String cipherAlgorithm="Rijndael";
		KeyGenerator kgen = KeyGenerator.getInstance(cipherAlgorithm);
		kgen.init(128); // 192 and 256 bits may not be available
		long startTime = System.currentTimeMillis();
		for (int i=0; i<10;i++)
		{	
			SecretKey skey = kgen.generateKey();
		}
		long endTime = System.currentTimeMillis();
		logger.warn("Generated 1000 AES keys in "+(endTime - startTime)+ " ms = "+((endTime-startTime)/1000)+" ms/key");
	}
}
