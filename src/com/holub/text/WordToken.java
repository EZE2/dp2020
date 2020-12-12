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
package com.holub.text;

import java.util.*;
import java.util.regex.*;

public class WordToken implements Token
{	
	private final String 	pattern;

	public WordToken( String pattern )
	{	this.pattern = pattern.toLowerCase();
	}

	public boolean match(String input, int offset)
	{
		// Check that the input matches the patter in a
		// case-insensitive way. If you don't want case
		// insenstivity, use the following, less complicated code:
		//
		// if( !input.toLowerCase().startsWith(pattern, offset) )
		//	  return false;

		if( (input.length() - offset) < pattern.length() )
			return false;

		String candidate = input.substring( offset,
										offset+pattern.length() );
		if( !candidate.equalsIgnoreCase(pattern) )
			return false;

		// Return true if the lexeme is at the end of the
		// input string or if the character following the
		// lexeme is not a letter or digit.

		return 	   ((input.length() - offset) == pattern.length())
				|| (!Character.isLetterOrDigit( 
						input.charAt(offset + pattern.length()) ));
	}

	public String lexeme()	{ return pattern; }
	public String toString(){ return pattern; }
}
