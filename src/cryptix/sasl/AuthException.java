package cryptix.sasl;

// $Id: AuthException.java 8783 2013-06-27 16:55:51Z dave $
//
// Copyright (c) 2000-2001 The Cryptix Foundation Limited. All rights reserved.
//
// Use, modification, copying and distribution of this software is subject to
// the terms and conditions of the Cryptix General Licence. You should have
// received a copy of the Cryptix General License along with this library; if
// not, you can download a copy from <http://www.cryptix.org/>.

import cryptix.sasl.security.SaslException;

/**
 * Exception thrown on errors and failures during the authentication process.
 *
 * @version $Revision: 1.2 $
 * @since   0.8.9
 */
public class AuthException extends SaslException
{
    private static final long serialVersionUID = -9104629455437745627L;

/**
    * Constructs a new instance of <tt>AuthException</tt>. The root exception
    * and the detailed message are null.
    */
   public AuthException() {
      super();
   }

   /**
    * Constructs a default exception with a detailed message and no root
    * exception.
    *
    * @param message Possibly null additional detail about the exception.
    */
   public AuthException(String message) {
      super(message);
   }

   /**
    * Constructs a new instance of <tt>AuthException</tt> with a detailed
    * message and a root exception.
    *
    * @param message possibly null additional detail about the exception.
    * @param x a possibly null root exception that caused this exception.
    */
   public AuthException(String message, Throwable x) {
      super(message, x);
   }
}
