package cryptix.sasl;

// $Id: IllegalMechanismStateException.java 8783 2013-06-27 16:55:51Z dave $
//
// Copyright (c) 2000-2001 The Cryptix Foundation Limited. All rights reserved.
//
// Use, modification, copying and distribution of this software is subject to
// the terms and conditions of the Cryptix General Licence. You should have
// received a copy of the Cryptix General License along with this library; if
// not, you can download a copy from <http://www.cryptix.org/>.

import cryptix.sasl.security.SaslException;

/**
 * A checked exception thrown to indicate that an operation that should be
 * invoked on a completed mechanism was invoked but the authentication phase of
 * that mechanism was not completed yet, or that an operation that should be
 * invoked on incomplete mechanisms was invoked but the authentication phase of
 * that mechanism was already completed.
 *
 * @version $Revision: 1.2 $
 * @since draft-weltman-java-sasl-05
 */
public class IllegalMechanismStateException
extends SaslException
{
    private static final long serialVersionUID = 847911711275031460L;

    /**
	 * Constructs a new instance of <tt>IllegalMechanismStateException</tt> with
	 * no detail message.
	 */
   public IllegalMechanismStateException() {
      super();
   }

	/**
	 * Constructs a new instance of <tt>IllegalMechanismStateException</tt> with
	 * the specified detail message.
	 *
	 * @param s the detail message.
	 */
   public IllegalMechanismStateException(String s) {
      super(s);
   }
}