package cryptix.sasl.rmi;

// $Id: SaslSocket.java 8783 2013-06-27 16:55:51Z dave $
//
// Copyright (c) 2000-2001 The Cryptix Foundation. All rights reserved.
//
// Use, modification, copying and distribution of this software is subject to
// the terms and conditions of the Cryptix General Licence. You should have
// received a copy of the Cryptix General License along with this library; if
// not, you can download a copy from <http://www.cryptix.org/>.

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.log4j.Category;

import cryptix.sasl.security.SaslServer;

/**
 * This class implements client sockets (also called just "sockets"). A socket
 * is an endpoint for communication between two machines.
 *
 * @version 1.0
 */
public class SaslSocket extends Socket
{
	// Constants and variables
	// --------------------------------------------------------------------------

   private static Category cat = Category.getInstance(SaslSocket.class);

   // InputStream used by socket
   private InputStream secureIn;

   // OutputStream used by socket
   private OutputStream secureOut;

   private static ThreadLocal threadUser = new ThreadLocal();
       // user is set for our thread in getInputStream()
       // this allows a server object to call getSASLUser()
       // to find out who the authenticated user is

   private SaslServer saslServer=null;

	// Constructor(s)
	// --------------------------------------------------------------------------

   /** No-arguments constructor --for server-side. */
   public SaslSocket() {
      super();

      // SASL authentication happens @ SaslServerSocket.accept() time...
   }

   /**
    * Constructs a <tt>SaslSocket<tt> given a host and a port --for client-side.
    *
    * @param host the host of the outbound connection.
    * @param port the port of the outbound connection.
    * @exception IOException if an exception occurs during construction.
    */
   public SaslSocket(String host, int port)
   throws IOException {
      super(host, port);

      // SASL authentication exchange occurs at:
      // for the client: SaslClientSocketFactory.createSocket() time, and
      // for the server: SaslServerSocket.accept() time
   }

   // Class methods
   // -------------------------------------------------------------------------

   // Instance methods
   // -------------------------------------------------------------------------

   /** Returns this socket's input stream. */
   public InputStream getInputStream() throws IOException {
      cat.debug("==> getInputStream()");
      threadUser.set(saslServer);
      InputStream result =
         (secureIn == null)
            ? super.getInputStream()
            : secureIn;

      cat.debug("<== getInputStream()");
      return result;
   }

   /** Returns this socket's output stream. */
   public OutputStream getOutputStream() throws IOException {
      cat.debug("==> getOutputStream()");

      OutputStream result =
         (secureOut == null)
            ? super.getOutputStream()
            : secureOut;

      cat.debug("<== getOutputStream()");
      return result;
   }

   /** Flushes the output stream before closing the socket. */
   public synchronized void close() throws IOException {
      getOutputStream().flush();
      super.close();
   }

   public synchronized void setSecureInputStream(InputStream in) {
      cat.debug("==> setSecureInputStream()");

      if (secureIn != null)
         cat.warn("replacing an already existing secure input stream...");

      secureIn = in;

      cat.debug("<== setSecureInputStream()");
   }

   public synchronized void setSecureOutputStream(OutputStream out) {
      cat.debug("==> setSecureOutputStream()");

      if (secureOut != null)
         cat.warn("replacing an already existing secure output stream...");

      secureOut = out;

      cat.debug("<== setSecureOutputStream()");
   }

   public void setSASLServer(SaslServer newSASLServer) {
     saslServer=newSASLServer;
   }
   /**
    * Returns the AuthorizationID for the SaslSocket that is in use on the current thread
    * for an RMI call.
    */
   public static String getSASLAuthorizationID() throws java.rmi.server.ServerNotActiveException {
       Object h =  threadUser.get();
       if (h != null) {
           return ((SaslServer)h).getAuthorizationID();
       } else {
           throw new java.rmi.server.ServerNotActiveException("not in a remote call");
       }
    }

    /**
     * Returns the Username for the SaslSocket that is in use on the current thread
     * for an RMI call.
    */
    public static String getSASLUsername() throws java.rmi.server.ServerNotActiveException {
        Object h =  threadUser.get();
        if (h != null) {
            return ((SaslServer)h).getUsername();
        } else {
            throw new java.rmi.server.ServerNotActiveException("not in a remote call");
        }
    }
}