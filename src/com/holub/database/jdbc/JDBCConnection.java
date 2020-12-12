/*  (c) 2004 Allen I. Holub. All rights reserved.
 *
 *  This code may be used freely by yourself with the following
 *  restrictions:
 *
 *  o Your splash screen, about box, or equivalent, must include
 *    Allen Holub's name, copyright, and URL. For example:
 *
 *      This program contains Allen Holub's SQL package.<br>
 *      (c) 2005 Allen I. Holub. All Rights Reserved.<br>
 *              http://www.holub.com<br>
 *
 *    If your program does not run interactively, then the foregoing
 *    notice must appear in your documentation.
 *
 *  o You may not redistribute (or mirror) the source code.
 *
 *  o You must report any bugs that you find to me. Use the form at
 *    http://www.holub.com/company/contact.html or send email to
 *    allen@Holub.com.
 *
 *  o The software is supplied <em>as is</em>. Neither Allen Holub nor
 *    Holub Associates are responsible for any bugs (or any problems
 *    caused by bugs, including lost productivity or data)
 *    in any of this code.
 */
package com.holub.database.jdbc;

import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.*;

import com.holub.database.*;
import com.holub.database.jdbc.adapters.*;
import com.holub.text.ParseFailure;

public class JDBCConnection extends ConnectionAdapter
{
	private Database database;

	// Establish a connection to the indicated database.
	//
	public JDBCConnection(String uri) throws SQLException,
											 URISyntaxException,
											 IOException
	{	this( new URI(uri) );
	}

	public JDBCConnection(URI uri) throws	SQLException,
											IOException
	{	database = new Database( uri );
	}

	public void close() throws SQLException
	{	try
		{	
			autoCommitState.close();

			database.dump();
			database=null;	// make the memory reclaimable and
							// also force a nullPointerException
							// if anybody tries to use the
							// connection after it's closed.
		}
		catch(IOException e)
		{	throw new SQLException( e.getMessage() );
		}
	}

	public Statement createStatement() throws SQLException
	{	return new JDBCStatement(database);
	}

	public void commit() throws SQLException
	{	autoCommitState.commit();
	}

	public void rollback() throws SQLException
	{	autoCommitState.rollback();
	}

	public void setAutoCommit( boolean enable ) throws SQLException
	{	autoCommitState.setAutoCommit(enable);
	}

	public boolean getAutoCommit() throws SQLException
	{	return autoCommitState == enabled;
	}

	//----------------------------------------------------------------------
	private interface AutoCommitBehavior
	{	void close() throws SQLException;
		void commit() throws SQLException;
		void rollback() throws SQLException;
		void setAutoCommit( boolean enable ) throws SQLException;
	}

	private AutoCommitBehavior enabled =
		new AutoCommitBehavior()
		{	public void close() throws SQLException {/* nothing to do */}
			public void commit() 					{/* nothing to do */}
			public void rollback() 				 	{/* nothing to do */}
			public void setAutoCommit( boolean enable )
			{	if( enable == false )
				{	database.begin();
					autoCommitState = disabled;
				}
			}
		};

	private AutoCommitBehavior disabled = 
		new AutoCommitBehavior()
		{	public void close() throws SQLException
			{	try
				{	database.commit();
				}
				catch( ParseFailure e )
				{	throw new SQLException( e.getMessage() );
				}
			}
			public void commit() throws SQLException
			{	try
				{	database.commit();
					database.begin();
				}
				catch( ParseFailure e )
				{	throw new SQLException( e.getMessage() );
				}
			}
			public void rollback() throws SQLException
			{	try
				{	database.rollback();
					database.begin();
				}
				catch( ParseFailure e )
				{	throw new SQLException( e.getMessage() );
				}
			}
			public void setAutoCommit( boolean enable ) throws SQLException
			{	try
				{	if( enable == true )
					{	database.commit();
						autoCommitState = enabled;
					}
				}
				catch( ParseFailure e )
				{	throw new SQLException( e.getMessage() );
				}
			}
		};

	private AutoCommitBehavior autoCommitState = enabled;
}
