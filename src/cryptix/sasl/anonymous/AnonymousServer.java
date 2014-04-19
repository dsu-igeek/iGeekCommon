package cryptix.sasl.anonymous;

// $Id: AnonymousServer.java 5474 2010-09-15 09:10:36Z dave $
//
// Copyright (c) 2000-2001 The Cryptix Foundation Limited. All rights reserved.
//
// Use, modification, copying and distribution of this software is subject to
// the terms and conditions of the Cryptix General Licence. You should have
// received a copy of the Cryptix General License along with this library; if
// not, you can download a copy from <http://www.cryptix.org/>.

import cryptix.sasl.SaslParams;
import cryptix.sasl.ServerMechanism;
import cryptix.sasl.security.SaslException;
import cryptix.sasl.security.SaslServer;


/**
 * The Cryptix implementation of the ANONYMOUS SASL mechanism.
 *
 * @version $Revision$
 * @since draft-burdis-cat-sasl-srp-03
 */
public class AnonymousServer
extends ServerMechanism
implements SaslServer, SaslParams
{
   // Constants and variables
   // -------------------------------------------------------------------------

   // Constructor(s)
   // -------------------------------------------------------------------------

   public AnonymousServer() {
      super(ANONYMOUS_MECHANISM, "", "", null, null);
   }

   // Class methods
   // -------------------------------------------------------------------------

   // javax.security.sasl.SaslServer interface implementation
   // -------------------------------------------------------------------------

   public byte[] evaluateResponse(byte[] response) throws SaslException {
      if (response == null)
         return null;
      username="anonymous";
      authorizationID = new String(response);
      if (AnonymousUtil.isValidTraceInformation(authorizationID)) {
         this.complete = true;
         return null;
      }

      authorizationID = null;
      throw new SaslException("Not a valid email address");
   }
}
