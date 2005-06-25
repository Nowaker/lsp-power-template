package test.services;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import nu.staldal.lsp.framework.*;


public class Include2 implements Service
{
    
    public void init()
        throws ServletException
    {
        // nothing to do
    }
         
    
    public String execute(ServletContext context, 
                HttpServletRequest request, HttpServletResponse response,
                Map pageParams, int requestType)
        throws ServletException, IOException
    {
        pageParams.put("foo", request.getAttribute(INCLUDE_ATTR_PREFIX+"myattr2"));        
        
        return "Include2";
    }
    
    
    public void destroy()
    {
        // nothing to do
    }
}

