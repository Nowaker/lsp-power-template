/*
 * Copyright (c) 2001-2002, Mikael St�ldal
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

import java.io.IOException;
import java.util.*;

import org.xml.sax.*;

import nu.staldal.xtree.*;

import nu.staldal.lsp.expr.*;
import nu.staldal.lsp.compile.*;
import nu.staldal.lsp.compiledexpr.*;
import nu.staldal.lsp.wrapper.*;

import nu.staldal.lagoon.core.Target;
import nu.staldal.lagoon.core.SourceManager;


public class LSPInterpreter implements LSPPage
{
	private static final boolean DEBUG = false;
	
    static final long serialVersionUID = -1168364109491726217L;

    private static final String LSP_CORE_NS = "http://staldal.nu/LSP/core";
    private static final String XML_NS = "http://www.w3.org/XML/1998/namespace";

    private long timeCompiled;
    private LSPNode theTree;
    private Hashtable importedFiles;
    private Vector includedFiles;
    private boolean compileDynamic;
    private boolean executeDynamic;

    private transient URLResolver resolver = null;
    private transient Hashtable params = null;
	private transient Target target = null;
	private transient SourceManager sourceMan = null;
	private transient Hashtable extLibs = null;

    public LSPInterpreter(LSPNode theTree, Hashtable importedFiles,
        Vector includedFiles, boolean compileDynamic, boolean executeDynamic)
        throws LSPException
    {
        this.timeCompiled = System.currentTimeMillis();
        this.theTree = theTree;
        this.importedFiles = importedFiles;
        this.includedFiles = includedFiles;
        this.compileDynamic = compileDynamic;
        this.executeDynamic = executeDynamic;
    }


    public Enumeration getCompileDependentFiles()
    {
        return importedFiles.keys();
    }

    public Enumeration getExecuteDependentFiles()
    {
        return includedFiles.elements();
    }

    public boolean isCompileDynamic()
    {
        return compileDynamic;
    }

    public boolean isExecuteDynamic()
    {
        return executeDynamic;
    }

    public long getTimeCompiled()
    {
        return timeCompiled;
    }


    public void execute(ContentHandler ch, URLResolver resolver,
        Hashtable params, Target target, SourceManager sourceMan)
        throws SAXException
    {
        this.params = params;
        this.resolver = resolver;
		this.target = target;
		this.sourceMan = sourceMan; 
		this.extLibs = new Hashtable();
        processNode(theTree, ch);
		this.extLibs = null;
		this.sourceMan = null;
		this.target = null;
        this.resolver = null;
        this.params = null;
    }


    private void processNode(LSPNode node, ContentHandler sax)
        throws SAXException
    {
        if (node instanceof LSPExtElement)
            processNode((LSPExtElement)node, sax);
        else if (node instanceof LSPElement)
            processNode((LSPElement)node, sax);
        else if (node instanceof LSPText)
            processNode((LSPText)node, sax);
        else if (node instanceof LSPIf)
            processNode((LSPIf)node, sax);
        else if (node instanceof LSPChoose)
            processNode((LSPChoose)node, sax);
        else if (node instanceof LSPForEach)
            processNode((LSPForEach)node, sax);
        else if (node instanceof LSPInclude)
            processNode((LSPInclude)node, sax);
        else if (node instanceof LSPProcessingInstruction)
            processNode((LSPProcessingInstruction)node, sax);
        else if (node instanceof LSPTemplate)
            processNode((LSPTemplate)node, sax);
        else if (node instanceof LSPContainer)
            processChildren((LSPContainer)node, sax);
        else
			throw new LSPException("Unrecognized LSPNode: "
				+ node.getClass().getName());
    }

	private void processChildren(LSPContainer el, ContentHandler sax)
		throws SAXException
	{
		for (int i = 0; i < el.numberOfChildren(); i++)
		{
			LSPNode child = el.getChild(i);
			processNode(child, sax);
		}
	}

    private void processNode(LSPExtElement el, ContentHandler sax)
        throws SAXException
    {
		LSPExtLib extLib = lookupExtensionHandler(el.getClassName());
		
		try {
			ContentHandler in = extLib.beforeElement(sax, target, sourceMan);
		
			processNode((LSPElement)el, in);
		
			String res = extLib.afterElement();
			if (res != null) sax.characters(res.toCharArray(), 0, res.length());
		}
		catch (IOException e)
		{
			throw new SAXException(e);
		}
	}
		
	private void processNode(LSPElement el, ContentHandler sax)
        throws SAXException
    {
		// Copy element to output verbatim

		for (int i = 0; i < el.numberOfNamespaceMappings(); i++)
		{
			String[] m = el.getNamespaceMapping(i);
			if (!m[1].equals(LSP_CORE_NS))
				sax.startPrefixMapping(m[0], m[1]);
		}

		org.xml.sax.helpers.AttributesImpl saxAtts =
			new org.xml.sax.helpers.AttributesImpl();

		for (int i = 0; i < el.numberOfAttributes(); i++)
		{
			String URI = el.getAttributeNamespaceURI(i);
			String local = el.getAttributeLocalName(i);
			String type = el.getAttributeType(i);
			LSPExpr value = el.getAttributeValue(i);

			saxAtts.addAttribute(URI, local, "", type,
				evalExprAsString(value));
		}
		sax.startElement(el.getNamespaceURI(), el.getLocalName(), "",
			saxAtts);

		processChildren(el, sax);

		sax.endElement(el.getNamespaceURI(), el.getLocalName(), "");

		for (int i = 0; i < el.numberOfNamespaceMappings(); i++)
		{
			String[] m = el.getNamespaceMapping(i);
			if (!m[1].equals(LSP_CORE_NS))
				sax.endPrefixMapping(m[0]);
		}
    }

    private void processNode(LSPText text, ContentHandler sax)
        throws SAXException
    {
		char[] chars = text.asCharArray();
        sax.characters(chars, 0, chars.length);
    }


	private void processNode(LSPProcessingInstruction el, ContentHandler sax)
		throws SAXException
	{
		StringHandler sh = new StringHandler();

		processNode(el.getData(), sh);

		sax.processingInstruction(
			evalExprAsString(el.getName()),
			sh.getBuf().toString());
	}


	private void processNode(LSPInclude el, ContentHandler sax)
		throws SAXException
	{
        String url = evalExprAsString(el.getFile());
		try {
			IncludeHandler ih = new IncludeHandler(sax);

            resolver.resolve(url, ih);
		}
		catch (IOException e)
		{
			throw new SAXException(e);
		}
	}


	private void processNode(LSPIf el, ContentHandler sax)
		throws SAXException
	{
		LSPExpr expr = el.getTest();

		if (evalExprAsBoolean(expr))
		{
			processNode(el.getBody(), sax);
		}
	}


	private void processNode(LSPChoose el, ContentHandler sax)
		throws SAXException
	{
		for (int i = 0; i<el.getNWhens(); i++)
		{
			LSPExpr expr = el.getWhenTest(i);

			if (evalExprAsBoolean(expr))
			{
				processNode(el.getWhenBody(i), sax);
				return;
			}

		}

		LSPNode otherwise = el.getOtherwise();
		if (otherwise != null)
		{
			processNode(otherwise, sax);
		}
	}


	private void processNode(LSPForEach el, ContentHandler sax)
		throws SAXException
	{
		final LSPList theList = evalExprAsList(el.getList());

		try {
			theList.reset();
		}
		catch (IllegalArgumentException e)
		{
			throw new LSPException("Cannot traverse list: " + e.getMessage());	
		}
		while (theList.hasNext())
		{
			Object o = theList.next();
			Object oldVar = params.get(el.getVariable());
			Object oldStatus = null;
			params.put(el.getVariable(), o);
			if (el.getStatusObject() != null)
			{
				oldStatus = params.get(el.getStatusObject());

				params.put(el.getStatusObject(), new LSPTuple()
					{
						public Object get(String key)
						{
							if (key.equals("index"))
								return new Double(theList.index());
							else if (key.equals("first"))
								return new Boolean(theList.index() == 1);
							else if (key.equals("last"))
								return new Boolean(!theList.hasNext());
							else if (key.equals("even"))
								return new Boolean(theList.index() % 2 == 0);
							else if (key.equals("odd"))
								return new Boolean(theList.index() % 2 != 0);
							else
								return null;
						}
					});
			}
			processNode(el.getBody(), sax);
			if (oldVar == null)
				params.remove(el.getVariable());
			else
				params.put(el.getVariable(), oldVar);
			if (el.getStatusObject() != null)
			{
				if (oldStatus == null)
					params.remove(el.getStatusObject());
				else
					params.put(el.getStatusObject(), oldStatus);
			}				
		}
	}
		

	private void processNode(LSPTemplate el, ContentHandler sax)
		throws SAXException
	{
		LSPExpr expr = el.getExpr();

		String text = evalExprAsString(expr);

		char[] chars = text.toCharArray();
        sax.characters(chars, 0, chars.length);
	}



	private Object evalExpr(LSPExpr expr) throws SAXException
	{
		if (expr instanceof StringLiteral)
		{
			return evalExpr((StringLiteral)expr);
		}
		else if (expr instanceof NumberLiteral)
		{
			return evalExpr((NumberLiteral)expr);
		}
		else if (expr instanceof BinaryExpr)
		{
			return evalExpr((BinaryExpr)expr);
		}
		else if (expr instanceof UnaryExpr)
		{
			return evalExpr((UnaryExpr)expr);
		}
		else if (expr instanceof BuiltInFunctionCall)
		{
			return evalExpr((BuiltInFunctionCall)expr);
		}
		else if (expr instanceof ExtensionFunctionCall)
		{
			return evalExpr((ExtensionFunctionCall)expr);
		}
		else if (expr instanceof VariableReference)
		{
			return evalExpr((VariableReference)expr);
		}
		else if (expr instanceof TupleExpr)
		{
			return evalExpr((TupleExpr)expr);
		}
        else
        {
			throw new LSPException("Unrecognized LSPExpr: "
				+ expr.getClass().getName());
		}
	}


	private String evalExpr(StringLiteral expr) throws LSPException
	{
		return expr.getValue();
	}


	private Double evalExpr(NumberLiteral expr) throws LSPException
	{
		return new Double(expr.getValue());
	}


	private Object evalExpr(BinaryExpr expr) throws SAXException
	{
		switch (expr.getOp())
		{
		case BinaryExpr.OR:
			if (evalExprAsBoolean(expr.getLeft()))
				return Boolean.TRUE;
			else
				return new Boolean(evalExprAsBoolean(expr.getRight()));

		case BinaryExpr.AND:
			if (!evalExprAsBoolean(expr.getLeft()))
				return Boolean.FALSE;
			else
				return new Boolean(evalExprAsBoolean(expr.getRight()));

		case BinaryExpr.EQ:
		case BinaryExpr.NE:
		{
			Object left = evalExpr(expr.getLeft());
			Object right = evalExpr(expr.getRight());
			boolean res;
			if ((left instanceof Boolean) || (right instanceof Boolean))
			{
				res = convertToBoolean(left) == convertToBoolean(right);
			}
			else if ((left instanceof Number) || (right instanceof Number))
			{
				res = convertToNumber(left) == convertToNumber(right);
			}
			else
			{
				res = convertToString(left).equals(convertToString(right));
			}
			if (expr.isOp(BinaryExpr.EQ))
				return new Boolean(res);
			else
				return new Boolean(!res);
		}

		case BinaryExpr.LT:
			return new Boolean(evalExprAsNumber(expr.getLeft()) < evalExprAsNumber(expr.getRight()));
		case BinaryExpr.LE:
			return new Boolean(evalExprAsNumber(expr.getLeft()) <= evalExprAsNumber(expr.getRight()));
		case BinaryExpr.GT:
			return new Boolean(evalExprAsNumber(expr.getLeft()) > evalExprAsNumber(expr.getRight()));
		case BinaryExpr.GE:
			return new Boolean(evalExprAsNumber(expr.getLeft()) >= evalExprAsNumber(expr.getRight()));

		case BinaryExpr.PLUS:
			return new Double(evalExprAsNumber(expr.getLeft()) + evalExprAsNumber(expr.getRight()));
		case BinaryExpr.MINUS:
			return new Double(evalExprAsNumber(expr.getLeft()) - evalExprAsNumber(expr.getRight()));
		case BinaryExpr.TIMES:
			return new Double(evalExprAsNumber(expr.getLeft()) * evalExprAsNumber(expr.getRight()));
		case BinaryExpr.DIV:
			return new Double(evalExprAsNumber(expr.getLeft()) / evalExprAsNumber(expr.getRight()));
		case BinaryExpr.MOD:
			return new Double(evalExprAsNumber(expr.getLeft()) % evalExprAsNumber(expr.getRight()));

		default: throw new LSPException("Unrecognized binary operator: "
			+ expr.getOp());
		}
	}


	private Object evalExpr(UnaryExpr expr) throws SAXException
	{
		return new Double(-evalExprAsNumber(expr.getLeft()));
	}


	private Object evalExpr(BuiltInFunctionCall expr) throws SAXException
	{
		if (expr.getName().equals("string"))
		{
			if (expr.numberOfArgs() != 1)
				throw new LSPException(
					"string() function must have 1 argument");

			return evalExprAsString(expr.getArg(0));
		}
		else if (expr.getName().equals("concat"))
		{
			if (expr.numberOfArgs() < 2)
				throw new LSPException(
					"concat() function must have at least 2 argument");

			StringBuffer sb = new StringBuffer();
			for (int i = 0; i<expr.numberOfArgs(); i++)
			{
				sb.append(evalExprAsString(expr.getArg(i)));
			}
			return sb.toString();
		}
		else if (expr.getName().equals("starts-with"))
		{
			if (expr.numberOfArgs() != 2)
				throw new LSPException(
					"starts-with() function must have 2 arguments");

			String a = evalExprAsString(expr.getArg(0));
			String b = evalExprAsString(expr.getArg(1));

			return new Boolean(a.startsWith(b));
		}
		else if (expr.getName().equals("contains"))
		{
			if (expr.numberOfArgs() != 2)
				throw new LSPException(
					"contains() function must have 2 arguments");

			String a = evalExprAsString(expr.getArg(0));
			String b = evalExprAsString(expr.getArg(1));

			return new Boolean(a.indexOf(b) > -1);
		}
		else if (expr.getName().equals("substring-before"))
		{
			if (expr.numberOfArgs() != 2)
				throw new LSPException(
					"substring-before() function must have 2 arguments");

			String a = evalExprAsString(expr.getArg(0));
			String b = evalExprAsString(expr.getArg(1));

			int index = a.indexOf(b);

			if (index < 0)
				return "";
			else
				return a.substring(0, index);
		}
		else if (expr.getName().equals("substring-after"))
		{
			if (expr.numberOfArgs() != 2)
				throw new LSPException(
					"substring-after() function must have 2 arguments");

			String a = evalExprAsString(expr.getArg(0));
			String b = evalExprAsString(expr.getArg(1));

			int index = a.indexOf(b);

			if (index < 0)
				return "";
			else
				return a.substring(index+1);
		}
		else if (expr.getName().equals("substring"))
		{
			if ((expr.numberOfArgs() != 2) && (expr.numberOfArgs() != 3))
				throw new LSPException(
					"substring() function must have 2 or 3 arguments");

			String a = evalExprAsString(expr.getArg(0));
			double bd = evalExprAsNumber(expr.getArg(1));
			double cd = (expr.numberOfArgs() == 3)
				? evalExprAsNumber(expr.getArg(2))
				: (a.length()+1);
			if (Double.isNaN(bd) || Double.isNaN(cd)) return "";

			int b = (int)Math.round(bd);
			int c = (int)Math.round(cd);

			if (b > a.length()) b = a.length();
			if (c < 1) return "";
			if (c > (a.length()-b+1)) c = a.length()-b+1;

			return a.substring((b-1 < 0) ? 0 : (b-1), b-1+c);
		}
		else if (expr.getName().equals("string-length"))
		{
			if (expr.numberOfArgs() != 1)
				throw new LSPException(
					"string-length() function must have 1 argument");

			String a = evalExprAsString(expr.getArg(0));

			return new Double(a.length());
		}
		else if (expr.getName().equals("normalize-space"))
		{
			if (expr.numberOfArgs() != 1)
				throw new LSPException(
					"normalize-space() function must have 1 argument");

			String a = evalExprAsString(expr.getArg(0)).trim();

			StringBuffer sb = new StringBuffer(a.length());
			boolean inSpace = false;
			for (int i = 0; i<a.length(); i++)
			{
				char c = a.charAt(i);
				if (c > ' ')
				{
					inSpace = false;
					sb.append(c);
				}
				else
				{
					if (!inSpace)
					{
						sb.append(' ');
						inSpace = true;
					}
				}
			}
			return sb.toString();
		}
		else if (expr.getName().equals("translate"))
		{
			if (expr.numberOfArgs() != 3)
				throw new LSPException(
					"translate() function must have 3 arguments");

			String a = evalExprAsString(expr.getArg(0));
			String b = evalExprAsString(expr.getArg(1));
			String c = evalExprAsString(expr.getArg(2));

			StringBuffer sb = new StringBuffer(a.length());
			for (int i = 0; i<a.length(); i++)
			{
				char ch = a.charAt(i);
				int index = b.indexOf(ch);
				if (index < 0)
					sb.append(c);
				else if (index >= c.length())
					;
				else
					sb.append(c.charAt(index));
			}
			return sb.toString();
		}
		else if (expr.getName().equals("boolean"))
		{
			if (expr.numberOfArgs() != 1)
				throw new LSPException(
					"boolean() function must have 1 argument");

			return new Boolean(evalExprAsBoolean(expr.getArg(0)));
		}
		else if (expr.getName().equals("not"))
		{
			if (expr.numberOfArgs() != 1)
				throw new LSPException(
					"not() function must have 1 argument");

			return new Boolean(!evalExprAsBoolean(expr.getArg(0)));
		}
		else if (expr.getName().equals("true"))
		{
			if (expr.numberOfArgs() != 0)
				throw new LSPException(
					"true() function must have no arguments");

			return Boolean.TRUE;
		}
		else if (expr.getName().equals("false"))
		{
			if (expr.numberOfArgs() != 0)
				throw new LSPException(
					"false() function must have no arguments");

			return Boolean.FALSE;
		}
		else if (expr.getName().equals("number"))
		{
			if (expr.numberOfArgs() != 1)
				throw new LSPException(
					"number() function must have 1 argument");

			return new Double(evalExprAsNumber(expr.getArg(0)));
		}
		else if (expr.getName().equals("floor"))
		{
			if (expr.numberOfArgs() != 1)
				throw new LSPException(
					"floor() function must have 1 argument");

			return new Double(Math.floor(evalExprAsNumber(expr.getArg(0))));
		}
		else if (expr.getName().equals("ceiling"))
		{
			if (expr.numberOfArgs() != 1)
				throw new LSPException(
					"ceiling() function must have 1 argument");

			return new Double(Math.ceil(evalExprAsNumber(expr.getArg(0))));
		}
		else if (expr.getName().equals("round"))
		{
			if (expr.numberOfArgs() != 1)
				throw new LSPException(
					"round() function must have 1 argument");

			double a = evalExprAsNumber(expr.getArg(0));

			return new Double(Math.floor(a + 0.5d));
		}
		else if (expr.getName().equals("count"))
		{
			if (expr.numberOfArgs() != 1)
				throw new LSPException(
					"count() function must have 1 argument");

			LSPList list = evalExprAsList(expr.getArg(0));

			try {
				return new Double(list.length());
			}
			catch (IllegalArgumentException e)
			{
				throw new LSPException(
					"Cannot check length of list: " + e.getMessage());	
			}				
		}
		else if (expr.getName().equals("seq"))
		{
			if (expr.numberOfArgs() < 2)
				throw new LSPException(
					"seq() function must have at least 2 arguments");
					
			double start = evalExprAsNumber(expr.getArg(0));
			double end = evalExprAsNumber(expr.getArg(1));
			double step = (expr.numberOfArgs() > 2) 
				? evalExprAsNumber(expr.getArg(2))
				: 1.0;

			Vector vec = new Vector((int)((end-start)/step));
			for (; start <= end; start+=step)
				vec.addElement(new Double(start));
			
			if (DEBUG) System.out.println("seq of length " + vec.size()); 

			return new LSPVectorList(vec);
		}
		else
		{
			throw new LSPException("Unrecognized built-in function: "
				+ expr.getName());
		}
	}


	private Object evalExpr(ExtensionFunctionCall expr) throws SAXException
	{
		LSPExtLib extLib = lookupExtensionHandler(expr.getClassName());
		
		Object[] args = new Object[expr.numberOfArgs()];
		for (int i = 0; i<expr.numberOfArgs(); i++)
		{
			args[i] = evalExpr(expr.getArg(i));
		}
		try {
			return extLib.function(expr.getName(), args);
		}
		catch (IOException e)
		{
			throw new SAXException(e);
		}
	}
	

	private Object evalExpr(VariableReference expr) throws LSPException
	{
		Object o = params.get(expr.getName());
		if (o == null)
			return "";
		else
			return o;
	}

	
	private Object evalExpr(TupleExpr expr) throws SAXException
	{
		LSPTuple tuple = evalExprAsTuple(expr.getBase());
		Object o = tuple.get(expr.getName());
		if (o == null)
			throw new LSPException("Element \'" + expr.getName() 
				+ "\' not found in tuple");
		else
			return o;
	}

	private String convertToString(Object value) throws LSPException
	{
		if (value instanceof String)
		{
			return (String)value;
		}
		else if (value instanceof Double)
		{
			double d = ((Double)value).doubleValue();
			if (d == 0)
				return "0";
			else if (d == Math.rint(d))
				return Long.toString(Math.round(d));
			else
				return Double.toString(d);
		}
		else if (value instanceof Boolean)
		{
			return value.toString();
		}
		else
		{
			throw new LSPException(
				"Convert to String not implemented for type "
				+ value.getClass().getName());
		}
	}

	private String evalExprAsString(LSPExpr expr) throws SAXException
	{
		return convertToString(evalExpr(expr));
	}


	private boolean convertToBoolean(Object value) throws LSPException
	{
		if (value instanceof Boolean)
		{
			return ((Boolean)value).booleanValue();
		}
		if (value instanceof Double)
		{
			double d = ((Double)value).doubleValue();
			return !((d == 0) || Double.isNaN(d));
		}
		if (value instanceof String)
		{
			return ((String)value).length() > 0;
		}
		else
		{
			throw new LSPException(
				"Convert to Boolean not implemented for type "
				+ value.getClass().getName());
		}
	}

	private boolean evalExprAsBoolean(LSPExpr expr) throws SAXException
	{
		return convertToBoolean(evalExpr(expr));
	}


	private double convertToNumber(Object value) throws LSPException
	{
		if (value instanceof Double)
		{
			return ((Double)value).doubleValue();
		}
		else if (value instanceof Boolean)
		{
			return ((Boolean)value).booleanValue() ? 1.0 : 0.0;
		}
		else if (value instanceof String)
		{
			try {
				return Double.valueOf((String)value).doubleValue();
			}
			catch (NumberFormatException e)
			{
				return Double.NaN;
			}
		}
		else
		{
			throw new LSPException(
				"Convert to Number not implemented for type "
				+ value.getClass().getName());
		}
	}

	private double evalExprAsNumber(LSPExpr expr) throws SAXException
	{
		return convertToNumber(evalExpr(expr));
	}

	private LSPList evalExprAsList(LSPExpr expr) throws SAXException
	{
		Object value = evalExpr(expr);
		if (value instanceof LSPList) 
			return (LSPList)value;
		else if (value instanceof Object[]) 
			return new LSPArrayList((Object[])value);
		else if (value instanceof Vector) 
			return new LSPVectorList((Vector)value);
		else if (value instanceof Enumeration) 
			return new LSPEnumerationList((Enumeration)value);
		else
			throw new LSPException(
				"Convert to list not implemented for type "
				+ value.getClass().getName());
	}

	private LSPTuple evalExprAsTuple(LSPExpr expr) throws SAXException
	{
		Object value = evalExpr(expr);
		if (value instanceof LSPTuple) 
			return (LSPTuple)value;
		else if (value instanceof Dictionary) 
			return new LSPDictionaryTuple((Dictionary)value);
		else
			throw new LSPException(
				"Convert to tuple not implemented for type "
				+ value.getClass().getName());
	}

	private LSPExtLib lookupExtensionHandler(String className)
		throws LSPException
	{
		try {
			LSPExtLib extLib = (LSPExtLib)extLibs.get(className);
			if (extLib == null)
			{
				Class extClass = Class.forName(className);
				extLib = (LSPExtLib)extClass.newInstance();
				extLibs.put(className, extLib);
			}
			return extLib;
		}
		catch (ClassNotFoundException e)
		{
			throw new LSPException("Extension class not found: " 
				+ className);
		}
		catch (InstantiationException e)
		{
			throw new LSPException("Unable to instantiate extension class: " 
				+ e.getMessage());
		}
		catch (IllegalAccessException e)
		{
			throw new LSPException("Unable to instantiate extension class: " 
				+ e.getMessage());
		}
		catch (ClassCastException e)
		{
			throw new LSPException("Extension class " + className 
				+ " does not implement the required interface");
		}
	}
	
}

