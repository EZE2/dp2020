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
import java.util.Iterator;

public class HTMLExporter implements Table.Exporter
{	private final Writer out;
	private 	  int	 width;

	public HTMLExporter(Writer out )
	{	this.out = out;
	}

	public void storeMetadata( String tableName,
							   int width,
							   int height,
							   Iterator columnNames ) throws IOException

	{	this.width = width;
		String _tableName = tableName == null ? "<anonymous>" : tableName;
		String tableHeader = String.format("<h2>%s</h2>", _tableName);
		out.write(tableHeader);
		out.write("\n");
		storeRow( columnNames ); // comma separated list of columns ids
	}

	public void storeRow( Iterator data ) throws IOException
	{	int i = width;
		out.write("<tr>");
		while( data.hasNext() )
		{	Object datum = data.next();

			// Null columns are represented by an empty field
			// (two commas in a row). There's nothing to write
			// if the column data is null.
			if( datum != null )
				out.write("<th>");
				out.write( datum.toString() );
				out.write("</th>");

		}
		out.write("</tr><br/>");
	}

	public void startTable() throws IOException {
		/*HTML 테이블을 작성하기 위해서, 필요한 앞 부분을 만드는 과정
		* 기존 csv익스포터는 이런 부분이 필요가 없었는데 여기는 필요함*/
		out.write("<html><table>\n");
	}
	public void endTable()   throws IOException {
		/*HTML 테이블을 작성하기 위해서, 필요한 뒷부분을 만드는 과정
		 * 기존 csv익스포터는 이런 부분이 필요가 없었는데 여기는 필요함*/
		out.write("</table></html>");
	}
}
