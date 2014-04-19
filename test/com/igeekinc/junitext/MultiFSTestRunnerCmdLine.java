/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * @Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.@
 */
 
package com.igeekinc.junitext;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.ResultPrinter;
import junit.textui.TestRunner;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import com.igeekinc.util.ClientFile;
import com.igeekinc.util.SystemInfo;

public class MultiFSTestRunnerCmdLine extends TestRunner
{
    private static final String kSikuliBundleExtension = ".sikuli";

    Session    mailSession;
    
    public static final String kLayoutPattern="%d %-6r [%t] %p %c %x - %m%n";
    public MultiFSTestRunnerCmdLine()
    {
        super(new LoggerResultPrinter(System.out));
        Properties   mailProperties = new Properties();
        mailProperties.put("mail.smtp.host","pop.igeekinc.com");
        mailSession = Session.getDefaultInstance(mailProperties,null);
    }

    public MultiFSTestRunnerCmdLine(PrintStream writer)
    {
        super(writer);
        // TODO Auto-generated constructor stub
    }

    public MultiFSTestRunnerCmdLine(ResultPrinter printer)
    {
        super(printer);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param args
     */
    public static void main(String args[]) 
    {
        Logger root = Logger.getRootLogger();
        root.addAppender(new ConsoleAppender(
               new PatternLayout(kLayoutPattern)));
        Logger.getRootLogger().setLevel(Level.WARN);
        MultiFSTestRunnerCmdLine aTestRunner= new MultiFSTestRunnerCmdLine();
        try {
            TestResult r= aTestRunner.start(args);
            if (!r.wasSuccessful()) 
                System.exit(FAILURE_EXIT);
            System.exit(SUCCESS_EXIT);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(EXCEPTION_EXIT);
        }
    }
    
    VolumeInfo [] volumeInfo;
    
    public Test getTest(String suiteClassName)
    {
        Test returnTest = super.getTest(suiteClassName);
        if (returnTest instanceof FSTest)
        {
            
            MultiFSTestUtilities.setupFSTest((FSTest)returnTest, volumeInfo);
        }
        if (returnTest instanceof TestSuite)
            setupSuite((TestSuite)returnTest);
        return returnTest;
    }
    
    public void setupSuite(TestSuite suiteToSetup)
    {
        Enumeration tests = suiteToSetup.tests();
        while (tests.hasMoreElements())
        {
            Test myTest = (Test)tests.nextElement();
            if (myTest instanceof TestSuite)
                setupSuite((TestSuite)myTest);
            if (myTest instanceof com.igeekinc.junitext.FSTest)
            {
                MultiFSTestUtilities.setupFSTest((FSTest)myTest, volumeInfo);
            }
        }
    }
    
    public TestResult start(String[] args) throws Exception
    {
        LongOpt [] longOptions = {
                new LongOpt("path", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
                new LongOpt("suite", LongOpt.REQUIRED_ARGUMENT, null, 's'),
                new LongOpt("sikuli", LongOpt.REQUIRED_ARGUMENT, null, 'k'),
                //new LongOpt("logdir", LongOpt.OPTIONAL_ARGUMENT, null, 'l'),
                new LongOpt("logdir", LongOpt.REQUIRED_ARGUMENT, null, 'l'),
                new LongOpt("mailto", LongOpt.REQUIRED_ARGUMENT, null, 'm'),
                new LongOpt("dbURL", LongOpt.REQUIRED_ARGUMENT, null, 'd'),
                new LongOpt("dbUser", LongOpt.REQUIRED_ARGUMENT, null, 'u'),
                new LongOpt("dbPass", LongOpt.REQUIRED_ARGUMENT, null, 'y'),
                new LongOpt("runID", LongOpt.REQUIRED_ARGUMENT, null, 'r'),
                new LongOpt("logconf", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
        };
       // Getopt getOpt = new Getopt("MultiFSTestRunner", args, "p:ns:", longOptions);
        Getopt getOpt = new Getopt("MultiFSTestRunner", args, "d32p:ns:l:", longOptions);
        int c;

        String suiteName = null;
        String sikuliBundlePath = null, sikuliFileName = null;
        String mailToAddr = null;
        ArrayList<VolumeInfo> volumeInfoList = new ArrayList<VolumeInfo>();
        File logDir = new File(".");
        String dbURL = "", dbUser="", dbPass = "";
        Integer runID = null;
        while ((c = getOpt.getopt()) != -1)
        {
            switch(c)
            {
            case 'p':
                String curPath = getOpt.getOptarg();
                try
                {
                    ClientFile fileForPath = SystemInfo.getSystemInfo().getClientFileForPath(curPath);
                    if (fileForPath != null)
                    {
                        VolumeInfo addInfo = new VolumeInfo();
                        addInfo.setSelected(true);
                        addInfo.setVolume(fileForPath.getVolume());
                        addInfo.setSelectedPath(fileForPath.getFilePath());
                        volumeInfoList.add(addInfo);
                    }
                } catch (IOException e)
                {
                    System.err.println("Got IOException for "+curPath);
                    e.printStackTrace();
                    System.exit(-1);
                }
                break;
            case 's':
                suiteName = getOpt.getOptarg();
                break;
            case 'k':
                sikuliBundlePath = getOpt.getOptarg();
                break;
            case 'l':
            	logDir = new File(getOpt.getOptarg());
            	if (!logDir.exists())
            	{
            		System.err.println("Specified log directory "+logDir+" does not exists");
            		System.exit(-1);
            	}
            	if (!logDir.isDirectory())
            	{
            		System.err.println("Specified log directory "+logDir+" exists and is not a directory");
            		System.exit(-1);
            	}
            	break;
            case 'm':
            	mailToAddr = getOpt.getOptarg();
            	break;
            case 'd':
                dbURL = getOpt.getOptarg();
                break;
            case 'u':
                dbUser = getOpt.getOptarg();
                break;
            case 'y':
                dbPass = getOpt.getOptarg();
                break;
            case 'r':
                try
                {
                    runID = Integer.parseInt(getOpt.getOptarg());
                }
                catch (NumberFormatException e)
                {
                    System.err.println("Supplied instanceID "+getOpt.getOptarg()+" is not an integer");
                }
                break;
            case 'c':
            	File logConfFile = new File(getOpt.getOptarg());
            	if (!logConfFile.exists())
            	{
            		System.err.println("Specified log configuration file '"+logConfFile.getAbsolutePath()+"' does not exists");
            		System.exit(-1);
            	}
            	PropertyConfigurator.configure(logConfFile.getAbsolutePath());
            	break;
            }
        }

        if ((suiteName == null || suiteName.length() == 0) && (sikuliBundlePath == null || sikuliBundlePath.length() == 0))
        {
        	showUsage();
        	System.exit(-1);
        }

        if (suiteName != null && sikuliBundlePath != null)
        {
            System.err.println("Cannot specify both a suitename and a Sikuli bundle");
            showUsage();
            System.exit(-1);
        }
        
        if (sikuliBundlePath != null)
        {
            File sikuliBundleFile = new File(sikuliBundlePath);
            if (!sikuliBundleFile.exists())
            {
                System.err.println("Sikuli bundle '"+sikuliBundlePath+"' does not exist");
                System.exit(-1);
            }
            
            if (!sikuliBundleFile.isDirectory())
            {
                System.err.println("Sikuli bundle '"+sikuliBundlePath+"' is not a directory");
                System.exit(-1);
            }
            
            String sikuliBundleName = sikuliBundleFile.getName();
            if (!sikuliBundleName.endsWith(kSikuliBundleExtension))
            {
                System.err.println("Sikuli bundle '"+sikuliBundlePath+"' is not a Sikuli bundle");
                System.exit(-1);
            }
            sikuliBundleName = sikuliBundleName.substring(0, sikuliBundleName.length() - kSikuliBundleExtension.length());
            sikuliFileName = new File(sikuliBundlePath, sikuliBundleName + ".py").getAbsolutePath();
            
        }
        if (dbURL.length() > 0 && (dbUser.length() == 0 || dbPass.length() == 0))
        {
            System.err.println("Error: dbURL was specified but no dbUser or dbPass");
            showUsage();
            System.exit(-1);
        }

        
        Connection dbConnection = null;
        if (dbURL.length() > 0)
        {
            dbConnection = DriverManager.getConnection(dbURL, dbUser, dbPass);
        }
        
        boolean createdRunID = false;
        
        if (dbURL.length() > 0 && runID == null)
        {
            System.err.println("No runID specified - creating runID");
            PreparedStatement testRunCreateStmt = dbConnection.prepareStatement("insert into testrun (softwareundertest, versionmajor, versionminor, versionpoint, teststart) values(?, ?, ?, ?, ?)");
            testRunCreateStmt.setString(1, suiteName);
            testRunCreateStmt.setString(2, "1");
            testRunCreateStmt.setString(3, "0");
            testRunCreateStmt.setString(4, "0");
            testRunCreateStmt.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
            testRunCreateStmt.execute();
            
            Statement getTestRunIDStmt = dbConnection.createStatement();
            ResultSet testRunIDRS = getTestRunIDStmt.executeQuery("select currval('testrun_testrunid_seq')");
            if (testRunIDRS.next())
            {
                runID = testRunIDRS.getInt(1);
                createdRunID = true;
            }
        }
        
        int instanceID = 0;
        iGeekJDBCAppender dbAppender = null;
        if (dbConnection != null)
        {
            PreparedStatement insertInstanceRunStmt = dbConnection.prepareStatement("insert into instance (suitename, hostname, ostype, osmajorversion, osminorversion, ospointversion, architecture, instancestart, testrunid) values(?, ?, ?, ?, ?, ?, ?, ?, ?)");
            insertInstanceRunStmt.setString(1, suiteName);
            java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
            String hostname = addr.getHostName();
            insertInstanceRunStmt.setString(2, hostname);
            
            insertInstanceRunStmt.setString(3, System.getProperty("os.name"));
            String osVersion = System.getProperty("os.version");
            StringTokenizer versionTokenizer = new StringTokenizer(osVersion, ".");
            
            insertInstanceRunStmt.setString(4, versionTokenizer.nextToken());
            insertInstanceRunStmt.setString(5, versionTokenizer.nextToken());
            insertInstanceRunStmt.setString(6, versionTokenizer.nextToken());
            insertInstanceRunStmt.setString(7, System.getProperty("os.arch"));
            insertInstanceRunStmt.setTimestamp(8, new java.sql.Timestamp(System.currentTimeMillis()));
            insertInstanceRunStmt.setInt(9, runID);
            insertInstanceRunStmt.execute();
            
            Statement getInstanceIDStmt = dbConnection.createStatement();
            ResultSet instanceIDRS = getInstanceIDStmt.executeQuery("select currval('instance_instanceid_seq')");
            if (instanceIDRS.next())
            {
                instanceID = instanceIDRS.getInt(1);
            }
            instanceIDRS.close();

            dbConnection = DriverManager.getConnection(dbURL, dbUser, dbPass);
            dbAppender = new iGeekJDBCAppender();
            dbAppender.setURL(dbURL);
            dbAppender.setUser(dbUser);
            dbAppender.setPassword(dbPass);
            dbAppender.setInstanceID(instanceID);
            dbAppender.setBufferSize(100);
            dbAppender.activateOptions();
            Logger.getRootLogger().addAppender(dbAppender);
            
        }
        SimpleDateFormat formatter = new SimpleDateFormat("MMddyyyy'-'HHmmss'.testlog'");
        File logFile = new File(logDir, formatter.format(new Date()));
        Logger root = Logger.getRootLogger();
        FileAppender fileAppender = new FileAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN), logFile.getAbsolutePath());
		fileAppender.activateOptions();
        root.addAppender(fileAppender);
        Logger logger = Logger.getLogger(getClass());
		logger.warn("MultiFSTestRunnerCmdLine starting");
        logger.warn("Log file is "+logFile.getAbsolutePath());
        logger.warn("Classpath is "+System.getProperty("java.class.path"));
        if (suiteName != null)
            logger.warn("Executing suite name "+suiteName);
        if (sikuliBundlePath != null)
            logger.warn("Executing Sikuli bundle "+sikuliBundlePath+" python file = "+sikuliFileName);
        volumeInfo = new VolumeInfo[volumeInfoList.size()];
        volumeInfo = (VolumeInfo [])volumeInfoList.toArray(volumeInfo);
        try {
            Test suite = null;
            if (suiteName != null)
                suite = getTest(suiteName);
            if (sikuliBundlePath != null)
                suite = SikuliTestUtilities.genSikuliTestSuite(sikuliFileName, sikuliBundlePath);
            if (suite == null)
            {
                logger.warn("Could not find a test suite!");
                System.exit(-1);
            }
        	//SLG: Return Result and the path of the log
            TestResult result=doRun(suite, false);
            try {
				if (!result.wasSuccessful()) 
				{
					System.out.println("FAILLOG:"+ logFile.getAbsolutePath());
					if (mailToAddr != null) 
				    sendStatusMail(suiteName, mailToAddr, logFile, "failed");
				} 
				else 
				{
					if (mailToAddr != null) 
				        sendStatusMail(suiteName, mailToAddr, logFile, "passed");
					System.out.println("OKLOG:"+ logFile.getAbsolutePath());
				}
			} catch (Exception e) 
			{
				logger.error("Error sending email", e);
			}
			if (dbConnection != null)
			{
			    if (result instanceof iGeekTestResult)
			    {
			        iGeekTestResult myResult = (iGeekTestResult)result;
			        TestRunRecord [] testRunRecords = myResult.getTestRuns();
			        PreparedStatement updateInstanceRunStmt = dbConnection.prepareStatement("update instance set instanceend = ? where instanceid = ?");
			        
			        updateInstanceRunStmt.setTimestamp(1, new java.sql.Timestamp(myResult.getEndTime().getTime()));
			        updateInstanceRunStmt.setInt(2, instanceID);
			        updateInstanceRunStmt.execute();
			        PreparedStatement insertTestCaseStmt = dbConnection.prepareStatement("insert into testcase (class, function, volumeformatlist, teststart, testend, state, errorclass, errormessage, instanceid) values(?, ?, ?, ?, ?, ?, ?, ?, "+instanceID+")");
			        PreparedStatement insertVolumeUsedStmt = dbConnection.prepareStatement("insert into volumeused(description, mountpoint, selectedpath, fstype, testid) values(?, ?, ?, ?, ?)");
			        PreparedStatement insertStackTraceElementStmt = dbConnection.prepareStatement("insert into stacktrace (traceelement, filename, classname, method, linenumber, testid) values(?, ?, ?, ?, ?, ?)");
			        for (TestRunRecord curRecord:testRunRecords)
			        {
			            insertTestCaseStmt.setString(1, curRecord.getTest().getClass().getName());
			            insertTestCaseStmt.setString(2, curRecord.getTest().getName());
			            insertTestCaseStmt.setString(3, curRecord.getVolumeFormatList());
			            insertTestCaseStmt.setTimestamp(4, new Timestamp(curRecord.getStart().getTime()));
			            insertTestCaseStmt.setTimestamp(5, new Timestamp(curRecord.getEnd().getTime()));
			            String state = "", errorClass = "", errorMessage = "";
                        Throwable curError = curRecord.getError();
                        if (curError != null)
                        {
                            errorClass = curError.getClass().toString();
                            errorMessage = curError.getMessage();
                        }
			            switch(curRecord.getState())
			            {
			            case Errors:
			                state = "E";
			                break;
			            case Failed:
			                state = "F";
			                break;
			            case Passed:
			                state = "P";
			                break;
			            }
			            insertTestCaseStmt.setString(6, state);
			            insertTestCaseStmt.setString(7, errorClass);
			            insertTestCaseStmt.setString(8, errorMessage);
			            insertTestCaseStmt.execute();
			            Statement getInstanceIDStmt = dbConnection.createStatement();
			            ResultSet testcaseIDRS = getInstanceIDStmt.executeQuery("select currval('testcase_testid_seq')");
			            if (testcaseIDRS.next())
			            {
			                int testcaseID = testcaseIDRS.getInt(1);


			                if (curError != null)
			                {
			                    StackTraceElement [] stackTrace = curError.getStackTrace();
			                    for (int curElementNum = 0; curElementNum < stackTrace.length; curElementNum++)
			                    {
			                        StackTraceElement curElement = stackTrace[curElementNum];
			                        insertStackTraceElementStmt.setInt(1, curElementNum);
			                        insertStackTraceElementStmt.setString(2, curElement.getFileName());
			                        insertStackTraceElementStmt.setString(3, curElement.getClassName());
			                        insertStackTraceElementStmt.setString(4, curElement.getMethodName());
			                        insertStackTraceElementStmt.setInt(5, curElement.getLineNumber());
			                        insertStackTraceElementStmt.setInt(6, testcaseID);
			                        insertStackTraceElementStmt.execute();
			                    }
			                }
			                for(TestRunVolume curTestRunVolume:curRecord.getTestRunVolumes())
			                {
			                    insertVolumeUsedStmt.setString(1, curTestRunVolume.getDescription());
			                    insertVolumeUsedStmt.setString(2, curTestRunVolume.getMountPoint());
			                    insertVolumeUsedStmt.setString(3, curTestRunVolume.getSelectedPath());
			                    insertVolumeUsedStmt.setString(4, curTestRunVolume.getFsType());
			                    insertVolumeUsedStmt.setInt(5, testcaseID);

			                    insertVolumeUsedStmt.execute();
			                }
			            }

			        }
			    }
			    
			    // We only fill in the time to end if we created the test run
			    if (createdRunID)
			    {
		            PreparedStatement testRunCreateStmt = dbConnection.prepareStatement("update testrun set testend=? where testrunid=?");
		            testRunCreateStmt.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
		            testRunCreateStmt.setInt(2, runID);
		            testRunCreateStmt.execute();
			    }
			    dbConnection.close();
			    if (dbAppender != null)
			    {
			        dbAppender.close();
			    }
			}
            return result;
        }
        catch(Exception e) 
        {
        	logger.error("Error executing test suite:"+suiteName, e);
            throw e;
        }
    }

    private void showUsage()
    {
        System.err.println("Usage: MultiFSTestRunnerCmdLine [--suite <suite to run>] [--sikuli <sikule bundle dir to run>] --logdir <directory to write log files to> [--mailto <send email here>] [--dbURL database for results] [--dbUser database user] [--dbPass database password] [--instanceID instance ID of the run ]");
    }

	private void sendStatusMail(String suiteName, String mailToAddr,
			File logFile, String status) throws MessagingException,
			AddressException 
	{
		Message msg = new MimeMessage(mailSession);
		msg.setFrom(new InternetAddress("test@igeekinc.com"));
		InternetAddress[] address = {new InternetAddress(mailToAddr)};
		msg.setRecipients(Message.RecipientType.TO, address);
		Date d=new Date();
		SimpleDateFormat sdf =new SimpleDateFormat("yyyy.mm.dd. HH:mm");
		String dstring=sdf.format(d);

		msg.setSubject(dstring + " " + suiteName + " "+ status);
		msg.setSentDate(d);
		msg.setText("Log file is "+logFile.getAbsolutePath() + "\n" +  "Executing suite name "+suiteName);
		Transport.send(msg);
	}

    @Override
    protected TestResult createTestResult()
    {
        return new iGeekTestResult();
    }
}
