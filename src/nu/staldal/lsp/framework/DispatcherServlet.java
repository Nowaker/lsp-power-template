/*
 * Copyright (c) 2005, Mikael St�ldal
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

package nu.staldal.lsp.framework;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.xml.sax.SAXException;

import nu.staldal.lsp.*;
import nu.staldal.lsp.servlet.*;


/**
 * Dispatcher Servlet for LSP framework.
 */
public class DispatcherServlet extends HttpServlet
{
    private LSPManager lspManager;
    private Map serviceCache;
    
    private List servicePackages;
    private String defaultService;

    
    public void init()
        throws ServletException
    {
        String sp = getInitParameter("ServicePackages");
        if (sp == null)
        {
            throw new ServletException("ServicePackages parameter missing");    
        }
        servicePackages = new ArrayList();
        StringTokenizer st = new StringTokenizer(sp, ",");
        while (st.hasMoreTokens())
        {
            servicePackages.add(st.nextToken());
        }
        if (servicePackages.isEmpty())
        {
            throw new ServletException("No ServicePackages specified");    
        }        
        
        defaultService = getInitParameter("DefaultService");
        
        lspManager = LSPManager.getInstance(getServletContext(),
            Thread.currentThread().getContextClassLoader());
        
        serviceCache = new HashMap();                
    }
         

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        doService(request, response, false);
    }
    

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        doService(request, response, true);
    }

    
    private void doService(HttpServletRequest request, HttpServletResponse response,
            boolean isPost)
        throws ServletException, IOException
    {
        String serviceName = fixServiceName(request.getServletPath());

        Map lspParams = new HashMap();
        while (true)
        {        
            Service service;
            try {
                service = lookupService(serviceName);
            }
            catch (InstantiationException e)
            {
                throw new ServletException("Unable to create service", e);    
            }
            catch (IllegalAccessException e)
            {
                throw new ServletException("Unable to create service", e);    
            }
            
            if (service == null)
            {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, 
                    "Service \'"+serviceName+"\' not found");    
                return;
            }
    
            String templateName = 
                service.execute(getServletContext(), request, response, lspParams, isPost);
            if (templateName == null || templateName.length() == 0)
            {
                break;        
            }
            else if (templateName.charAt(0) == '*')
            { 
                // Forward
                serviceName = templateName.substring(1);
                continue;
            }
            else 
            {   // LSP page        
                LSPPage lspPage = lspManager.getPage(templateName);
                if (lspPage == null)
                {
                    throw new ServletException("Template \'"+templateName+"\' not found");
                }           
                    
                try {		
                    lspManager.executePage(lspPage, lspParams, request, response);
                }
                catch (SAXException e)
                {
                    Exception ee = e.getException();
                    if (ee == null)
                        throw new ServletException(e);
                    if (ee instanceof IOException)
                        throw (IOException)ee;
                    else if (ee instanceof ServletException)
                        throw (ServletException)ee;
                    else if (ee instanceof RuntimeException)
                        throw (RuntimeException)ee;
                    else
                        throw new ServletException(ee);
                }
                break;
            }
        }
    }
    
    
    public void destroy()
    {
        for (Iterator it = serviceCache.values().iterator(); it.hasNext(); )
        {
            Service s = (Service)it.next();
            
            s.destroy();
        }
        
        serviceCache.clear();
    }

    
    /**
     * Strip leading '/' and extension. 
     * Never return <code>null</code>
     */
    static String fixServiceName(String serviceName)
    {
        if (serviceName == null) return "";

        int startPos = serviceName.startsWith("/") ? 1 : 0;

        int dot = serviceName.lastIndexOf('.');
        if (dot < 0) dot = serviceName.length();
        
        return serviceName.substring(startPos, dot);
    }    
    
    
    /**
     * @return <code>null</code> if not found
     */
    private synchronized Service lookupService(String serviceName)
        throws InstantiationException, IllegalAccessException, ServletException
    {
        Service s = (Service)serviceCache.get(serviceName);
        
        if (s == null)
        {
            String name = serviceName;
            if (serviceName.length() == 0) // default service
            {
                if (defaultService == null)
                    return null;
                else                    
                    name = defaultService;    
            }
            
            Class serviceClass = null;
            
            for (Iterator it = servicePackages.iterator(); it.hasNext(); )
            {
                String packageName = (String)it.next();
                String className = packageName + '.' + name;
                
                try {
                    serviceClass = Class.forName(className, true, 
                        lspManager.getClassLoader());
                    break;
                }
                catch (ClassNotFoundException ignore) {}
            }
            
            if (serviceClass == null) return null;
            
            s = (Service)serviceClass.newInstance();
            s.init();
            
            serviceCache.put(serviceName, s);
        }
        
        return s;
    }
    
}
