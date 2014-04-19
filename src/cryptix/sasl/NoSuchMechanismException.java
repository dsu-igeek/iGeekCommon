package cryptix.sasl;

// $Id: NoSuchMechanismException.java 8783 2013-06-27 16:55:51Z dave $
//
// Copyright (c) 2000-2001 The Cryptix Foundation Limited. All rights reserved.
//
// Use, modification, copying and distribution of this software is subject to
// the terms and conditions of the Cryptix General Licence. You should have
// received a copy of the Cryptix General License along with this library; if
// not, you can download a copy from <http://www.cryptix.org/>.

import cryptix.sasl.security.SaslException;

/**
 * A checked exception thrown to indicate that a designated SASL mechanism
 * implementation was not found.
 *
 * @version $Revision: 1.2 $
 */
public class NoSuchMechanismException extends SaslException {
    private static final long serialVersionUID = 7969928219349278746L;

    /**
	 * Constructs a <tt>NoSuchMechanismException</tt> with the specified detail
	 * message. In the case of this exception, the detail message designates
	 * the offending mechanism name.
	 *
	 * @param arg the detail message, which in this case is the offencding
	 * mechanism name.
	 */
   public NoSuchMechanismException(String arg) {
      super(arg);
   }
}