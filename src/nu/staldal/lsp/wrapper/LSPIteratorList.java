/*
 * Copyright (c) 2002, Mikael St�ldal
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the author nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * Note: This is known as "the modified BSD license". It's an approved
 * Open Source and Free Software license, see
 * http://www.opensource.org/licenses/
 * and
 * http://www.gnu.org/philosophy/license-list.html
 */

package nu.staldal.lsp.wrapper;

import java.util.*;

import nu.staldal.lsp.*;


/**
 * Implementation of LSPList for {@link java.util.Iterator}.
 */
public class LSPIteratorList implements LSPList
{
    private int theLength = 0;
	private Iterator iter;
	private boolean atEnd = false;

	public LSPIteratorList(Iterator iter)
	{
		this.iter = iter;
	}

	public boolean hasNext()
	{
		return iter.hasNext();
	}

	public Object next() throws NoSuchElementException
	{
		try {
			Object o = iter.next();
			theLength++;
			return o;
		}
		catch (NoSuchElementException e)
		{
			atEnd = true;
			throw e;
		}
	}

	public int index()
	{
		return theLength;	
	}

    public int length() throws IllegalArgumentException
	{
		if (atEnd)
			return theLength;
		else
			throw new IllegalArgumentException(
				"not possible to check length of an Enumeration");
	}

	public void reset() throws IllegalArgumentException
	{
		if (theLength > 0)
			throw new IllegalArgumentException(
				"not possible to reset an Enumeration");
	}
	
}

