/*
 * Copyright (c) 2001, Mikael St�ldal
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

import java.io.*;
import java.util.*;

import org.xml.sax.*;

import nu.staldal.xtree.*;
import nu.staldal.syntax.ParseException;

import nu.staldal.lsp.compile.*;
import nu.staldal.lsp.expr.*;

public class LSPCompiler
{
	private static final boolean DEBUG = true;

    private static final String LSP_CORE_NS = "http://staldal.nu/LSP/core";
    private static final String XML_NS = "http://www.w3.org/XML/1998/namespace";

    private TreeBuilder tb;
    private URLResolver resolver;

    private Hashtable importedFiles;
    private Vector includedFiles;
    private boolean compileDynamic;
    private boolean executeDynamic;

	private int raw;
	private boolean inPi;


	private static LSPException fixSAXException(SAXException e)
	{
		// *** better error message
		return new LSPException(e.toString());
	}


	private static LSPException fixParseException(ParseException e)
	{
		// *** better error message
		return new LSPException("Illegal expression: " + e.getMessage());
	}


    private static LSPException fixIllegalTemplate(String template)
    {
        if (template.length() > 64)
            return new LSPException("Illegal template");
        else
            return new LSPException("Illegal template: " + template);
    }


	private static Vector processTemplate(
        char left, char right, char quot1, char quot2,
        String template)
        throws LSPException
	{
		Vector vector = new Vector(template.length()/16);
		StringBuffer text = new StringBuffer();
		StringBuffer expr = null;
		char quote = 0;
		char brace = 0;

		for (int i = 0; i < template.length(); i++)
		{
			char c = template.charAt(i);
			if (expr == null)
			{
				if (c == left)
				{
					if (brace == 0)
					{
						brace = left;
					}
					else if (brace == left)
					{
						text.append(left);
						brace = 0;
					}
					else if (brace == right)
					{
						throw fixIllegalTemplate(template);
					}
				}
				else if (c == right)
				{
					if (brace == 0)
					{
						brace = right;
					}
					else if (brace == right)
					{
						text.append(right);
						brace = 0;
					}
					else if (brace == left)
					{
						throw fixIllegalTemplate(template);
					}
				}
				else
				{
					if (brace == left)
					{
						if (text.length() > 0)
							vector.addElement(text.toString());
						text = null;

						expr = new StringBuffer();
						expr.append(c);
						brace = 0;
					}
					else if (brace == right)
					{
						throw fixIllegalTemplate(template);
					}
					else
					{
						text.append(c);
					}
				}
			}
			else // expr != null
			{
				if (c == quot1 || c == quot2)
				{
					expr.append(c);
					if (quote == 0)
					{
						quote = c;
					}
					else if (quote == c)
					{
						quote = 0;
					}
				}
				else if (c == right)
				{
					if (quote == 0)
					{
                        String exp = expr.toString();
                        LSPExpr res;
                        try {
                            res = LSPExpr.parseFromString(exp);
                        }
                        catch (ParseException e)
                        {
                            throw fixParseException((ParseException)e);
                        }
                        vector.addElement(res);
						expr = null;
						text = new StringBuffer();
					}
					else
					{
						expr.append(c);
					}
				}
				else
				{
					expr.append(c);
				}
			}
		}

		if (brace != 0)
		{
		    throw fixIllegalTemplate(template);
		}

		if ((text != null) && (text.length() > 0))
			vector.addElement(text.toString());
		text = null;

		return vector;
	}


    public LSPCompiler()
    {
        tb = null;
        resolver = null;
    }


    public ContentHandler startCompile(URLResolver r)
    {
    	importedFiles = new Hashtable();
	    includedFiles = new Vector();
        compileDynamic = false;
        executeDynamic = false;

        resolver = r;
        tb = new TreeBuilder();

        return tb;
    }


    public LSPPage finishCompile()
    	throws LSPException, IOException
    {
        if (tb == null) throw new IllegalStateException(
            "startCompile() must be invoked before finishCompile()");

        Element tree = tb.getTree();

		long startTime = System.currentTimeMillis();
		if (DEBUG) System.out.println("LSP Compile...");

        processImports(tree);

        raw = 0;
        inPi = false;
        LSPNode compiledTree = compileNode(tree);

        tb = null;
        resolver = null;

		long timeElapsed = System.currentTimeMillis()-startTime;
		if (DEBUG) System.out.println("in " + timeElapsed + " ms");

        return new LSPInterpreter(compiledTree, importedFiles, includedFiles,
            compileDynamic, executeDynamic);
    }


    private void processImports(Element el)
    	throws LSPException, IOException
    {
		for (int i = 0; i < el.numberOfChildren(); i++)
		{
			if (!(el.getChild(i) instanceof Element)) continue;

			Element child = (Element)el.getChild(i);

			if ((child.getNamespaceURI() != null)
					&& child.getNamespaceURI().equals(LSP_CORE_NS)
					&& child.getLocalName().equals("import"))
			{
				String url = LSPUtil.getAttr("file", child, true);
				if (LSPUtil.absoluteURL(url))
				{
					compileDynamic = true;
				}
				else
				{
					if (importedFiles.put(url, url) != null)
					{
						// *** check for circular import
					}
				}

				InputSource inputSource = resolver.resolve(url);
				Element importedDoc;
				try {
					importedDoc = TreeBuilder.parseXML(inputSource, false);
				}
				catch (SAXException e)
				{
					throw fixSAXException(e);
				}
				catch (javax.xml.parsers.ParserConfigurationException e)
				{
					throw new Error(e.toString());
				}

				el.replaceChild(importedDoc, i);
				processImports(importedDoc);
			}
			else
			{
				processImports(child);
			}
		}
	}


    private LSPNode compileNode(Node node) throws LSPException
    {
        if (node instanceof Element)
            return compileNode((Element)node);
        else if (node instanceof Text)
            return compileNode((Text)node);
        else if (node instanceof ProcessingInstruction)
            return compileNode((ProcessingInstruction)node);
        else
        	throw new LSPException("Unrecognized XTree Node: "
        		+ node.getClass().getName());
    }


    private LSPNode compileNode(Element el) throws LSPException
    {
		if ((el.getNamespaceURI() != null)
				&& el.getNamespaceURI().equals(LSP_CORE_NS))
		{
			// Dispatch LSP command
			if (el.getLocalName().equals("root"))
			{
				return process_root(el);
			}
			else if (el.getLocalName().equals("processing-instruction"))
			{
				return process_processing_instruction(el);
			}
			else if (el.getLocalName().equals("include"))
			{
				return process_include(el);
			}
			else if (el.getLocalName().equals("if"))
			{
				return process_if(el);
			}
			else if (el.getLocalName().equals("raw"))
			{
				return process_raw(el);
			}
			// *** more to implement
			else
			{
				throw new LSPException("unrecognized LSP command: "
						+ el.getLocalName());
			}
		}
		else
		{
			LSPElement newEl = new LSPElement(
				el.getNamespaceURI(), el.getLocalName(),
				el.numberOfAttributes(), el.numberOfChildren());

			for (int i = 0; i < el.numberOfNamespaceMappings(); i++)
			{
				String[] m = el.getNamespaceMapping(i);
				if (!m[1].equals(LSP_CORE_NS))
					newEl.addNamespaceMapping(m[0], m[1]);
			}

			for (int i = 0; i < el.numberOfAttributes(); i++)
			{
				String URI = el.getAttributeNamespaceURI(i);
				String local = el.getAttributeLocalName(i);
				String type = el.getAttributeType(i);
				String value = el.getAttributeValue(i);

				LSPExpr newValue = (raw > 0)
                    ? new StringLiteral(value)
                    : processTemplateExpr(value);

				newEl.addAttribute(URI, local, type, newValue);
			}

			compileChildren(el, newEl);

			return newEl;
		}
    }


    private LSPNode compileNode(Text text)
        throws LSPException
    {
		if (raw > 0)
		{
			return new LSPText(text.getValue());
		}
		else
		{
			return processTemplateNode(text.getValue());
		}
    }


    private LSPNode compileNode(ProcessingInstruction pi)
        throws LSPException
    {
        return new LSPContainer(0);
    }


    private LSPNode processTemplateNode(String template)
        throws LSPException
    {
		Vector vec = processTemplate('{', '}', '\'', '\"', template);

		LSPContainer container = new LSPContainer(vec.size());

		for (Enumeration e = vec.elements(); e.hasMoreElements(); )
		{
			Object o = e.nextElement();
			if (o instanceof String)
			{
				container.addChild(new LSPText((String)o));
			}
			else if (o instanceof LSPExpr)
			{
				container.addChild(new LSPTemplate((LSPExpr)o));
			}
		}

		if (container.numberOfChildren() == 1)
			return container.getChild(0);
		else
			return container;
	}


    private LSPExpr processTemplateExpr(String template)
        throws LSPException
    {
		Vector vec = processTemplate('{', '}', '\'', '\"', template);

		FunctionCall expr = new FunctionCall(null, "concat", vec.size());

		for (Enumeration e = vec.elements(); e.hasMoreElements(); )
		{
			Object o = e.nextElement();
			if (o instanceof String)
			{
				expr.addArgument(new StringLiteral((String)o));
			}
			else if (o instanceof LSPExpr)
			{
				expr.addArgument((LSPExpr)o);
			}
		}

		if (expr.numberOfArgs() == 0)
			return new StringLiteral("");
		else if (expr.numberOfArgs() == 1)
			return expr.getArg(0);
		else
			return expr;
    }


	private void compileChildren(Element el, LSPContainer container)
		throws LSPException
	{
		for (int i = 0; i < el.numberOfChildren(); i++)
		{
			Node child = el.getChild(i);
			container.addChild(compileNode(child));
		}
	}


	private LSPNode compileChildren(Element el)
		throws LSPException
	{
		if (el.numberOfChildren() == 1)
			return compileNode(el.getChild(0)); // optimization
		else
		{
			LSPContainer container = new LSPContainer(el.numberOfChildren());
			compileChildren(el, container);
			return container;
		}
	}


	private LSPNode process_root(Element el)
		throws LSPException
	{
		return compileChildren(el);
	}


	private LSPNode process_raw(Element el)
		throws LSPException
	{
        raw++;
        LSPNode ret = compileChildren(el);
        raw--;
        return ret;
	}


	private LSPNode process_processing_instruction(Element el)
		throws LSPException
	{
		if (inPi) throw new LSPException(
			"<lsp:processing-instruction> may not be nested");

		LSPExpr name = processTemplateExpr(LSPUtil.getAttr("name", el, true));

		inPi = true;
		LSPNode data = compileChildren(el);
		inPi = false;

		return new LSPProcessingInstruction(name, data);
	}


	private LSPNode process_include(Element el)
		throws LSPException
	{
        LSPExpr file = processTemplateExpr(
			LSPUtil.getAttr("file", el, true));

		if (file instanceof StringLiteral)
		{
			// nothing to do
		}
		else
		{
			executeDynamic = true;
		}

		return new LSPInclude(file);
	}


	private LSPNode process_if(Element el)
		throws LSPException
	{
		try {
			LSPExpr test = LSPExpr.parseFromString(
				LSPUtil.getAttr("test", el, true));

			return new LSPIf(test, compileChildren(el));
		}
		catch (ParseException e)
		{
			throw fixParseException(e);
		}
	}

}
