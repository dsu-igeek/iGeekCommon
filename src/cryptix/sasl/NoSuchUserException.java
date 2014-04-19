package cryptix.sasl;

// $Id: NoSuchUserException.java 8783 2013-06-27 16:55:51Z dave $
//
// Copyright (c) 2000-2001 The Cryptix Foundation Limited. All rights reserved.
//
// Use, modification, copying and distribution of this software is subject to
// the terms and conditions of the Cryptix General Licence. You should have
// received a copy of the Cryptix General License along with this library; if
// not, you can download a copy from <http://www.cryptix.org/>.

import cryptix.sasl.security.SaslException;

/**
 * A checked exception thrown to indicate that a designated user is unknown to
 * the authentication layer.
 *
 * @version $Revision: 1.2 $
 * @since draft-burdis-cat-sasl-srp-03
 */
public class NoSuchUserException
   extends SaslException
{
    private static final long serialVersionUID = 8791127758014064256L;

    /**
	 * Constructs a <tt>NoSuchUserException</tt> with no detail message.
	 */
   public NoSuchUserException()
   {
      super();
   }

	/**
	 * Constructs a <tt>NoSuchUserException</tt> with the specified detail
	 * message. In the case of this exception, the detail message designates
	 * the offending username.
	 *
	 * @param arg the detail message, which in this case is the username.
	 */
   public NoSuchUserException
      (
         String arg
      )
   {
      super(arg);
   }
}

