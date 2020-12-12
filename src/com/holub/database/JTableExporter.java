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
import javax.swing.*;

public class JTableExporter implements Table.Exporter
{
	private String[]	columnHeads;
	private Object[][]	contents;
	private int			rowIndex = 0;

	public void startTable() throws IOException { rowIndex = 0;	}

	public void storeMetadata( String tableName,
							   int width,
							   int height,
							   Iterator columnNames ) throws IOException
	{
		contents	= new Object[height][width];
		columnHeads = new String[width];

		int columnIndex = 0;
		while( columnNames.hasNext() )
			columnHeads[columnIndex++] = columnNames.next().toString();
	}

	public void storeRow( Iterator data ) throws IOException
	{	int columnIndex = 0;
		while( data.hasNext() )
			contents[rowIndex][columnIndex++] = data.next();
		++rowIndex;
	}

	public void endTable() throws IOException {/*nothing to do*/}

	public JTable getJTable()
	{	return new JTable( contents, columnHeads );
	}

	public static class Test
	{ 	public static void main( String[] args ) throws IOException
		{	
			Table people = TableFactory.create( "people",
						   new String[]{ "First", "Last"		} );
			people.insert( new String[]{ "Allen",	"Holub" 	} );
			people.insert( new String[]{ "Ichabod",	"Crane" 	} );
			people.insert( new String[]{ "Rip",		"VanWinkle" } );
			people.insert( new String[]{ "Goldie",	"Locks" 	} );

			javax.swing.JFrame frame = new javax.swing.JFrame();
			frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

			JTableExporter tableBuilder = new JTableExporter();
			people.export( tableBuilder );

			frame.getContentPane().add(
					new JScrollPane( tableBuilder.getJTable() ) );
			frame.pack();
			frame.setVisible( true );
		}
	}
}
