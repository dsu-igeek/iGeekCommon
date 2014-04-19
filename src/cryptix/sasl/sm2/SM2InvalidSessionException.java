package cryptix.sasl.sm2;

// $Id: SM2InvalidSessionException.java 8783 2013-06-27 16:55:51Z dave $
//
// Copyright (c) 2000-2001 The Cryptix Foundation Limited. All rights reserved.
//
// Use, modification, copying and distribution of this software is subject to
// the terms and conditions of the Cryptix General Licence. You should have
// received a copy of the Cryptix General License along with this library; if
// not, you can download a copy from <http://www.cryptix.org/>.

import cryptix.sasl.security.SaslException;

/**
 * A checked exception thrown to indicate that a designated SM2 session
 * is, or has become, invalid.
 *
 * @version $Revision: 1.2 $
 * @since draft-naffah-cat-sasl-sm2-00
 */
public class SM2InvalidSessionException extends SaslException {
    private static final long serialVersionUID = -3905466041306976541L;

    /**
	 * Constructs a <tt>SM2InvalidSessionException</tt> with the specified
	 * detail message. In the case of this exception, the detail message
	 * designates the session identifier.
	 *
	 * @param sid the session identifier.
	 */
   public SM2InvalidSessionException(String sid) {
      super(sid);
   }
}