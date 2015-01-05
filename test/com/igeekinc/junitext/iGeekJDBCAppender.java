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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;

public class iGeekJDBCAppender extends org.apache.log4j.AppenderSkeleton
		implements org.apache.log4j.Appender
{

	/**
	 * URL of the DB for default connection handling
	 */
	protected String databaseURL = "jdbc:odbc:myDB";

	/**
	 * User to connect as for default connection handling
	 */
	protected String databaseUser = "me";

	/**
	 * User to use for default connection handling
	 */
	protected String databasePassword = "mypassword";

	/**
	 * Connection used by default. The connection is opened the first time it is
	 * needed and then held open until the appender is closed (usually at
	 * garbage collection). This behavior is best modified by creating a
	 * sub-class and overriding the <code>getConnection</code> and
	 * <code>closeConnection</code> methods.
	 */
	protected Connection connection = null;

	protected PreparedStatement loggingStatement;
	/**
	 * size of LoggingEvent buffer before writting to the database. Default is
	 * 1.
	 */
	protected int bufferSize = 1;

	/**
	 * ArrayList holding the buffer of Logging Events.
	 */
	protected ArrayList<LoggingEvent> buffer;

	/**
	 * Helper object for clearing out the buffer
	 */
	protected ArrayList<LoggingEvent> removes;

	protected int instanceID;
	
	public iGeekJDBCAppender()
	{
		super();
		buffer = new ArrayList<LoggingEvent>(bufferSize);
		removes = new ArrayList<LoggingEvent>(bufferSize);
	}

	/**
	 * Adds the event to the buffer. When full the buffer is flushed.
	 */
	public synchronized void append(LoggingEvent event)
	{
		buffer.add(event);

		if (buffer.size() >= bufferSize)
			flushBuffer();
	}

	/**
	 * Override this to return the connection to a pool, or to clean up the
	 * resource.
	 * 
	 * The default behavior holds a single connection open until the appender is
	 * closed (typically when garbage collected).
	 */
	protected void closeConnection(Connection con)
	{
	}

	/**
	 * Override this to link with your connection pooling system.
	 * 
	 * By default this creates a single connection which is held open until the
	 * object is garbage collected.
	 */
	protected Connection getConnection() throws SQLException
	{
		if (!DriverManager.getDrivers().hasMoreElements())
			setDriver("sun.jdbc.odbc.JdbcOdbcDriver");

		if (connection == null)
		{
			connection = DriverManager.getConnection(databaseURL, databaseUser,
					databasePassword);
		}

		return connection;
	}

	
	protected PreparedStatement getLoggingStatement() throws SQLException
	{
		if (loggingStatement == null)
		{
			loggingStatement = getConnection().prepareStatement("insert into logmessages(instanceid, time, thread, severity, class, detail) values("+instanceID+", ?, ?, ?, ?, ?)");
		}
		return loggingStatement;
	}
	/**
	 * Closes the appender, flushing the buffer first then closing the default
	 * connection if it is open.
	 */
	public void close()
	{
		flushBuffer();

		try
		{
			if (connection != null && !connection.isClosed())
				connection.close();
		} catch (SQLException e)
		{
			errorHandler.error("Error closing connection", e,
					ErrorCode.GENERIC_FAILURE);
		}
		this.closed = true;
	}

	/**
	 * loops through the buffer of LoggingEvents, gets a sql string from
	 * getLogStatement() and sends it to execute(). Errors are sent to the
	 * errorHandler.
	 * 
	 * If a statement fails the LoggingEvent stays in the buffer!
	 */
	public synchronized void flushBuffer()
	{
		// Do the actual logging
		removes.ensureCapacity(buffer.size());
		PreparedStatement logStatement;
		try
		{
			logStatement = getLoggingStatement();
		} catch (SQLException e1)
		{
			errorHandler.error("Could not allocate prepared statement or connection", e1,
					ErrorCode.FLUSH_FAILURE);
			return;
		}
		for (Iterator<LoggingEvent> i = buffer.iterator(); i.hasNext();)
		{
			try
			{
				LoggingEvent logEvent = i.next();
				try
				{

					logStatement.setTimestamp(1, new java.sql.Timestamp(logEvent.timeStamp) );
					String threadName = logEvent.getThreadName();
					logStatement.setString(2, threadName);
					if (threadName.length() > 50)
						threadName = threadName.substring(0, 50);
					String severity = logEvent.getLevel().toString();
					if (severity.length() > 10)
						severity = severity.substring(0, 10);
					logStatement.setString(3, severity);
					String loggerName = logEvent.getLoggerName();
					if (loggerName.length() > 300)
						loggerName = loggerName.substring(0, 300);
					logStatement.setString(4, loggerName);
					String message = logEvent.getMessage().toString();
					message = removeNulls(message);   // Filter out any nulls - the com.igeekinc.util.AES test has a nasty habit of slipping them into the message and this break the insert
                    logStatement.setBytes(5, message.getBytes("UTF-8"));
					logStatement.execute();
				} catch (SQLException e)
				{
					loggingStatement = null;
					connection = null;
					throw e;
				} catch (UnsupportedEncodingException e)
				{
					throw new SQLException("Couldn't get UTF-8 char set");
				}

				removes.add(logEvent);
			} catch (SQLException e)
			{
				errorHandler.error("Failed to excute sql", e,
						ErrorCode.FLUSH_FAILURE);
			}
		}

		// remove from the buffer any events that were reported
		buffer.removeAll(removes);

		// clear the buffer of reported events
		removes.clear();
	}

	/** closes the appender before disposal */
	public void finalize()
	{
		close();
	}

	/**
	 * JDBCAppender requires a layout.
	 * */
	public boolean requiresLayout()
	{
		return true;
	}

	public void setUser(String user)
	{
		databaseUser = user;
	}

	public void setURL(String url)
	{
		databaseURL = url;
	}

	public void setPassword(String password)
	{
		databasePassword = password;
	}

	public void setBufferSize(int newBufferSize)
	{
		bufferSize = newBufferSize;
		buffer.ensureCapacity(bufferSize);
		removes.ensureCapacity(bufferSize);
	}

	public String getUser()
	{
		return databaseUser;
	}

	public String getURL()
	{
		return databaseURL;
	}

	public String getPassword()
	{
		return databasePassword;
	}

	public int getBufferSize()
	{
		return bufferSize;
	}

	public void setInstanceID(int instanceID)
	{
		this.instanceID = instanceID;
	}
	
	public int getInstanceID()
	{
		return instanceID;
	}
	
	/**
	 * Ensures that the given driver class has been loaded for sql connection
	 * creation.
	 */
	public void setDriver(String driverClass)
	{
		try
		{
			Class.forName(driverClass);
		} catch (Exception e)
		{
			errorHandler.error("Failed to load driver", e,
					ErrorCode.GENERIC_FAILURE);
		}
	}

	public String removeNulls(String removeString)
	{
		StringBuffer returnStringBuf = new StringBuffer();
		for (int curPos = 0; curPos < removeString.length(); curPos++)
		{
			if (removeString.charAt(curPos) != 0)
				returnStringBuf.append(removeString.charAt(curPos));
		}
		return returnStringBuf.toString();
	}
}
