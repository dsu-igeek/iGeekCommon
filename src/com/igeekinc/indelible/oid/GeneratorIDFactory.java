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
 
package com.igeekinc.indelible.oid;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;

import com.igeekinc.util.BitTwiddle;
import com.igeekinc.util.EthernetID;
import com.igeekinc.util.SystemInfo;

public class GeneratorIDFactory
{
  static final int kGeneratorSrcSize = 46;
  static final int kMACAddrOffset = 0;
  static final int kIPAddrOffset = 6;
  static final int kTimeOffset = 22;
  static final int kRandomnessOffset = 30;
  static final int kGeneratorMD5Size = 16;

  static final int kRandomnessDataSize = 16;
  static final int kIPAddrSize = 16; // Leave space for an IPV6 address
  SystemInfo    ourSystemInfo;
  public GeneratorIDFactory()
  {
    ourSystemInfo = SystemInfo.getSystemInfo();
  }

  public GeneratorID createGeneratorID()
  {
    byte [] generatorData = new byte[kGeneratorSrcSize];
    byte [] generatorMD5 = new byte[kGeneratorMD5Size];
    fillInTimeData(generatorData, kTimeOffset);
    fillInRandomnessData(generatorData, kRandomnessOffset);
    fillInMACAddrData(generatorData, kMACAddrOffset);
    fillInIPAddrData(generatorData, kIPAddrOffset);
    createMD5Data(generatorData, generatorMD5, 0);
    return new GeneratorID(generatorMD5);
  }

  void fillInTimeData(byte [] timeData, int offset)
  {
    Date   now = new Date();
    long   longTime = now.getTime();
    BitTwiddle.longToJavaByteArray(longTime, timeData, offset);
  }

  void fillInRandomnessData(byte [] randomnessData, int offset)
  {
    byte [] prngRandomnessData = new byte[kRandomnessDataSize];

    try
    {
      SecureRandom prng = SecureRandom.getInstance("SHA1PRNG"); //$NON-NLS-1$
      prng.nextBytes(prngRandomnessData);
    }
    catch(NoSuchAlgorithmException e)
    {
      System.err.println("Couldn't get secure random number generator - falling back to java.util.Random"); //$NON-NLS-1$
      java.util.Random prng = new java.util.Random();
      prng.nextBytes(prngRandomnessData);
    }

    System.arraycopy(prngRandomnessData, 0, randomnessData, offset, kRandomnessDataSize);
  }

  void fillInMACAddrData(byte [] generatorData, int offset)
  {
    EthernetID ourID = ourSystemInfo.getEthernetID();
    ourID.getBytes(generatorData, offset);
  }

  void fillInIPAddrData(byte [] generatorData, int offset)
  {
    int     zeroFill;
    try
    {
      InetAddress localAddr = InetAddress.getLocalHost();
      byte [] addrBytes = localAddr.getAddress();

      for (zeroFill = 0; zeroFill < kIPAddrSize - addrBytes.length; zeroFill++)
        generatorData[zeroFill+offset] = 0;
      for (int curByteNum = 0; curByteNum < addrBytes.length; curByteNum++)
        generatorData[zeroFill+offset+curByteNum] = addrBytes[curByteNum];
    }
    catch (UnknownHostException e)
    {
      // Hmmm...guess we don't have an IP address - let's just fill in some 0's
      for (zeroFill = 0; zeroFill < kIPAddrSize; zeroFill++)
        generatorData[zeroFill+offset] = 0;

    }
  }
  void createMD5Data(byte [] generatorData, byte [] generatorMD5Data, int offset)
  {
    java.security.MessageDigest md5Gen;
    try
    {
      //md5Gen = MessageDigest.getInstance("MD5");
      md5Gen = java.security.MessageDigest.getInstance("MD5"); //$NON-NLS-1$
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new InternalError("Couldn't find an MD5 instance"); //$NON-NLS-1$
    }
      if (md5Gen.getDigestLength() != kGeneratorMD5Size)
    {
      throw new java.lang.InternalError("MD5 digest size != "+kGeneratorMD5Size); //$NON-NLS-1$
    }
    md5Gen.update(generatorData);
    byte [] md5Data = md5Gen.digest();
    System.arraycopy(md5Data, 0, generatorMD5Data, offset, kGeneratorMD5Size);
  }
}