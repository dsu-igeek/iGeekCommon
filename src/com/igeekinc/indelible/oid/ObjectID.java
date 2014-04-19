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
import java.math.BigInteger;

import com.igeekinc.util.BitTwiddle;
public abstract class ObjectID implements Serializable
{
    static final long serialVersionUID = 1329942042374183297L;
    byte version=1, type;

    GeneratorID generatorID;
    long        timeStamp;

    int         uniquifier;


    static final int kVersionOffset = 0;
    static final int kVersionLen = 1;
    static final int kTypeOffset = kVersionOffset+kVersionLen;
    static final int kTypeLen = 1;
    static final int kGeneratorIDOffset = kTypeOffset+kTypeLen;
    static final int kGeneratorIDLen = 16;
    static final int kTimestampOffset = kGeneratorIDOffset+kGeneratorIDLen;
    static final int kTimestampLen = 8;
    static final int kUniquifierOffset = kTimestampOffset+kTimestampLen;
    static final int kUniquifierLen = 4;
    static final int kReservedOffset = kUniquifierOffset+kUniquifierLen;
    static final int kReservedLen = 2;

    public static final int kTotalBytes = kReservedOffset + kReservedLen; // (32 bytes)

    public static final byte kOperationSetOIDType=1;
    public static final byte kExecutionPlanOIDType=2;
    public static final byte kIndelibleFSOIDType = 4;
    public static final byte kMediaSetOIDType = 5;
    public static final byte kIndelibleRuleSetOIDType = 6;
    public static final byte kCASCollectionOIDType = 7;
    //public static final byte kCASServerOIDType = 8;   // Not in use anymore
    public static final byte kCASSegmentOIDType = 9;
    public static final byte kExecutionPlanStatusOIDType=10;
    public static final byte kFileOperationQueueOIDType = 11;
    public static final byte kMediaOIDType = 12;
    public static final byte kQueueOIDType = 13;
    public static final byte kInventoryOIDType=14;
    public static final byte kNetworkDataDescriptorOIDType = 15;
    public static final byte kServerOIDType = 16;
    public static final byte kDataMoverSessionOIDType = 17;
    public static final byte kCASStoreOIDType = 18;

    static final int kObjectIDLength = kReservedOffset + kReservedLen;
    public static final int kObjectStrLength = kReservedOffset * 2;  // When we serialize to a string the two empty bytes do not get serialized
    public void getBytes(byte [] dest, int offset)
    {
        dest[offset+kVersionOffset] = version;
        dest[offset+kTypeOffset] = type;
        generatorID.getBytes(dest, offset+kGeneratorIDOffset);
        BitTwiddle.longToJavaByteArray(timeStamp, dest, offset+kTimestampOffset);
        BitTwiddle.intToJavaByteArray(uniquifier, dest, offset+kUniquifierOffset);
        for (int curZeroFill = 0; curZeroFill < kReservedLen; curZeroFill++)
        {
            dest[offset + kReservedOffset + curZeroFill]=0;
        }
    }


	public byte[] getBytes()
	{
		byte [] returnBytes = new byte[kTotalBytes];
		getBytes(returnBytes, 0);
		return returnBytes;
	}
	
    void setFromBytes(byte [] src, int offset)
    {
        version = src[offset + kVersionOffset];
        if (version != 1)
            throw new IllegalArgumentException("ObjectID version in byte input != 1"); //$NON-NLS-1$
        type = src[offset+kTypeOffset];
        generatorID = new GeneratorID(src, offset+kGeneratorIDOffset);
        timeStamp = BitTwiddle.javaByteArrayToLong(src, offset + kTimestampOffset);
        uniquifier = BitTwiddle.javaByteArrayToInt(src, offset + kUniquifierOffset);
    }

    public String toString()
    {
        StringBuffer returnString = new StringBuffer(20);
        returnString.append(BitTwiddle.toHexString(version, 2));
        returnString.append(BitTwiddle.toHexString(type, 2));
        returnString.append(generatorID.toString());
        returnString.append(BitTwiddle.toHexString(timeStamp, 16));
        returnString.append(BitTwiddle.toHexString(uniquifier, 8));
        return(returnString.toString());
    }

    public BigInteger toBigInteger()
    {
        BigInteger returnInteger = new BigInteger(toString(), 16);
        return returnInteger;
    }

    protected void setFromString(String idString)
    {
        version = getVersionFromString(idString);

        byte myType = getTypeFromString(idString);
        type = myType;

        GeneratorID myGeneratorID = getGeneratorIDFromString(idString);
        generatorID = myGeneratorID;

        timeStamp = getTimeStampFromString(idString);

        uniquifier = getUniquifierFromString(idString);  	
    }

    /**
     * Validates that the string is a valid ID.  Throws an IllegalArgumentException if not
     * @param idString
     */
    public static void validateString(String idString)
    {
        if (idString.length() != kObjectStrLength)
            throw new IllegalArgumentException("Object ID '"+idString+"' is length "+idString.length()+" not "+kObjectStrLength+" - invalid ID"); //$NON-NLS-1$ //$NON-NLS-2$
        for (int curCharNum = 0; curCharNum < idString.length(); curCharNum++)
        {
            char curChar = idString.charAt(curCharNum);
            if (!((curChar >= '0' && curChar <='9') || (curChar >= 'A' && curChar <= 'F') || (curChar >='a' && curChar <='f')))
                throw new IllegalArgumentException("Object ID string must be hexadecimal ascii");          //$NON-NLS-1$
        }
    }
    /**
     * @param idString
     * @return
     */
    public static int getUniquifierFromString(String idString)
    {
        validateString(idString);
        String uniquifierString = idString.substring(52, 60);
        int myUniquifier = Integer.parseInt(uniquifierString, 16);
        return myUniquifier;
    }

    /**
     * @param idString
     * @return
     */
    public static long getTimeStampFromString(String idString)
    {
        validateString(idString);
        String timeStampString = idString.substring(36, 52);
        long myTimeStamp = Long.parseLong(timeStampString, 16);
        return myTimeStamp;
    }

    /**
     * @param idString
     * @return
     */
    public static GeneratorID getGeneratorIDFromString(String idString)
    {
        validateString(idString);
        String generatorIDString = idString.substring(4, 36);
        GeneratorID myGeneratorID = new GeneratorID(generatorIDString);
        return myGeneratorID;
    }

    /**
     * @param idString
     * @return
     */
    public static byte getTypeFromString(String idString)
    {
        validateString(idString);
        String typeString = idString.substring(2, 4);
        byte myType = (byte)Integer.parseInt(typeString, 16);
        return myType;
    }

    /**
     * @param idString
     * @return
     */
    public static byte getVersionFromString(String idString)
    {
        validateString(idString);
        String versionString = idString.substring(0, 2);
        byte myVersion = (byte)Integer.parseInt(versionString, 16);
        return myVersion;
    }

    static public byte getVersion(byte [] oidBytes)
    {
        return(getVersion(oidBytes, 0, oidBytes.length));
    }

    static public byte getVersion(byte [] oidBytes, int offset, int len)
    {
        if (len != kObjectIDLength)
        {
            throw new IllegalArgumentException("Object ID byte arrays must be "+kObjectIDLength+" bytes long"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return oidBytes[kVersionOffset];
    }

    static public byte getType(byte [] oidBytes)
    {
        return(getType(oidBytes, 0, oidBytes.length));
    }

    static public byte getType(byte [] oidBytes, int offset, int len)
    {
        if (len != kObjectIDLength)
        {
            throw new IllegalArgumentException("Object ID byte arrays must be "+kObjectIDLength+" bytes long"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return oidBytes[kTypeOffset];
    }

    void setTimestamp(long inTimestamp)
    {
        timeStamp = inTimestamp;
    }

    void setUniquifier(int inUniquifier)
    {
        uniquifier = inUniquifier;
    }

    public boolean equals(Object checkObject)
    {
        if (checkObject != null && ObjectID.class.isAssignableFrom(checkObject.getClass()))
        {
            ObjectID checkID = (ObjectID)checkObject;
            if (checkID.type == type && checkID.version == version && checkID.generatorID.equals(generatorID) &&
                    checkID.timeStamp==timeStamp && checkID.uniquifier == uniquifier)
                return true;
        }
        return false;
    }

    public int hashCode()
    {
        int hcv = generatorID.hashCode();
        hcv = hcv ^ (int)(timeStamp >> 32);
        hcv = hcv ^ (int)timeStamp;
        hcv = hcv ^ uniquifier;
        return hcv;
    }
}