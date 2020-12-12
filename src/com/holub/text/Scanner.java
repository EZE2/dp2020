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

import java.util.Iterator;
import java.io.*;
import com.holub.text.ParseFailure;

public class Scanner
{	private Token			currentToken	= new BeginToken();
	private BufferedReader	inputReader 	= null;
	private int		 		inputLineNumber = 0;
	private String	 		inputLine     	= null;
	private int				inputPosition 	= 0;

	private TokenSet tokens;

	public Scanner( TokenSet tokens, String input )
	{	this( tokens, new StringReader(input) );	
	}

	public Scanner( TokenSet tokens, Reader inputReader )
	{	this.tokens 	 = tokens;
		this.inputReader =
			(inputReader instanceof BufferedReader)
				? (BufferedReader) inputReader
				: new BufferedReader( inputReader )
				;
		loadLine();
	}

	private boolean loadLine()
	{	try
		{	inputLine = inputReader.readLine();
			if( inputLine != null )
			{	++inputLineNumber;
				inputPosition = 0;
			}
			return inputLine != null;
		}
		catch( IOException e )
		{	return false;
		}
	}

	public boolean match( Token candidate )
	{	return currentToken == candidate;
	}

	public Token advance() throws ParseFailure
	{	try
		{	
			if( currentToken != null )	// not at end of file
			{	
				inputPosition += currentToken.lexeme().length();
				currentToken   = null;

				if( inputPosition == inputLine.length() )
					if( !loadLine() )
						return null;

				while( Character.isWhitespace(
								inputLine.charAt(inputPosition)) )
					if( ++inputPosition == inputLine.length() )
						if( !loadLine() )
							return null;

				for( Iterator i = tokens.iterator(); i.hasNext(); ) //{=Scanner.search}
				{	Token t = (Token)(i.next());
					if( t.match(inputLine, inputPosition) )
					{	currentToken = t;
						break;
					}
				}

				if( currentToken == null )
					throw failure("Unrecognized Input");
			}
		}
		catch( IndexOutOfBoundsException e ){ /* nothing to do */ }
		return currentToken;
	}

	public ParseFailure failure( String message )
	{ 	return new ParseFailure(message,
						inputLine, inputPosition, inputLineNumber);
	}

	public String matchAdvance( Token candidate ) throws ParseFailure
	{	if( match(candidate) )
		{	String lexeme = currentToken.lexeme();
			advance();
			return lexeme;
		}
		return null;
	}

	public final String required( Token candidate ) throws ParseFailure
	{	String lexeme =	matchAdvance(candidate);
		if( lexeme == null )
			throw failure(
					"\"" + candidate.toString() + "\" expected.");
		return lexeme;
	}

	/*--------------------------------------------------------------*/
	public static class Test
	{
		private static TokenSet tokens = new TokenSet();

		private static final Token
			COMMA		= tokens.create( "'," 			 	),
			IN			= tokens.create( "'IN'" 		 	),
			INPUT		= tokens.create( "INPUT"		 	),
			IDENTIFIER	= tokens.create( "[a-z_][a-z_0-9]*" );

		public static void main(String[] args) throws ParseFailure
		{
			assert COMMA 	  instanceof SimpleToken: "Factory Failure 1";
			assert IN 	  	  instanceof WordToken  : "Factory Failure 2";
			assert INPUT 	  instanceof WordToken  : "Factory Failure 3";
			assert IDENTIFIER instanceof RegexToken : "Factory Failure 4";

			Scanner analyzer = new Scanner( tokens, ",aBc In input inputted" );

			assert analyzer.advance() == COMMA 	 	: "COMMA unrecognized";
			assert analyzer.advance() == IDENTIFIER : "ID unrecognized";
			assert analyzer.advance() == IN 		: "IN unrecognized";
			assert analyzer.advance() == INPUT 	 	: "INPUT unrecognized";
			assert analyzer.advance() == IDENTIFIER : "ID unrecognized 1";

			analyzer = new Scanner(tokens, "Abc IN\nCde");
			analyzer.advance(); // advance to first token.

			assert( analyzer.matchAdvance(IDENTIFIER).equals("Abc") );
			assert( analyzer.matchAdvance(IN).equals("in")  );
			assert( analyzer.matchAdvance(IDENTIFIER).equals("Cde") );

			// Deliberately force an exception toss
			analyzer = new Scanner(tokens, "xyz\nabc + def");
			analyzer.advance();
			analyzer.advance();
			try
			{	analyzer.advance(); // should throw an exception
				assert false : "Error Detection Failure";
			}
			catch( ParseFailure e )
			{	assert e.getErrorReport().equals(
											  "Line 2:\n"
											+ "abc + def\n"
											+ "____^\n"	);
			}

			System.out.println("Scanner PASSED");

			System.exit(0);
		}
	}
}
