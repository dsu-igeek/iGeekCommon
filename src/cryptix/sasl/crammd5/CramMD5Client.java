package cryptix.sasl.crammd5;

// $Id: CramMD5Client.java 8783 2013-06-27 16:55:51Z dave $
//
// Copyright (c) 2000-2001 The Cryptix Foundation Limited. All rights reserved.
//
// Use, modification, copying and distribution of this software is subject to
// the terms and conditions of the Cryptix General Licence. You should have
// received a copy of the Cryptix General License along with this library; if
// not, you can download a copy from <http://www.cryptix.org/>.

import java.io.IOException;
import java.util.Hashtable;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import cryptix.sasl.ClientMechanism;
import cryptix.sasl.SaslParams;
import cryptix.sasl.SaslUtil;
import cryptix.sasl.security.SaslClient;
import cryptix.sasl.security.SaslException;


/**
 *
 *
 * @version $Revision: 1.2 $
 * @since draft-burdis-cat-sasl-srp-03
 */
public class CramMD5Client
extends ClientMechanism
implements SaslClient, CramMD5Params, SaslParams
{
   // Constants and variables
   // -------------------------------------------------------------------------

   // Constructor(s)
   // -------------------------------------------------------------------------

   public CramMD5Client
      (
         String authorizationID,
         Hashtable props,
         CallbackHandler cbh
      )
   {
      super(CRAM_MD5_MECHANISM, authorizationID, "", "", props, cbh);
   }

   // Class methods
   // -------------------------------------------------------------------------

   // javax.security.sasl.SaslClient interface implementation
   // -------------------------------------------------------------------------

   public boolean hasInitialResponse()
   {
      return false;
   }

   public byte[]
   evaluateChallenge
      (
         byte[] challenge
      )
      throws SaslException
   {
      if (challenge == null)
         throw new SaslException("null challenge");
      try
      {
         String username;
         char[] password;
         Callback[] callbacks;

         if ((!properties.containsKey(USERNAME)) &&
             (!properties.containsKey(PASSWORD)))
         {
            callbacks = new Callback[2];

            NameCallback nameCB;
            String defaultName = System.getProperty("user.name");
            if (defaultName == null)
               nameCB = new NameCallback("username: ");
            else
               nameCB = new NameCallback("username: ",defaultName);

            PasswordCallback pwdCB = new PasswordCallback("password: ",false);
            callbacks[0] = nameCB;
            callbacks[1] = pwdCB;
            this.handler.handle(callbacks);
            username = nameCB.getName();
            password = pwdCB.getPassword();
         }
         else
         {
            if (properties.containsKey(USERNAME))
               username = (String) properties.get(USERNAME);
            else
            {
               callbacks = new Callback[1];
               NameCallback nameCB;
               String defaultName = System.getProperty("user.name");
               if (defaultName == null)
                  nameCB = new NameCallback("username: ");
               else
                  nameCB = new NameCallback("username: ", defaultName);

               callbacks[0] = nameCB;
               this.handler.handle(callbacks);
               username = nameCB.getName();
            }

            if (properties.containsKey(PASSWORD))
               password = ((String) properties.get(PASSWORD)).toCharArray();
            else
            {
               callbacks = new Callback[1];
               PasswordCallback pwdCB = new PasswordCallback("password: ",false);
               callbacks[0] = pwdCB;
               this.handler.handle(callbacks);
               password = pwdCB.getPassword();
            }

         }

         if (password == null)
            throw new SaslException("null password supplied");

         byte[] digest = CramMD5Util.createHMac(password,challenge);
         String response = username+" "+SaslUtil.toString(digest).toLowerCase();
         this.complete = true;

         return response.getBytes();
      }
      catch (UnsupportedCallbackException e)
      {
         throw new SaslException("evaluateChallenge()", e);
      }
      catch (IOException e)
      {
         throw new SaslException("evaluateChallenge()", e);
      }
   }

   protected String
   getNegotiatedQOP()
   {
      return "auth";
   }
}