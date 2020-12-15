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

public class TableFactory
{	
	public static Table create( String name, String[] columns )
	{	return new ConcreteTable( name, columns );
	}

	public static Table create( Table.Importer importer )
												throws IOException
	{	return new ConcreteTable( importer );
	}

	public static Table load( String name ) throws IOException
	{	return load( name, new File(".") );
	} 

	public static Table load( String name, String location )
												throws IOException
	{	return load( name, new File(location) );
	} 

	/*
	여기서 최초로 임포트 할수있는 데이터베이스들 추가할 수 있음.
	현재는 csv만 존재	 */
	public static Table load( String name, File directory )
													throws IOException
	{
		if( !(name.endsWith( ".csv" ) || name.endsWith( ".CSV" )) )
			throw new java.io.IOException(
					 "Filename (" +name+ ") does not end in "
					+"supported extension (.csv)" );

		Reader in = new FileReader( new File( directory, name ));
		Table loaded = new ConcreteTable( new CSVImporter( in ));
		in.close();
		return loaded;
	}
}
