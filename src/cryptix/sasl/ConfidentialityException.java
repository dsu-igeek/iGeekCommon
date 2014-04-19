package cryptix.sasl;

// $Id: ConfidentialityException.java 8783 2013-06-27 16:55:51Z dave $
//
// Copyright (c) 2000-2001 The Cryptix Foundation Limited. All rights reserved.
//
// Use, modification, copying and distribution of this software is subject to
// the terms and conditions of the Cryptix General Licence. You should have
// received a copy of the Cryptix General License along with this library; if
// not, you can download a copy from <http://www.cryptix.org/>.

import cryptix.sasl.security.SaslException;

/**
 * Used by mechanisms that offer a security services layer, this checked
 * exception is thrown to indicate that a violation has occured during the
 * processing of a <i>confidentiality</i> protection filter.
 *
 * @version $Revision: 1.2 $
 * @since draft-burdis-cat-srp-sasl-04
 */
public class ConfidentialityException extends SaslException {
    private static final long serialVersionUID = 2873388790933293650L;

    /**
	 * Constructs a new instance of <tt>ConfidentialityException</tt> with no
	 * detail message.
	 */
   public ConfidentialityException() {
      super();
   }

	/**
	 * Constructs a new instance of <tt>ConfidentialityException</tt> with the
	 * specified detail message.
	 *
	 * @param s the detail message.
	 */
   public ConfidentialityException(String s) {
      super(s);
   }

   /**
    * Constructs a new instance of <tt>ConfidentialityException</tt> with a
    * detailed message and a root exception.
    *
    * @param s possibly null additional detail about the exception.
    * @param x a possibly null root exception that caused this one.
    */
   public ConfidentialityException(String s, Throwable x) {
      super(s, x);
   }
}