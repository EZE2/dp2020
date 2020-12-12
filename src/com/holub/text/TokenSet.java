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

public class TokenSet
{
	private Collection members = new ArrayList();

	public Iterator iterator()
	{	return members.iterator();
	}

	public Token create( String spec )
	{	Token token;
		int start = 1;

		if( !spec.startsWith("'") )
		{	if( containsRegexMetacharacters(spec) )
			{
				token = new RegexToken( spec );
				members.add(token);
				return token;
			}

			--start;	// don't compensate for leading quote

			// fall through to the "quoted-spec" case
		}
		
		int end = spec.length();

		if( start==1 &&  spec.endsWith("'") ) // saw leading '
			--end;

		token = Character.isJavaIdentifierPart(spec.charAt(end-1))
				? (Token) new WordToken  ( spec.substring(start,end) )
				: (Token) new SimpleToken( spec.substring(start,end) )
				;

		members.add( token );
		return token;
	}

	private static final boolean containsRegexMetacharacters(String s)
	{	// This method could be implemented more efficiently,
		// but its not called very often.
		Matcher m = metacharacters.matcher(s);
		return m.find();
	}
	private static final Pattern metacharacters =
							Pattern.compile("[\\\\\\[\\]{}$\\^*+?|()]");
}
