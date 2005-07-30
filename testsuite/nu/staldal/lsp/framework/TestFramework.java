package nu.staldal.lsp.framework;

import junit.framework.*;


public class TestFramework extends TestCase
{
    public TestFramework(String name)
    {
        super(name);
    }

    private DispatcherServlet dispatcherServlet;
    
    public  void setUp()
    {
        dispatcherServlet = new DispatcherServlet();
    }
    
    public void testFixServiceName() throws Exception
    {
        assertEquals("foobar", dispatcherServlet.fixServiceName("foobar"));
        assertEquals("foobar", dispatcherServlet.fixServiceName("/foobar"));
        assertEquals("foobar", dispatcherServlet.fixServiceName("foobar.s"));
        assertEquals("foobar", dispatcherServlet.fixServiceName("/foobar.s"));
        assertEquals("foobar", dispatcherServlet.fixServiceName("foobar.sss"));
        
        assertEquals("foo/bar", dispatcherServlet.fixServiceName("foo/bar"));
        assertEquals("foo/bar", dispatcherServlet.fixServiceName("/foo/bar"));
        assertEquals("foo/bar", dispatcherServlet.fixServiceName("foo/bar.s"));
        assertEquals("foo/bar", dispatcherServlet.fixServiceName("/foo/bar.s"));
        
        assertEquals("", dispatcherServlet.fixServiceName(""));
        assertEquals("", dispatcherServlet.fixServiceName("/"));
        assertEquals("", dispatcherServlet.fixServiceName("/."));
        assertEquals("", dispatcherServlet.fixServiceName("/.s"));
        assertEquals("", dispatcherServlet.fixServiceName("/.sss"));
        assertEquals("", dispatcherServlet.fixServiceName(null));
	}
	
}
