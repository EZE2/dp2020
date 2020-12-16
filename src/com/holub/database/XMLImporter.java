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

import com.holub.tools.ArrayIterator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;

public class XMLImporter implements Table.Importer
{	private BufferedReader  in;			// null once end-of-file reached
	private String[]        columnNames;
	private String          tableName;

	public XMLImporter(Reader in )
	{	this.in = in instanceof BufferedReader
						? (BufferedReader)in
                        : new BufferedReader(in)
	                    ;
	}
	public void startTable()			throws IOException
	{	in.readLine(); // 첫 줄은 버림.
		tableName   = in.readLine().trim().replaceAll("<|>", "");
		String columnLine = in.readLine().trim().replaceAll("(<cols>)|(</cols>)","");
		columnNames = columnLine.replaceAll("<","").replaceAll("/>","```").trim().split("```");
	}
	public String loadTableName()		throws IOException
	{	return tableName;
	}
	public int loadWidth()			    throws IOException
	{	return columnNames.length;
	}
	public Iterator loadColumnNames()	throws IOException
	{	return new ArrayIterator(columnNames);
	}

	public Iterator loadRow()			throws IOException
	{	Iterator row = null;
		if( in != null )
		{	String line = in.readLine();

		// XMLImporter에서는 null 대신 </테이블명>의 양식으로 파일이 끝남. 따라서 이 부분을 핸들링함.
			if( line.equals(String.format("</%s>", tableName)))
				in = null;
			else {
				// 각 값의 리스트를 넣어야 함.
				String rowdata = line.trim().replaceAll("(<row>)|(</row>)", "");

				rowdata = rowdata.replaceAll("<.*?>","``");
				rowdata = rowdata.substring(2, rowdata.length()-2);
				row = new ArrayIterator(rowdata.split("````"));
//				row = new ArrayIterator(rowdata.replaceAll("<.*?>","``").replaceAll("````","``").split("``"));
			}
		}
		return row;
	}

	public void endTable() throws IOException {}
}
