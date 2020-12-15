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

import java.util.*;
import java.io.*;
import java.text.NumberFormat;
import java.net.URI;

import com.holub.text.Token;
import com.holub.text.TokenSet;
import com.holub.text.Scanner;
import com.holub.text.ParseFailure;
import com.holub.tools.ThrowableContainer;

public final class Database
{	/* The directory that represents the database.
	 */
	private File 	  location     = new File(".");

	private int		  affectedRows = 0;


	private final Map tables = new TableMap( new HashMap() );

	private int transactionLevel = 0;

	private final class TableMap implements Map
	{ 		
		private final Map realMap;
		public TableMap( Map realMap ){	this.realMap = realMap; }

		public Object get( Object key )
		{	String tableName = (String)key;
			try
			{	Table desiredTable = (Table) realMap.get(tableName);
				if( desiredTable == null )
				{	desiredTable = TableFactory.load(
									tableName + ".csv",location);
					put(tableName, desiredTable);
				}
				return desiredTable;
			}
			catch( IOException e )
			{	// Can't use verify(...) or error(...) here because the
				// base-class "get" method doesn't throw any exceptions.
				// Kludge a runtime-exception toss. Call in.failure()
				// to get an exception object that calls out the
				// input file name and line number, then transmogrify
				// the ParseFailure to a RuntimeException.

				String message =
					"Table not created internally and couldn't be loaded."
											+"("+ e.getMessage() +")\n";
				throw new RuntimeException(
									in.failure( message ).getMessage() );
			}
		}
		
		public Object put(Object key, Object value)
		{	// If transactions are active, put the new
			// table into the same transaction state
			// as the other tables.
		
			for( int i = 0; i < transactionLevel; ++i )
				((Table)value).begin();

			return realMap.put(key,value);
		}

		public void putAll(Map m)
		{	throw new UnsupportedOperationException();
		}

		public int		size() 				{ return realMap.size(); 		}
		public boolean	isEmpty()			{ return realMap.isEmpty();		}
		public Object	remove(Object k)	{ return realMap.remove(k);		}
		public void		clear()				{		 realMap.clear();		}
		public Set		keySet()			{ return realMap.keySet();		}
		public Collection values()			{ return realMap.values();		}
		public Set		entrySet()			{ return realMap.entrySet();	}
		public boolean	equals(Object o)	{ return realMap.equals(o);		}
		public int		hashCode()			{ return realMap.hashCode();	}

		public boolean	containsKey(Object k)
		{	return realMap.containsKey(k);
		}
		public boolean	containsValue(Object v)
		{	return realMap.containsValue(v);
		}
	}

	//@token-start
	//--------------------------------------------------------------
	// The token set used by the parser. Tokens automatically
	// The Scanner object matches the specification against the
	// input in the order of creation. For example, it's important
	// that the NUMBER token is declared before the IDENTIFIER token
	// since the regular expression associated with IDENTIFIERS
	// will also recognize some legitimate numbers.

	private static final TokenSet tokens = new TokenSet();

	private static final Token
		COMMA		= tokens.create( "'," 		), //{=Database.firstToken}
		EQUAL		= tokens.create( "'=" 		),
		LP			= tokens.create( "'(" 		),
		RP 			= tokens.create( "')" 		),
		DOT			= tokens.create( "'." 		),
		STAR		= tokens.create( "'*" 		),
		SLASH		= tokens.create( "'/" 		),
		AND			= tokens.create( "'AND"		),
		BEGIN		= tokens.create( "'BEGIN"	),
		COMMIT		= tokens.create( "'COMMIT"	),
		CREATE		= tokens.create( "'CREATE"	),
		DATABASE	= tokens.create( "'DATABASE"),
		DELETE		= tokens.create( "'DELETE"	),
		DROP		= tokens.create( "'DROP"	),
		DUMP		= tokens.create( "'DUMP"	),
		FROM		= tokens.create( "'FROM"	),
		INSERT 		= tokens.create( "'INSERT"	),
		INTO 		= tokens.create( "'INTO"	),
		KEY 		= tokens.create( "'KEY"		),
		LIKE		= tokens.create( "'LIKE"	),
		NOT 		= tokens.create( "'NOT"		),
		NULL		= tokens.create( "'NULL"	),
		OR			= tokens.create( "'OR"		),
		PRIMARY		= tokens.create( "'PRIMARY"	),
		ROLLBACK	= tokens.create( "'ROLLBACK"),
		SELECT		= tokens.create( "'SELECT"	),
		SET			= tokens.create( "'SET"		),
		TABLE		= tokens.create( "'TABLE"	),
		UPDATE		= tokens.create( "'UPDATE"	),
		USE			= tokens.create( "'USE"		),
		VALUES 		= tokens.create( "'VALUES"	),
		WHERE		= tokens.create( "'WHERE"	),

		WORK		= tokens.create( "WORK|TRAN(SACTION)?"		),
		ADDITIVE	= tokens.create( "\\+|-" 					),
		STRING		= tokens.create( "(\".*?\")|('.*?')"		),
		RELOP		= tokens.create( "[<>][=>]?"				),
		NUMBER		= tokens.create( "[0-9]+(\\.[0-9]+)?"		),

		INTEGER		= tokens.create( "(small|tiny|big)?int(eger)?"),
		NUMERIC		= tokens.create( "decimal|numeric|real|double"),
		CHAR		= tokens.create( "(var)?char"				),
		DATE		= tokens.create( "date(\\s*\\(.*?\\))?"		),

		IDENTIFIER	= tokens.create( "[a-zA-Z_0-9/\\\\:~]+"		); //{=Database.lastToken}

	private String  expression;	// SQL expression being parsed
	private Scanner in;			// The current scanner.

	// Enums to identify operators not recognized at the token level
	// These are used by various inner classes, but must be declared
	// at the outer-class level because they're static.

	private static class  RelationalOperator{ private RelationalOperator(){} }
	private static final  RelationalOperator EQ = new RelationalOperator();
	private static final  RelationalOperator LT = new RelationalOperator();
	private static final  RelationalOperator GT = new RelationalOperator();
	private static final  RelationalOperator LE = new RelationalOperator();
	private static final  RelationalOperator GE = new RelationalOperator();
	private static final  RelationalOperator NE = new RelationalOperator();

	private static class MathOperator{ private MathOperator(){} }
	private static final MathOperator PLUS   = new MathOperator();
	private static final MathOperator MINUS  = new MathOperator();
	private static final MathOperator TIMES  = new MathOperator();
	private static final MathOperator DIVIDE = new MathOperator();

	//@declarations-end
	//--------------------------------------------------------------
	public Database() { }

	public Database( URI directory ) throws IOException
	{	useDatabase( new File(directory) );
	}

	public Database( File path ) throws IOException
	{	useDatabase( path );
	}

	public Database( String path ) throws IOException
	{	useDatabase( new File(path)  );
	}

	public Database( File path, Table[] database ) throws IOException
	{	useDatabase( path );
		for( int i = 0; i < database.length; ++i )
			tables.put( database[i].name(), database[i] );
	}

	//--------------------------------------------------------------
	// Private parse-related workhorse functions.

	private void error( String message ) throws ParseFailure
	{	throw in.failure( message.toString() );
	}

	private void verify( boolean test, String message ) throws ParseFailure
	{	if( !test )
			throw in.failure( message );
	}


	//--------------------------------------------------------------
	// Public methods that duplicate some SQL statements.
	// The SQL interpreter calls these methods to
	// do the actual work.

	public void useDatabase( File path ) throws IOException
	{	dump();
		tables.clear();	// close old database if there is one
		this.location = path;
	}

	public void createDatabase( String name ) throws IOException
	{	File location = new File( name );
		location.mkdir();
		this.location = location;
	}

	public void createTable( String name, List columns )
	{	String[] columnNames = new String[ columns.size() ];
		int i = 0;
		for( Iterator names = columns.iterator(); names.hasNext(); )
			columnNames[i++] = (String) names.next();

		Table newTable = TableFactory.create(name, columnNames);
		tables.put( name, newTable );
	}

	public void dropTable( String name )
	{	tables.remove( name );	// ignore the error if there is one.

		File tableFile = new File(location,name);
		if( tableFile.exists() )
			tableFile.delete();
	}

	// 실제 익스포트 메소드는 여기
	public void HTMLdump() throws IOException
	{	Collection values = tables.values();
		if( values != null )
		{	for( Iterator i = values.iterator(); i.hasNext(); )
		{	Table current = (Table ) i.next();
			if( current.isDirty() )
			{	Writer out =
					new FileWriter(
							new File(location, current.name() + ".html"));
				current.export( new HTMLExporter(out) );
				out.close();
			}
		}
		}
	}

	public void XMLdump() throws IOException
	{
		Collection values = tables.values();
		if( values != null )
		{	for( Iterator i = values.iterator(); i.hasNext(); )
		{	Table current = (Table ) i.next();
			if( current.isDirty() )
			{	Writer out =
					new FileWriter(
							new File(location, current.name() + ".xml"));
				current.export( new XMLExporter(out) );
				out.close();
			}
		}
		}
	}

	public void dump() throws IOException
	{	Collection values = tables.values();
		if( values != null )
		{	for( Iterator i = values.iterator(); i.hasNext(); )
			{	Table current = (Table ) i.next();
				if( current.isDirty() )
				{	Writer out =
						new FileWriter(
								new File(location, current.name() + ".csv"));
					current.export( new CSVExporter(out) );
					out.close();
				}
			}
		}
	}

	public int affectedRows()
	{	return affectedRows;
	}
	//@transactions-start
	//----------------------------------------------------------------------
	// Transaction processing.

	public void begin()
	{	++transactionLevel;

		Collection currentTables = tables.values();
		for( Iterator i = currentTables.iterator(); i.hasNext(); )
			((Table) i.next()).begin();
	}

	public void commit() throws ParseFailure
	{	
		assert transactionLevel > 0 : "No begin() for commit()";
		--transactionLevel;
		
		try
		{	Collection currentTables = tables.values();
			for( Iterator i = currentTables.iterator(); i.hasNext() ;)
				((Table) i.next()).commit( Table.THIS_LEVEL );
		}
		catch(NoSuchElementException e)
		{	verify( false, "No BEGIN to match COMMIT" );
		}
	}

	public void rollback() throws ParseFailure
	{	assert transactionLevel > 0 : "No begin() for commit()";
		--transactionLevel;
		try
		{	Collection currentTables = tables.values();

			for( Iterator i = currentTables.iterator(); i.hasNext() ;)
				((Table) i.next()).rollback( Table.THIS_LEVEL );
		}
		catch(NoSuchElementException e)
		{	verify( false, "No BEGIN to match ROLLBACK" );
		}
	}
	//@transactions-end
	//@parser-start

	public Table execute( String expression ) throws IOException, ParseFailure
	{	try
		{	this.expression   = expression;
			in				  = new Scanner(tokens, expression);
			in.advance();	// advance to the first token.
			return statement();
		}
		catch( ParseFailure e )
		{	if( transactionLevel > 0 )
				rollback();
			throw e;
		}
		catch( IOException e )
		{	if( transactionLevel > 0 )
				rollback();
			throw e;
		}
	}

	private Table statement() throws ParseFailure, IOException
	{
		affectedRows = 0;	// is modified by UPDATE, INSERT, DELETE

		// These productions map to public method calls:
		// 명령 처리하는 포인트

		if( in.matchAdvance(CREATE) != null )
		{	if( in.match( DATABASE ) )
			{	in.advance();
				createDatabase( in.required( IDENTIFIER ) );
			}
			else // must be CREATE TABLE
			{	in.required( TABLE );
				String tableName = in.required( IDENTIFIER );
				in.required( LP );
				createTable( tableName, declarations() );
				in.required( RP );
			}
		}
		else if( in.matchAdvance(DROP) != null )
		{	in.required( TABLE );
			dropTable( in.required(IDENTIFIER) );
		}
		else if( in.matchAdvance(USE) != null )
		{	in.required( DATABASE   );
			useDatabase( new File( in.required(IDENTIFIER) ));
		}

		else if( in.matchAdvance(BEGIN) != null )
		{	in.matchAdvance(WORK);	// ignore it if it's there
			begin();
		}
		else if( in.matchAdvance(ROLLBACK) != null )
		{	in.matchAdvance(WORK);	// ignore it if it's there
			rollback();
		}
		else if( in.matchAdvance(COMMIT) != null )
		{	in.matchAdvance(WORK);	// ignore it if it's there
			commit();
		}
		else if( in.matchAdvance(DUMP) != null )
		{	dump();
		}

		// These productions must be handled via an
		// interpreter:

		else if( in.matchAdvance(INSERT) != null )
		{	in.required( INTO );
			String tableName = in.required( IDENTIFIER );

			List columns = null, values = null;

			if( in.matchAdvance(LP) != null )
			{	columns = idList();
				in.required(RP);
			}
			if( in.required(VALUES) != null )
			{	in.required( LP );
				values = exprList();
				in.required( RP );
			}
			affectedRows = doInsert( tableName, columns, values );
		}
		else if( in.matchAdvance(UPDATE) != null )
		{	// First parse the expression
			String tableName = in.required( IDENTIFIER );
			in.required( SET );
			final String columnName = in.required( IDENTIFIER );
			in.required( EQUAL );
			final Expression value = expr();
			in.required(WHERE);
			affectedRows =
				doUpdate( tableName, columnName, value, expr() );
		}
		else if( in.matchAdvance(DELETE) != null )
		{	in.required( FROM );
			String tableName = in.required( IDENTIFIER );
			in.required( WHERE );
			affectedRows = doDelete( tableName, expr() );
		}
		else if( in.matchAdvance(SELECT) != null )
		// *일시 columns에 null 전달.
		{	List columns = idList();

			String into = null;
			if( in.matchAdvance(INTO) != null )
				into = in.required(IDENTIFIER);

			in.required( FROM );
			List requestedTableNames = idList();

			Expression where = (in.matchAdvance(WHERE) == null)
								? null : expr();
			Table result = doSelect(columns, into,
								requestedTableNames, where );
			return result;
		}
		else
		{	error("Expected insert, create, drop, use, "
										+"update, delete or select");
		}

		return null;
	}
	//----------------------------------------------------------------------
	// idList			::= IDENTIFIER idList' | STAR
	// idList'			::= COMMA IDENTIFIER idList'
	// 					|	e
	// Return a Collection holding the list of columns
	// or null if a * was found.

	private List idList()			throws ParseFailure
	{	List identifiers = null;
		if( in.matchAdvance(STAR) == null )
		{	identifiers = new ArrayList();
			String id;
			while( (id = in.required(IDENTIFIER)) != null )
			{	identifiers.add(id);
				if( in.matchAdvance(COMMA) == null )
					break;
			}
		}
		return identifiers;
	}

	//----------------------------------------------------------------------
	// declarations  ::= IDENTIFIER [type] declaration'
	// declarations' ::= COMMA IDENTIFIER [type] [NOT [NULL]] declarations'
	//				 |   e
	//
	// type			 ::= INTEGER [ LP expr RP 				]
	//				 |	 CHAR 	 [ LP expr RP				]
	//				 |	 NUMERIC [ LP expr COMMA expr RP	]
	//				 |	 DATE			// format spec is part of token

	private List declarations()			throws ParseFailure
	{	List identifiers = new ArrayList();

		String id;
		while( true )
		{	if( in.matchAdvance(PRIMARY) != null )
			{	in.required(KEY);
				in.required(LP);
				in.required(IDENTIFIER);
				in.required(RP);
			}
			else
			{	id = in.required(IDENTIFIER);

				identifiers.add(id);	// get the identifier

				// Skip past a type declaration if one's there

				if(	(in.matchAdvance(INTEGER) != null)
				||  (in.matchAdvance(CHAR)    != null)	)
				{
					if( in.matchAdvance(LP) != null )
					{	expr();
						in.required(RP);
					}
				}
				else if( in.matchAdvance(NUMERIC) != null )
				{	if( in.matchAdvance(LP) != null )
					{	expr();
						in.required(COMMA);
						expr();
						in.required(RP);
					}
				}
				else if( in.matchAdvance(DATE) 	!= null	)
				{	; // do nothing
				}

				in.matchAdvance( NOT );
				in.matchAdvance( NULL );
			}

			if( in.matchAdvance(COMMA) == null ) // no more columns
				break;
		}

		return identifiers;
	}

	// exprList 		::= 	  expr exprList'
	// exprList'		::= COMMA expr exprList'
	// 					|	e

	private List exprList()			throws ParseFailure
	{	List expressions = new LinkedList();

		expressions.add( expr() );
		while( in.matchAdvance(COMMA) != null )
		{	expressions.add( expr() );
		}
		return expressions;
	}

	private Expression expr()			throws ParseFailure
	{	Expression left = andExpr();
		while( in.matchAdvance(OR) != null )
			left = new LogicalExpression( left, OR, andExpr());
		return left;
	}

	// andExpr			::= 	relationalExpr andExpr'
	// andExpr'			::= AND relationalExpr andExpr'
	// 					|	e

	private Expression andExpr()			throws ParseFailure
	{	Expression left = relationalExpr();
		while( in.matchAdvance(AND) != null )
			left = new LogicalExpression( left, AND, relationalExpr() );
		return left;
	}

	// relationalExpr ::=   		additiveExpr relationalExpr'
	// relationalExpr'::=	  RELOP additiveExpr relationalExpr'
	// 						| EQUAL additiveExpr relationalExpr'
	// 						| LIKE  additiveExpr relationalExpr'
	// 						| e

	private Expression relationalExpr()			throws ParseFailure
	{	Expression left = additiveExpr();
		while( true )
		{	String lexeme;
			if( (lexeme = in.matchAdvance(RELOP)) != null )
			{	RelationalOperator op;
				if( lexeme.length() == 1 )
					op = lexeme.charAt(0)=='<' ? LT : GT ;
				else
				{	if( lexeme.charAt(0)=='<' && lexeme.charAt(1)=='>')
						op = NE;
					else
						op = lexeme.charAt(0)=='<' ? LE : GE ;
				}
				left = new RelationalExpression(left, op, additiveExpr());
			}
			else if( in.matchAdvance(EQUAL) != null )
			{	left = new RelationalExpression(left, EQ, additiveExpr());
			}
			else if( in.matchAdvance(LIKE) != null )
			{	left = new LikeExpression(left, additiveExpr());
			}
			else
				break;
		}
		return left;
	}

	// additiveExpr	::= 			 multiplicativeExpr additiveExpr'
	// additiveExpr'	::= ADDITIVE multiplicativeExpr additiveExpr'
	// 					|	e

	private Expression additiveExpr()			throws ParseFailure
	{	String lexeme;
		Expression left = multiplicativeExpr();
		while( (lexeme = in.matchAdvance(ADDITIVE)) != null )
		{	MathOperator op = lexeme.charAt(0)=='+' ? PLUS : MINUS;
			left = new ArithmeticExpression(
							left, multiplicativeExpr(), op );
		}
		return left;
	}

	// multiplicativeExpr	::=       term multiplicativeExpr'
	// multiplicativeExpr'	::= STAR  term multiplicativeExpr'
	// 						|	SLASH term multiplicativeExpr'
	// 						|	e

	private Expression multiplicativeExpr()			throws ParseFailure
	{ Expression left = term();
		while( true )
		{	if( in.matchAdvance(STAR) != null)
				left = new ArithmeticExpression( left, term(), TIMES );
			else if( in.matchAdvance(SLASH) != null)
				left = new ArithmeticExpression( left, term(), DIVIDE );
			else
				break;
		}
		return left;
	}

	// term				::=	NOT expr
	// 					|	LP expr RP
	// 					|	factor

	private Expression term()			throws ParseFailure
	{	if( in.matchAdvance(NOT) != null )
		{	return new NotExpression( expr() );
		}
		else if( in.matchAdvance(LP) != null )
		{	Expression toReturn = expr();
			in.required(RP);
			return toReturn;
		}
		else
			return factor();
	}

	// factor			::= compoundId | STRING | NUMBER | NULL
	// compoundId		::= IDENTIFIER compoundId'
	// compoundId'		::= DOT IDENTIFIER
	// 					|	e

	private Expression factor() throws ParseFailure
	{	try
		{	String  lexeme;
			Value	result;

			if( (lexeme = in.matchAdvance(STRING)) != null )
				result = new StringValue( lexeme );

			else if( (lexeme = in.matchAdvance(NUMBER)) != null )
				result = new NumericValue( lexeme );

			else if( (lexeme = in.matchAdvance(NULL)) != null )
				result = new NullValue();

			else
			{	String columnName  = in.required(IDENTIFIER);
				String tableName   = null;

				if( in.matchAdvance(DOT) != null )
				{	tableName  = columnName;
					columnName = in.required(IDENTIFIER);
				}

				result = new IdValue( tableName, columnName );
			}

			return new AtomicExpression(result);
		}
		catch( java.text.ParseException e) { /* fall through */ }

		error("Couldn't parse Number"); // Always throws a ParseFailure
		return null;
	}
	//@parser-end
	//@expression-start
	//======================================================================
	// The methods that parse the the productions rooted in expr work in
	// concert to build an Expression object that evaluates the expression.
	// This is an example of both the Interpreter and Composite pattern.
	// An expression is represented in memory as an abstract syntax tree
	// made up of instances of the following classes, each of which
	// references its subexpressions.

	private interface Expression
	{	/* Evaluate an expression using rows identified by the
		 * two iterators passed as arguments. <code>j</code>
		 * is null unless a join is being processed.
		 */

		Value evaluate(Cursor[] tables) throws ParseFailure;
	}
	//- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	private class ArithmeticExpression implements Expression
	{	private final MathOperator	operator;
		private final Expression	left, right;

		public ArithmeticExpression( Expression left, Expression right,
												MathOperator operator )
		{	this.operator = operator;
			this.left	  = left;
			this.right	  = right;
		}

		public Value evaluate(Cursor[] tables) throws ParseFailure
		{
			Value leftValue  = left.evaluate ( tables );
			Value rightValue = right.evaluate( tables );

			verify
			(	 leftValue  instanceof NumericValue
			  && rightValue instanceof NumericValue,
			  "Operands to < > <= >= = must be Boolean"
			);

			double l = ((NumericValue)leftValue).value();
			double r = ((NumericValue)rightValue).value();

			return new NumericValue
			( 	(  operator == PLUS 	) ? ( l + r ) :
			 	(  operator == MINUS 	) ? ( l - r ) :
			 	(  operator == TIMES 	) ? ( l * r ) :
			 	/* operator == DIVIDE  */   ( l / r )
			);
		}
	}
	//- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	private class LogicalExpression implements Expression
	{	private final boolean 	 isAnd;
		private final Expression left, right;

		public LogicalExpression( Expression left,  Token op,
													Expression right )
		{	assert op==AND || op==OR;
			this.isAnd	= (op == AND);
			this.left	= left;
			this.right	= right;
		}

		public Value evaluate( Cursor[] tables ) throws ParseFailure
		{	Value leftValue  = left. evaluate(tables);
			Value rightValue = right.evaluate(tables);
			verify
			(	 leftValue  instanceof BooleanValue
			  && rightValue instanceof BooleanValue,
			  "operands to AND and OR must be logical/relational"
			);

			boolean l = ((BooleanValue)leftValue).value();
			boolean r = ((BooleanValue)rightValue).value();

			return new BooleanValue( isAnd ? (l && r) : (l || r) );
		}
	}
	//- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	private class NotExpression implements Expression
	{	private final Expression operand;

		public NotExpression( Expression operand )
		{	this.operand = operand;
		}
		public Value evaluate( Cursor[] tables ) throws ParseFailure
		{	Value value = operand.evaluate( tables );
			verify( value instanceof BooleanValue,
					  "operands to NOT must be logical/relational");
			return new BooleanValue( !((BooleanValue)value).value() );
		}
	}
	//- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	private class RelationalExpression implements Expression
	{
		private final RelationalOperator	operator;
		private final Expression  			left, right;

		public RelationalExpression(Expression left,
									RelationalOperator operator,
									Expression right )
		{	this.operator = operator;
			this.left	  = left;
			this.right	  = right;
		}

		public Value evaluate( Cursor[] tables ) throws ParseFailure
		{
			Value leftValue  = left.evaluate ( tables );
			Value rightValue = right.evaluate( tables );

			if( 	(leftValue  instanceof StringValue)
				||	(rightValue instanceof StringValue) )
			{	verify(operator==EQ || operator==NE,
							"Can't use < <= > or >= with string");

				boolean isEqual =
					leftValue.toString().equals(rightValue.toString());

				return new BooleanValue(operator==EQ ? isEqual:!isEqual);
			}

			if( rightValue instanceof NullValue
			 ||	leftValue  instanceof NullValue )
			{
				verify(operator==EQ || operator==NE,
							"Can't use < <= > or >= with NULL");

				// Return true if both the left and right sides are instances
				// of NullValue.
				boolean isEqual = 
						leftValue.getClass() == rightValue.getClass();

				return new BooleanValue(operator==EQ ? isEqual : !isEqual);
			}

			// Convert Boolean values to numbers so we can compare them.
			//
			if( leftValue instanceof BooleanValue )
				leftValue = new NumericValue(
								((BooleanValue)leftValue).value() ? 1 : 0 );
			if( rightValue instanceof BooleanValue )
				rightValue = new NumericValue(
								((BooleanValue)rightValue).value() ? 1 : 0 );

			verify( 	leftValue  instanceof NumericValue
				     && rightValue instanceof NumericValue,
									 "Operands must be numbers" );

			double l = ((NumericValue)leftValue).value();
			double r = ((NumericValue)rightValue).value();

			return new BooleanValue
			( 	( operator == EQ	  ) ? ( l == r ) :
			  	( operator == NE	  ) ? ( l != r ) :
				( operator == LT  	  ) ? ( l >  r ) :
				( operator == GT  	  ) ? ( l <  r ) :
				( operator == LE 	  ) ? ( l <= r ) :
				/* operator == GE	 */   ( l >= r )
			);
		}
	}
	//- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	private class LikeExpression implements Expression
	{	private final Expression left, right;
		public LikeExpression( Expression left, Expression right )
		{	this.left	= left;
			this.right	= right;
		}

		public Value evaluate(Cursor[] tables) throws ParseFailure
		{	Value leftValue	 = left.evaluate(tables);
			Value rightValue = right.evaluate(tables);
			verify
			(	leftValue  instanceof StringValue
			 && rightValue instanceof StringValue,
			 	"Both operands to LIKE must be strings"
			);

			String  compareTo = ((StringValue) leftValue).value();
			String  regex	  = ((StringValue) rightValue).value();
					regex 	  = regex.replaceAll("%",".*");

			return new BooleanValue( compareTo.matches(regex) );
		}
	}
	//- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	private class AtomicExpression implements Expression
	{	private final Value atom;
		public AtomicExpression( Value atom )
		{	this.atom = atom;
		}
		public Value evaluate( Cursor[] tables )
		{	return atom instanceof IdValue
			 	? ((IdValue)atom).value(tables)	// lookup cell in table and
				: atom							// convert to appropriate type
				;
		}
	}
	//@expression-end
	//@value-start
	//--------------------------------------------------------------
	// The expression classes pass values around as they evaluate
	// the expression.  // There  are four value subtypes that represent
	// the possible/ operands to an expression (null, numbers,
	// strings, table.column). The implementors of Value provide
	// convenience methods for using those operands.
	//
	private interface Value	// tagging interface
	{
	}
	//- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	private static class NullValue implements Value
	{	public String toString(){ return null; }
	}
	//- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	private static final class BooleanValue implements Value
	{	boolean value;
		public BooleanValue( boolean value )
		{	this.value = value;
		}
		public boolean	value()	  { return value; }
		public String	toString(){ return String.valueOf(value); };
	}
	//- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	private static class StringValue implements Value
	{	private String value;
		public StringValue(String lexeme)
		{	value = lexeme.replaceAll("['\"](.*?)['\"]", "$1" );
		}
		public String value()	{ return value; }
		public String toString(){ return value; }
	}
	//- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	private final class NumericValue implements Value
	{	private double value;
		public NumericValue(double value)	// initialize from a double.
		{	this.value = value;
		}
		public NumericValue(String s) throws java.text.ParseException
		{	this.value = NumberFormat.getInstance().parse(s).doubleValue();
		}
		public double value()
		{	return value;
		}
		public String toString() // round down if the fraction is very small
		{	
			if( Math.abs(value - Math.floor(value)) < 1.0E-20 )
				return String.valueOf( (long)value );
			else
				return String.valueOf( value );
		}
	}
	//- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	private final class IdValue implements Value
	{	String tableName;
		String columnName;

		public IdValue(String tableName, String columnName)
		{	this.tableName  = tableName;
			this.columnName = columnName;
		}

		public String toString( Cursor[] participants )
		{	Object content = null;

			// If no name is to the left of the dot, then use
			// the (only) table.

			if( tableName == null )
				content= participants[0].column( columnName );
			else
			{	Table container = (Table) tables.get(tableName);

				// Search for the table whose name matches
				// the one to the left of the dot, then extract
				// the desired column from that table.

				content = null;
				for( int i = 0; i < participants.length; ++i )
				{	if( participants[i].isTraversing(container) )
					{	content = participants[i].column(columnName);
						break;
					}
				}
			}

			// All table contents are converted to Strings, whatever
			// their original type. This conversion can cause
			// problems if the table was created manually.

			return (content == null) ? null : content.toString();
		}

		public Value value( Cursor[] participants )
		{	String s = toString( participants );
			try
			{	return ( s == null )
					? (Value) new NullValue()
					: (Value) new NumericValue(s)
					;
			}
			catch( java.text.ParseException e )
			{	// The NumericValue constructor failed, so it must be
				// a string. Fall through to the return-a-string case.
			}
			return new StringValue( s );
		}
	}
	//@value-end
	//@workhorse-start
	//======================================================================
	// Workhorse methods called from the parser.
	//
	private Table doSelect( List columns, String into,
										List requestedTableNames,
										final Expression where )
										throws ParseFailure
	{

		Iterator tableNames = requestedTableNames.iterator();

		assert tableNames.hasNext() : "No tables to use in select!" ;

		// The primary table is the first one listed in the
		// FROM clause. The participantsInJoin are the other
		// tables listed in the FROM clause. We're passed in the
		// table names; use these names to get the actual Table
		// objects.

		Table primary = (Table) tables.get( (String) tableNames.next() );

		List participantsInJoin = new ArrayList();
		while( tableNames.hasNext() )
		{	String participant = (String) tableNames.next();
			participantsInJoin.add( tables.get(participant) );
		}

		// Now do the select operation. First create a Strategy
		// object that picks the correct rows, then pass that
		// object through to the primary table's select() method.

		Selector selector = (where == null) ? Selector.ALL : //{=Database.selector}
			new Selector.Adapter()
			{	public boolean approve(Cursor[] tables)
				{	try
					{	
						Value result = where.evaluate(tables);

						verify( result instanceof BooleanValue,
								"WHERE clause must yield boolean result" );
						return ((BooleanValue)result).value();
					}
					catch( ParseFailure e )
					{	throw new ThrowableContainer(e);
					}
				}
			};

		try
		{	Table result = primary.select(selector, columns, participantsInJoin);

			// If this is a "SELECT INTO <table>" request, remove the 
			// returned table from the UnmodifiableTable wrapper, give
			// it a name, and put it into the tables Map.

			if( into != null )
			{	result = ((UnmodifiableTable)result).extract();
				result.rename(into);
				tables.put( into, result );
			}
			return result;
		}
		catch( ThrowableContainer container )
		{	throw (ParseFailure) container.contents();
		}
	}
	//- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	private int doInsert(String tableName, List columns, List values)
												throws ParseFailure
	{
		List  processedValues = new LinkedList();
		Table t = (Table) tables.get( tableName );

		for( Iterator i = values.iterator(); i.hasNext(); )
		{	Expression current = (Expression) i.next();
			processedValues.add(
					current.evaluate(null).toString() );
		}

		// finally, put the values into the table.

		if( columns == null )
			return t.insert( processedValues );

		verify( columns.size() == values.size(),
				"There must be a value for every listed column" );
		return t.insert( columns, processedValues );
	}
	//- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	private int doUpdate( String tableName, final String columnName,
						  final Expression value, final Expression where)
												  throws ParseFailure
	{
		Table t = (Table) tables.get( tableName );
		try
		{	return t.update
			(	new Selector()
				{	public boolean approve(	Cursor[] tables )
					{	try
						{	Value result = where.evaluate(tables);

							verify( result instanceof BooleanValue,
								"WHERE clause must yield boolean result" );

							return ((BooleanValue)result).value();
						}
						catch( ParseFailure e )
						{	throw new ThrowableContainer(e);
						}
					}
					public void modify( Cursor current )
					{	try
						{	Value newValue = value.evaluate( new Cursor[]{current} );
							current.update( columnName, newValue.toString() );
						}
						catch( ParseFailure e )
						{	throw new ThrowableContainer(e);
						}
					}
				}
			);
		}
		catch( ThrowableContainer container )
		{	throw (ParseFailure) container.contents();
		}
	}
	//- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	private int doDelete( String tableName, final Expression where )
												throws ParseFailure
	{	Table t = (Table) tables.get( tableName );
		try
		{	return t.delete
			(	new Selector.Adapter()
				{	public boolean approve( Cursor[] tables )
					{	try
						{	Value result = where.evaluate(tables);
							verify( result instanceof BooleanValue,
								"WHERE clause must yield boolean result" );
							return ((BooleanValue)result).value();
						}
						catch( ParseFailure e )
						{	throw new ThrowableContainer(e);
						}
					}
				}
			);
		}
		catch( ThrowableContainer container )
		{	throw (ParseFailure) container.contents();
		}
	}
	//@workhorse-end
	//--------------------------------------------------------------
	public static class Test
	{	public static void main(String[] args) throws IOException, ParseFailure
		{	Database theDatabase = new Database();

			// Read a sequence of SQL statements in from the file
			// Database.test.sql and execute them.

			BufferedReader sql = new BufferedReader(
									new FileReader( "Database.test.sql" ));
			String test;
			while( (test = sql.readLine()) != null )
			{	test = test.trim();
				if( test.length() == 0 )
					continue;

				while( test.endsWith("\\") )
				{	test = test.substring(0, test.length()-1 );
					test += sql.readLine().trim();
				}

				System.out.println("Parsing: " + test);
				Table result = theDatabase.execute( test );

				if( result != null )	// it was a SELECT of some sort
					System.out.println( result.toString() );
			}

			try
			{	theDatabase.execute("insert garbage SQL");
				System.out.println("Database FAILED");
				System.exit(1);
			}
			catch( ParseFailure e )
			{	System.out.println("Correctly found garbage SQL:\n"
					+ e + "\n"
					+ e.getErrorReport() );
			}

			theDatabase.dump();
			System.out.println("Database PASSED");
			System.exit(0);
		}
	}
}
