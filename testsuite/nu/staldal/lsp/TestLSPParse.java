package nu.staldal.lsp;

import java.io.*;

import nu.staldal.syntax.ParseException;

import junit.framework.*;

public class TestLSPParse extends TestCase
{
    public static final String correctExpr = "1+2-3.2*4 div f($a.b,$c)-\'4ad\'";
    public static final String correctTest = "BinaryExpr(BinaryExpr(BinaryExpr(NumberLiteral(1.0),12,NumberLiteral(2.0)),13,BinaryExpr(BinaryExpr(NumberLiteral(3.2),14,NumberLiteral(4.0)),15,FunctionCall(null,f,[TupleExpr(VariableReference(a).b),VariableReference(c)]))),13,StringLiteral(4ad))";

    public static final String invalidExpr = "1+2-a-3.2*f($a.b,$c)";

    public TestLSPParse(String name)
    {
        super(name);
    }

    public void testLSPParse() throws Exception
    {
        LSPExpr correctParsed = LSPExpr.parseFromString(correctExpr);
        assertEquals(correctParsed.toString(), correctTest);
    
        try {
    		LSPExpr invalidParsed = LSPExpr.parseFromString(invalidExpr);
            fail("Parser did not signal error in invalid expression: "
                + invalidExpr);
        }
        catch (ParseException e)
        { }
	}
	
	public static void main(String[] args)
	{
		System.out.println(args[0]);
		try {
			LSPExpr parsed = LSPExpr.parseFromString(args[0]);
			System.out.println(parsed.toString());
        }
        catch (ParseException e)
        {
			System.out.println(LSPUtil.nChars(e.getColumn()-1,' ') 
				+ "^ "+ e.getMessage());
		}
		
	}
}
