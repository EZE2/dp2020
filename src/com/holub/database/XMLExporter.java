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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

public class XMLExporter implements Table.Exporter
{	private final Writer out;
	private 	  int	 width;
	private String tableName = null;
	private ArrayList<String> columnNames = new ArrayList<>();


	public XMLExporter(Writer out )
	{	this.out = out;
	}

	public void storeMetadata( String tableName,
							   int width,
							   int height,
							   Iterator columnNames ) throws IOException

	{	this.width = width;
		this.tableName = tableName == null ? "<anonymous>" : tableName;
		columnNames.forEachRemaining(c -> this.columnNames.add((String) c));
		String tableHeader = String.format("<%s>", this.tableName);
		out.write(tableHeader);
		out.write("\n");

		// Import의 편의성을 위한 열 데이터 저장
		storeColumn();
	}

	private void storeColumn()  throws IOException{
		out.write("<cols>");
		for (String s : this.columnNames){
			out.write(String.format("<%s/>", s));
		}
		out.write("</cols>\n");
	}

	public void storeRow( Iterator data ) throws IOException
	{	int i = width;
		int idx = 0;
		out.write("<row>");
		while( data.hasNext() )
		{	Object datum = data.next();

			// Null columns are represented by an empty field
			// (two commas in a row). There's nothing to write
			// if the column data is null.
			if( datum != null )
				out.write(String.format("<%s>", this.columnNames.get(idx)));
				out.write( datum.toString() );
				out.write(String.format("</%s>", this.columnNames.get(idx)));

//			if( --i > 0 )
//				out.write(String.format("<%s>", this.columnNames.get(idx)));
//				out.write("NULL");
//				out.write(String.format("</%s>", this.columnNames.get(idx)));
			idx++;
		}
		out.write("</row>\n");
	}

	public void startTable() throws IOException {
		/*XML 테이블을 작성하기 위해서, 필요한 앞 부분을 만드는 과정 */
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
	}
	public void endTable()   throws IOException {
		/*XML 테이블을 작성하기 위해서, 필요한 뒷부분을 만드는 과정*/
		out.write(String.format("</%s>", this.tableName));
	}
}
