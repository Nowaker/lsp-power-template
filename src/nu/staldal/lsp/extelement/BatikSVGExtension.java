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

package nu.staldal.lsp.extelement;

import java.io.*;
import java.net.URL;

import org.xml.sax.*;
import org.xml.sax.helpers.AttributesImpl;
import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.*;
import org.apache.batik.dom.svg.*;

import nu.staldal.lsp.*;
import nu.staldal.lagoon.core.*;
import nu.staldal.lagoon.util.*;


public class BatikSVGExtension implements LSPExtLib
{
	private static boolean DEBUG = true;	
	
	private ImageTranscoder transcoder;
	private SAXSVGDocumentFactory docFactory;
	private ContentHandler out;
	private Target target;
	private SourceManager sourceMan;
	private int imageNumber;
	
	public BatikSVGExtension()
	{
		if (DEBUG) System.out.println("new BatikSVGExtension");
		transcoder = new PNGTranscoder();
		imageNumber = 0;
		docFactory = null;
		out = null;
		target = null;
		sourceMan = null;		
	}
	
	
	/**
	 * Invoked before the element is sent.
	 *
	 * @param out     where to write XML output.
	 * @param target  the current target
	 *
	 * @return  a ContentHandler to send input to.
	 */
	public ContentHandler beforeElement(ContentHandler out, 
		Target target, SourceManager sourceMan)
		throws SAXException
	{
		this.out = out;
		this.target = target;
		this.sourceMan = sourceMan;
		docFactory = new SAXSVGDocumentFactory();
		docFactory.prepareDocument();
		docFactory.startDocument();
		return new ContentHandlerFixer(docFactory);
	}

	
	/**
	 * Invoked after the element is sent.
	 *
	 * @return  a string output.
	 */
	public String afterElement()
		throws SAXException, IOException
	{	
		URL sourceURL;
		String _sourceURL = sourceMan.getSourceURL();
		if (LagoonUtil.absoluteURL(_sourceURL))
			sourceURL = new URL(_sourceURL);
		else if (LagoonUtil.pseudoAbsoluteURL(_sourceURL))
			sourceURL = new java.net.URL(sourceMan.getRootDir().toURL(),
				_sourceURL.substring(1));
		else
			sourceURL = new java.net.URL(sourceMan.getRootDir().toURL(),
				_sourceURL);
							
		if (DEBUG) System.out.println("The source URL: " + sourceURL);
		
		int slash = _sourceURL.lastIndexOf('/');
		String sourceName = 
			(slash < 0) ? _sourceURL : _sourceURL.substring(slash+1); 
		String imageName = sourceName + "_image" + (++imageNumber) + ".png";

		docFactory.endDocument();
		
		SVGOMDocument doc = docFactory.getDocument(sourceURL); 

		if (DEBUG) System.out.println("Batik SVG DOM building complete");
        TranscoderInput input = new TranscoderInput(doc);

		OutputHandler oh = ((FileTarget)target).newAsyncTarget(imageName, false);
		
        TranscoderOutput output = new TranscoderOutput(oh.getOutputStream());
        try {
			if (DEBUG) System.out.println("about to transcode");
			transcoder.transcode(input, output);
			if (DEBUG) System.out.println("transcoding complete");
		} catch(TranscoderException e) {
			throw new SAXException(e);
		}
		
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("", "src", "", "CDATA", imageName);
		atts.addAttribute("", "alt", "", "CDATA", "");
		out.startElement("" /* *** "http://www.w3.org/1999/xhtml" */, "img", "", atts);
		out.endElement("" /* *** "http://www.w3.org/1999/xhtml" */, "img", "");

		docFactory = null;
		out = null;
		target = null;		
		sourceMan = null;		
	
		return null;
	}
		
}
