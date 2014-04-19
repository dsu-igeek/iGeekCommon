package cryptix.sasl.otp;

// $Id: OTPOutputBuffer.java 8783 2013-06-27 16:55:51Z dave $
//
// Copyright (c) 2000-2001 The Cryptix Foundation Limited. All rights reserved.
//
// Use, modification, copying and distribution of this software is subject to
// the terms and conditions of the Cryptix General Licence. You should have
// received a copy of the Cryptix General License along with this library; if
// not, you can download a copy from <http://www.cryptix.org/>.

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.Category;

import cryptix.sasl.OutputBuffer;
import cryptix.sasl.SaslEncodingException;
import cryptix.sasl.SaslUtil;

/**
 * The cryptix implementation of an outgoing SASL buffer.
 *
 * @version $Revision: 1.2 $
 */
public class OTPOutputBuffer
   extends OutputBuffer
{
   // Constants and variables
   // -------------------------------------------------------------------------

	private static Category cat = Category.getInstance(OTPOutputBuffer.class);

   /** The internal output stream. */
   private ByteArrayOutputStream out;


   // Constructor(s)
   // -------------------------------------------------------------------------

   public OTPOutputBuffer()
   {
      out = new ByteArrayOutputStream();
   }

   // Class methods
   // -------------------------------------------------------------------------

   // Instance methods
   // -------------------------------------------------------------------------

   /**
    * Encodes a SASL Text to the current buffer.
    *
    * @param str the Text element.
    * @exception SaslEncodingException if an encoding size constraint is
    * violated.
    * @exception UnsupportedEncodingException if the UTF-8 encoding is not
    * supported on this platform.
    * @exception IOException if any other I/O exception occurs during the
    * operation.
    */
   public void
   setText
      (
         String str
      )
      throws IOException
   {
      cat.debug("==> setText()");

      byte[] b = str.getBytes("UTF8");
      cat.debug("b = "+SaslUtil.dumpString(b));
      int length = b.length;
      if (length > TWO_BYTE_HEADER_LIMIT)
      {
         cat.error("SASL Text too long");
         throw new SaslEncodingException("SASL text too long");
      }

      out.write(b);

      cat.debug("<== setText()");
   }

   /**
    * Returns the encoded form of the current buffer.
    *
    * @exception SaslEncodingException if an encoding size constraint is
    * violated.
    */
   public byte[]
   encode()
      throws SaslEncodingException
   {
      cat.debug("==> encode()");

      int length = out.size();
      if (length > BUFFER_LIMIT || length < 0)
      {
         cat.error("SASL Buffer too long");
         throw new SaslEncodingException("SASL buffer too long");
      }

      byte[] result = new byte[length+4];
      result[0] = (byte)(length >>> 24);
      result[1] = (byte)(length >>> 16);
      result[2] = (byte)(length >>>  8);
      result[3] = (byte) length;
      byte[] buffer = out.toByteArray();
      System.arraycopy(buffer, 0, result, 4, length);

      cat.debug("<== encode() --> "+SaslUtil.dumpString(result));
      return result;
   }
}