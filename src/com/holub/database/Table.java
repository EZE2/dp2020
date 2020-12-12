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
package com.holub.database;

import java.io.*;
import java.util.*;
import com.holub.database.Selector;

public interface Table extends Serializable, Cloneable
{
	Object clone() throws CloneNotSupportedException;

	String  name();

	void rename( String newName );

	boolean isDirty();

	int  insert( String[] columnNames, Object[] values );

	int  insert( Collection columnNames, Collection values );

	int  insert( Object[]	values );

	int  insert( Collection values );

	int  update( Selector where );

	int  delete( Selector where );

	public void begin();

	public void commit( boolean all ) throws IllegalStateException;

	public void rollback( boolean all ) throws IllegalStateException;

	public static final boolean THIS_LEVEL = false;

	public static final boolean ALL	= true;


	Table select(Selector where, String[] requestedColumns, Table[] other);

	Table select(Selector where, String[] requestedColumns );

	Table select(Selector where);

	Table select(Selector where, Collection requestedColumns,
												Collection other);

	Table select(Selector where, Collection requestedColumns );

	Cursor rows();

	void export( Table.Exporter importer ) throws IOException;

	public interface Exporter				//{=Table.Exporter}
	{	public void startTable()			throws IOException;
		public void storeMetadata(
					String tableName,
					int width,
					int height,
					Iterator columnNames )	throws IOException;
		public void storeRow(Iterator data) throws IOException;
		public void endTable()			 	throws IOException;
	}

	public interface Importer				//{=Table.Importer}
	{	void 	 startTable()		throws IOException;
		String   loadTableName()	throws IOException;
		int 	 loadWidth()		throws IOException;
		Iterator loadColumnNames()	throws IOException;
		Iterator loadRow()			throws IOException;
		void 	 endTable()			throws IOException;
	}
}
