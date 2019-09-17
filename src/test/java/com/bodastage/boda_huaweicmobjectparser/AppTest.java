package com.bodastage.boda_huaweicmobjectparser;

import java.io.File;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        
        ClassLoader classLoader = getClass().getClassLoader();
        File inFile = new File(classLoader.getResource("gexport.xml").getFile());
        
        HuaweiCMObjectParser parser = new HuaweiCMObjectParser();
        String inputFile = inFile.getAbsolutePath();
        
        String outputFolder = System.getProperty("java.io.tmpdir");
        
        String[] args = { "-i", inputFile, "-o", outputFolder};
        
        assertTrue( true );
        
    }
}
