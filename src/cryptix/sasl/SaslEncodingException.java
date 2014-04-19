package cryptix.sasl;



// $Id: SaslEncodingException.java 8783 2013-06-27 16:55:51Z dave $

//

// Copyright (c) 2000-2001 The Cryptix Foundation Limited. All rights reserved.

//

// Use, modification, copying and distribution of this software is subject to

// the terms and conditions of the Cryptix General Licence. You should have

// received a copy of the Cryptix General License along with this library; if

// not, you can download a copy from <http://www.cryptix.org/>.



import cryptix.sasl.security.SaslException;



/**
 * A checked exception, thrown when an exception occurs while decoding a SASL
 * buffer and/or a SASL data element from/to a buffer.
 *
 * @version $Revision: 1.2 $
 * @since draft-burdis-cat-srp-sasl-04
 */

public class SaslEncodingException
   extends SaslException
{
    private static final long serialVersionUID = 2824488076934125630L;

    /**
	 * Constructs a <tt>SaslEncodingException</tt> with no detail message.
	 */

   public SaslEncodingException()
   {
      super();
   }



	/**
	 * Constructs a <tt>SaslEncodingException</tt> with the specified detail
	 * message.
	 *
	 * @param s the detail message.
	 */

   public SaslEncodingException(String arg)
   {
      super(arg);
   }
}