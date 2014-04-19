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

import java.io.Serializable;

import com.igeekinc.util.BitTwiddle;

public class GeneratorID implements Serializable
{
  private static final int kGeneratorIDByteSize = 16;
  static final long serialVersionUID = 1098115936587407823L;
  public static final int kGeneratorIDStringLength = 32;
  private transient Integer hashCodeVal = null;
  
  byte [] idBytes;
  public GeneratorID(byte [] inIDBytes)
  {
    if (inIDBytes.length != kGeneratorIDByteSize)
      throw new IllegalArgumentException("inIDBytes to GeneratorID() must be 16 byes in length"); //$NON-NLS-1$
    idBytes = new byte[16];

    System.arraycopy(inIDBytes, 0, idBytes, 0, kGeneratorIDByteSize);
  }

  public GeneratorID(byte [] inIDBytes, int offset)
  {
    if (inIDBytes.length - offset < kGeneratorIDByteSize)
      throw new IllegalArgumentException("inIDBytes too short"); //$NON-NLS-1$
    idBytes = new byte[16];

    System.arraycopy(inIDBytes, offset, idBytes, 0, kGeneratorIDByteSize);
  }
  
  public GeneratorID(String inIDString)
  {
  	if (inIDString.length() != kGeneratorIDStringLength)
  		throw new IllegalArgumentException("GeneratorID string representation must be 32 characters, not "+ //$NON-NLS-1$
  				inIDString.length());
  	idBytes = new byte[kGeneratorIDByteSize];
  	for (int curByteNum = 0; curByteNum < idBytes.length; curByteNum++)
  	{
  		int curStartPos = curByteNum * 2;
  		idBytes[curByteNum] = (byte)Integer.parseInt(inIDString.substring(curStartPos, curStartPos + 2), 16);
  	}
  }
  public String toString()
  {
    StringBuffer returnStrBuf=new StringBuffer(idBytes.length*2);
    int curByteNum;
    /*
    for (curByteNum = 0; curByteNum < idBytes.length-1; curByteNum++)
      returnStrBuf.append(Integer.toHexString(((int)idBytes[curByteNum])&0xff)+":");;
    returnStrBuf.append(Integer.toHexString(((int)idBytes[idBytes.length-1])&0xff));
    */
    for (curByteNum = 0; curByteNum < idBytes.length; curByteNum++)
      returnStrBuf.append(BitTwiddle.toHexString(idBytes[curByteNum], 2));
    return(returnStrBuf.toString());
  }

  public byte [] getBytes()
  {
	  byte [] returnBytes = new byte[idBytes.length];
	  getBytes(returnBytes, 0);
	  return returnBytes;
  }
  
  public void getBytes(byte [] dest, int offset)
  {
    System.arraycopy(idBytes, 0, dest, offset, idBytes.length);
  }

  void setFromBytes(byte [] src, int offset)
  {
    System.arraycopy(src, offset, idBytes, 0, idBytes.length);
  }
  public boolean equals(Object checkObject)
  {
    if (GeneratorID.class.isAssignableFrom(checkObject.getClass()))
    {
      GeneratorID checkID = (GeneratorID)checkObject;
      for (int curByteNum = 0; curByteNum < idBytes.length; curByteNum++)
        if (checkID.idBytes[curByteNum] != idBytes[curByteNum])
          return false;
      return true;
    }
    return false;
  }
  
    public int hashCode()
    {
        if (hashCodeVal == null)
        {
            int hcv = 0;
            for (int curIntNum = 0; curIntNum < kGeneratorIDByteSize/4; curIntNum++)
            {
                hcv = hcv ^ BitTwiddle.javaByteArrayToInt(idBytes, curIntNum * 4);
            }
            hashCodeVal = new Integer(hcv);
        }
        return hashCodeVal.intValue();
    }
}