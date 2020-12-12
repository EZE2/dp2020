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
package com.holub.tools;

import java.util.*;

public final class ArrayIterator implements Iterator
{
	private int	 			position = 0;
	private final Object[]	items;

	public ArrayIterator(Object[] items){ this.items = items; }

	public boolean hasNext()
	{	return ( position < items.length );
	}

	public Object next()
	{	if( position >= items.length )
			throw new NoSuchElementException();
		return items[ position++ ];
	}

	public void remove()
	{	throw new UnsupportedOperationException(
								"ArrayIterator.remove()");
	}

	public Object[] toArray()
	{	return (Object[]) items.clone();
	}
}	
