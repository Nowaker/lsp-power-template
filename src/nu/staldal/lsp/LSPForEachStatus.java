/*
 * Copyright (c) 2003-2004, Mikael St�ldal
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

package nu.staldal.lsp;

import java.util.*;

public class LSPForEachStatus implements Map
{
    private ListIterator theIterator;

    public LSPForEachStatus(ListIterator iterator)
    {
        theIterator = iterator;
    }
    
    public Object get(Object key)
    {
        if (key.equals("index"))
            return new Double(theIterator.nextIndex());
        else if (key.equals("first"))
            return Boolean.valueOf(theIterator.nextIndex() == 1);
        else if (key.equals("last"))
            return Boolean.valueOf(!theIterator.hasNext());
        else if (key.equals("even"))
            return Boolean.valueOf(theIterator.nextIndex() % 2 == 0);
        else if (key.equals("odd"))
            return Boolean.valueOf(theIterator.nextIndex() % 2 != 0);
        else
            return null;
    }
    
    public boolean containsKey(Object key)
    {
        if (key.equals("index"))
            return true;
        else if (key.equals("first"))
            return true;
        else if (key.equals("last"))
            return true;
        else if (key.equals("even"))
            return true;
        else if (key.equals("odd"))
            return true;
        else
            return false;
    }

    public int size()
    {   
        return 5;
    }

    public boolean isEmpty()
    {
        return size() == 0;    
    }

    public boolean containsValue(Object value)
    {
        throw new UnsupportedOperationException();    
    }
    
    public Object put(Object key, Object value)
    {
        throw new UnsupportedOperationException();    
    }
    

    public Object remove(Object key)
    {
        throw new UnsupportedOperationException();    
    }

    public void putAll(Map t)
    {
        throw new UnsupportedOperationException();    
    }

    public void clear()
    {
        throw new UnsupportedOperationException();    
    }

    public java.util.Set keySet()
    {
        throw new UnsupportedOperationException();    
    }

    public java.util.Collection values()
    {
        throw new UnsupportedOperationException();    
    }

    public java.util.Set entrySet()
    {
        throw new UnsupportedOperationException();    
    }

}

