package cryptix.sasl.srp;

// $Id: SRPSecretKey.java 8783 2013-06-27 16:55:51Z dave $
//
// Copyright (c) 2000-2001 The Cryptix Foundation Limited. All rights reserved.
//
// Use, modification, copying and distribution of this software is subject to
// the terms and conditions of the Cryptix General Licence. You should have
// received a copy of the Cryptix General License along with this library; if
// not, you can download a copy from <http://www.cryptix.org/>.

import java.math.BigInteger;

import javax.crypto.SecretKey;

import cryptix.sasl.SaslUtil;

/**
 * The SRP session key.
 *
 * @version $Revision: 1.2 $
 * @since draft-burdis-cat-sasl-srp-04
 */
public class SRPSecretKey
   implements SecretKey
{
	// Constants and variables
	// -------------------------------------------------------------------------

	/**
     * 
     */
    private static final long serialVersionUID = 4828781301643528189L;
    private final byte[] key;

	// Constructor(s)
	// -------------------------------------------------------------------------

	public SRPSecretKey
      (
         byte[] key
      )
	{
		super();

		this.key = (byte[]) key.clone();
	}

	public SRPSecretKey
      (
         BigInteger K
      )
	{
		this(SaslUtil.trim(K));
	}

	// Class methods
	// -------------------------------------------------------------------------

	// java.security.Key interface implementation
	// -------------------------------------------------------------------------

	public String
	getAlgorithm()
	{
		return "SRP";
	}

	public String
	getFormat()
	{
		return "RAW";
	}

	public byte[]
	getEncoded()
	{
		return (byte[]) key.clone();
	}

}

