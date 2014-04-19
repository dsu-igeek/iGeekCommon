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
 
package com.igeekinc.util;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;

import com.igeekinc.junitext.FSRequirement;
import com.igeekinc.junitext.FSTestCase;
import com.igeekinc.junitext.VolumeInfo;
import com.igeekinc.testutils.FileCompare;
import com.igeekinc.testutils.FileCompareException;
import com.igeekinc.testutils.TestFilesTool;
import com.igeekinc.util.pauseabort.AbortedException;
import com.igeekinc.util.pauseabort.PauseAbort;

public class FileCopyTest extends FSTestCase
{
	
    protected void setUp() throws Exception 
    {
		super.setUp();
		//int cryptixPos = Security.insertProviderAt(new cryptix.jce.provider.CryptixCrypto(), 2);
		Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 2);
	}

	public void testCopyFile()
    throws IOException, AbortedException, FileCompareException
    {        
        testFileCopyForSize(1024*1024);
        testFileCopyForSize(16*1024*1024);
        testFileCopyForSize(64*1024*1024);
    }

    private void testFileCopyForSize(long copySize) throws IOException, AbortedException, FileCompareException
    {
        VolumeInfo [] volInfo = getVolumesForRun();
        ClientFile sourceDir = SystemInfo.getSystemInfo().getClientFileForPath(volInfo[0].getSelectedPath().getPath());
        ClientFile source = (ClientFile)sourceDir.getChild("src");
        
        ClientFile destDir = SystemInfo.getSystemInfo().getClientFileForPath(volInfo[1].getSelectedPath().getPath());
        ClientFile dest = (ClientFile)destDir.getChild("dest");

        if (dest.exists())
        	dest.delete();
        logger.warn("source = "+source+", dest = "+dest);
        SHA1HashID testID = TestFilesTool.createTestFile(source, copySize);
        assertTrue(TestFilesTool.verifyFile(source, testID, copySize));         
        FileCopy fileCopy = SystemInfo.getSystemInfo().getFileCopy();

        PauseAbort pauser = new PauseAbort(Logger.getLogger(getClass()));
        FileCopyProgressIndicator fcpi = new FileCopyProgressIndicator();
        
        try
        {
            Thread.sleep(3000); // Make sure that create/modify dates will be different
        }
        catch (InterruptedException e)
        {
            
        }
        long startTime = System.currentTimeMillis();
        
        
        fileCopy.copyFile(source, false, dest, false, true, pauser, fcpi);
        
        
        long endTime = System.currentTimeMillis();
        long elapsedMS = (endTime - startTime);
        double mbPerSec = (double)copySize/(double)elapsedMS * (double)1000/((double)1024*1024);
        logger.warn("Time to copy "+copySize+" bytes = "+elapsedMS+" ms ("+mbPerSec+" MB/s)");
        FileCompare.getFileCompare().compareFiles(source, dest);
        assertTrue(TestFilesTool.verifyFile(dest, testID, copySize));
        source.delete();
        dest.delete();
    }

    public void testEncryptFile() throws IOException, AbortedException, FileCompareException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, NoSuchPaddingException, IllegalStateException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, ClassNotFoundException, InterruptedException
    {
    	int copySize = 1024*1024;
    	VolumeInfo [] volInfo = getVolumesForRun();
        ClientFile sourceDir = SystemInfo.getSystemInfo().getClientFileForPath(volInfo[0].getSelectedPath().getPath());
        ClientFile plaintext = (ClientFile)sourceDir.getChild("plaintext");
        
        ClientFile destDir = SystemInfo.getSystemInfo().getClientFileForPath(volInfo[1].getSelectedPath().getPath());
        ClientFile encrypted = (ClientFile)destDir.getChild("plaintext.enc");

        ClientFile decrypted = (ClientFile)sourceDir.getChild("decrypted");
        
        plaintext.delete();
        encrypted.delete();
        decrypted.delete();
        
        logger.warn("source = "+plaintext+", dest = "+encrypted);
        
        SHA1HashID testID = TestFilesTool.createTestFile(plaintext, copySize);
        assertTrue(TestFilesTool.verifyFile(plaintext, testID, copySize));   
        Thread.sleep(2000);     // Make some time elapsed difference between the creation of the plaintext and the encrypted file
        FileCopy fileCopy = SystemInfo.getSystemInfo().getFileCopy();

        PauseAbort pauser = new PauseAbort(Logger.getLogger(getClass()));
        FileCopyProgressIndicator fcpi = new FileCopyProgressIndicator();
        
		String passphrase = "abcd";
		PBEKeySpec passphraseSpec = new PBEKeySpec(passphrase.toCharArray());

		KeyGenerator kgen = KeyGenerator.getInstance("Rijndael");
		kgen.init(128); // 192 and 256 bits may not be available

		// Generate the secret key specs.
		SecretKey skey = kgen.generateKey();
		byte[] raw = skey.getEncoded();

		SecretKeySpec sessionKey = new SecretKeySpec(raw, "Rijndael");
		EncryptionKeys testKeys = new EncryptionKeys(passphraseSpec, false);
		
		
        long startTime = System.currentTimeMillis();
        
        // Encrypt the file!
        fileCopy.encryptFile(plaintext, encrypted,testKeys.getEncryptionKey(), sessionKey, false, true, pauser, fcpi);
        
        
        long endTime = System.currentTimeMillis();
        long elapsedMS = (endTime - startTime);
        double mbPerSec = (double)copySize/(double)elapsedMS * (double)1000/((double)1024*1024);
        logger.warn("Time to encrypt/copy "+copySize+" bytes = "+elapsedMS+" ms ("+mbPerSec+" MB/s)");
        
		PBEKeySpec testKeySpec = new PBEKeySpec(passphrase.toCharArray());
		PrivateKey decryptKey = testKeys.decryptDecryptionKey(testKeySpec);
		
		startTime = System.currentTimeMillis();
		
		// Decrypt the file!
        fileCopy.decryptFile(encrypted, false, decryptKey, decrypted, true, pauser, fcpi);
        
        
        endTime = System.currentTimeMillis();
        elapsedMS = (endTime - startTime);
        mbPerSec = (double)copySize/(double)elapsedMS * (double)1000/((double)1024*1024);
        logger.warn("Time to decrypt/copy "+copySize+" bytes = "+elapsedMS+" ms ("+mbPerSec+" MB/s)");
        
        FileCompare.getFileCompare().compareFiles(plaintext, decrypted);
        assertTrue(TestFilesTool.verifyFile(decrypted, testID, copySize));
        plaintext.delete();
        encrypted.delete();
        decrypted.delete();
    }

    public void testDecryptFile()
    {
    }

    public void testCopyFork()
    {
    }

    public FSRequirement[] getFSRequirements()
    {
        FSRequirement [] returnRequirements = 
        {new FSRequirement("Source", new int[]{FSRequirement.kAnyFS}, 64*1024*1024), 
            new FSRequirement("Destination", new int[]{FSRequirement.kAnyFS}, 64*1024*1024)};
    return returnRequirements;
    }

}
