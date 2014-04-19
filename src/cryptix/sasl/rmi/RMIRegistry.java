package cryptix.sasl.rmi;

// $Id: RMIRegistry.java 8783 2013-06-27 16:55:51Z dave $
//
// Copyright (c) 2000-2001 The Cryptix Foundation. All rights reserved.
//
// Use, modification, copying and distribution of this software is subject to
// the terms and conditions of the Cryptix General Licence. You should have
// received a copy of the Cryptix General License along with this library; if
// not, you can download a copy from <http://www.cryptix.org/>.

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Date;
import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Category;

import cryptix.sasl.security.Sasl;
import cryptix.sasl.srp.PasswordFile;
import cryptix.sasl.srp.SRPParams;

/**
 * A utility class to start/run an RMI registry using SASL on connections for
 * bind and lookup operations.
 *
 * @version $Revision: 1.2 $
 */
public class RMIRegistry implements Runnable
{
	// Constants and variables
	// ------------------------------------------------------------------------

   private static Category cat = Category.getInstance(RMIRegistry.class);

   // the default port for RMI
   private static final int DEFAULT_RMI_PORT = 1099;

	// Constructor(s)
	// ------------------------------------------------------------------------

   /**
    * Constructs an RMI registry listening on the default RMI port -1099- for
    * SASL connections.
    *
    * @exception RemoteException thrown when the registry can not be started,
    * due to unavailabiliy of port 1099.
    */
   private RMIRegistry() throws RemoteException {
      this(DEFAULT_RMI_PORT);
   }

   /**
    * Constructs an RMI registry listening on the designated port for SASL
    * connections.
    *
    * @param port the port to start the registry on.
    * @exception RemoteException thrown when the registry can not be started,
    * due to an invalid or unavailable port.
    */
   private RMIRegistry(int port) throws RemoteException {
      this(port, true);
   }

   /**
    * Constructs an RMI registry.
    *
    * @param port the port to start the registry on.
    * @param secure whether or not the registry is started using the SASL RMI
    * socket factories.
    * @exception RemoteException if the registry can not be started due to an
    * invalid or unavailable port.
    */
   private RMIRegistry(int port, boolean secure) throws RemoteException {
      super();

      cat.info("Listen on port #"+String.valueOf(port)+" for "
         +(secure ? "" : "non-")+"SASL connection(s)...");

      if (secure)
         LocateRegistry.createRegistry(port,
                                       new SaslClientSocketFactory(),
                                       new SaslServerSocketFactory());
      else
         LocateRegistry.createRegistry(port);
   }

	// Class methods
	// ------------------------------------------------------------------------

   /**
    * Runs an RMI registry that listens on the port specified by the first
    * command line argument, or the default RMI port (1099) if no arguments
    * are present.<p>
    *
    * If a second argument is present, it is taken as the name of a system
    * environment variable that holds the string representation of a boolean
    * value. If the value is <tt>true</tt> then the RMI Registry will
    * authenticate connections to it using SASL mechanisms, otherise, if the
    * value is <tt>false</tt> then default sockets will be used.<p>
    *
    * <b>Note</b>: If the registry is to listen on the default RMI port but
    * should authenticate connections to it using SASL, then the token 1099
    * (the value of the port) should be present as the first argument.
    *
    * @param args command-line arguments.
    */
   public static final void main(String[] args)
   {
//      PropertyConfigurator.configureAndWatch("log.properties");
      RMIRegistry registry = null;
      //Security.addProvider(new cryptix.jce.provider.CryptixCrypto());
      System.setProperty(RMIParams.CALLBACK_HANDLER_CLASS, "com.igeekinc.indelible.client.executor.ExecutorCallbackHandler");

      BasicConfigurator.configure();
      try
      {
      File passwordDir = new File("/tmp/ExecutorPasswords");
      if (!passwordDir.exists())
        passwordDir.mkdir();
      File saslPasswordFile = new File(passwordDir, "executorPasswd.sasl");
      System.setProperty(SRPParams.PASSWORD_FILE, saslPasswordFile.getAbsolutePath());

      if (!saslPasswordFile.exists())
        saslPasswordFile.createNewFile();

      PasswordFile saslFile = new PasswordFile(saslPasswordFile);

      // Make sure that the "indelibleRMI" user is in the password file
      String user = "indelibleRMI";
      String password = "indelibleRMI";
      if (!saslFile.contains(user))
      {
        //cat.info("Adding RMI user...");
        byte[] testSalt = new byte[10];
        new Random().nextBytes(testSalt);
        saslFile.add(user, password, testSalt, "1");
        //cat.info("Added RMI user...");
      }
      else
      {
        //cat.info("Updating RMI user...");
        saslFile.changePasswd(user, password);
        //cat.info("Updated RMI user...");
      }
    saslFile.savePasswd();
      }
      catch (IOException e)
      {
        e.printStackTrace();
        return;
      }
      Sasl.setSaslClientFactory(new cryptix.sasl.ClientFactory());
      Sasl.setSaslServerFactory(new cryptix.sasl.ServerFactory());

      try {
         if (args != null) {
            if (args.length == 1)
               registry = new RMIRegistry(Integer.parseInt(args[0]));
            else if (args.length == 2) {
               boolean secure = Boolean.getBoolean(args[1]);
               registry = new RMIRegistry(Integer.parseInt(args[0]), secure);
            } else {
               cat.warn("Command line arguments ignored. Using defaults...");
               registry = new RMIRegistry();
            }
         }
         else
            registry = new RMIRegistry();
      } catch (RemoteException x) {
         cat.fatal("Could not instantiate RMI registry", x);
         throw new Error(String.valueOf(x));
      }

      final Thread t = new Thread(registry, "SASL RMI Registry");
      t.start();

      Runtime.getRuntime().addShutdownHook(
         new Thread() {
            public void run() {
               cat.info("Started shutdown...");
               if (t.isAlive())
                  try {
                     t.interrupt();
                     t.join();
                  } catch (Exception ignored) {
                     cat.warn("t.interrupt(). Ignored...", ignored);
                  }
               cat.info("Completed shutdown. Exiting...");
            }
         }
      );
   }

	// Instance methods
	// ------------------------------------------------------------------------

   /** Starts the registry. */
   public void run() {
      cat.info("RMIRegistry started on "+String.valueOf(new Date()));

      while (true) {
         try {
            Thread.sleep(5000);
         } catch (InterruptedException x) {
            break;
         } catch (Throwable x) {
            cat.fatal("run()", x);
            break;
         }
      }

      cat.info("RMIRegistry stopped on "+String.valueOf(new Date()));
   }
}