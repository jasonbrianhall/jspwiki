/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.    
 */

package org.apache.wiki;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.wiki.NoSuchVariableException;
import org.apache.wiki.VariableManager;
import org.apache.wiki.WikiContext;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.preferences.Preferences.TimeFormat;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class VariableManagerTest extends TestCase
{
    VariableManager m_variableManager;
    WikiContext     m_context;

    static final String PAGE_NAME = "TestPage";
    
    TestEngine testEngine = null;

    public VariableManagerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        Properties props = new Properties();
        try
        {
            props.load( TestEngine.findTestProperties() );

            m_variableManager = new VariableManager( props );
            testEngine = new TestEngine( props );
            testEngine.deletePage( PAGE_NAME );
            m_context = testEngine.getWikiContextFactory().newViewContext( testEngine.createPage(WikiPath.valueOf(PAGE_NAME)) );

        }
        catch( IOException e ) {}
    }

    public void tearDown()
    {
        testEngine.shutdown();
    }

    public void testIllegalInsert1()
        throws Exception
    {
        try
        {
            m_variableManager.parseAndGetValue( m_context, "" );
            fail( "Did not fail" );
        }
        catch( IllegalArgumentException e )
        {
            // OK.
        }
    }

    public void testIllegalInsert2()
        throws Exception
    {
        try
        {
            m_variableManager.parseAndGetValue( m_context, "{$" );
            fail( "Did not fail" );
        }
        catch( IllegalArgumentException e )
        {
            // OK.
        }
    }

    public void testIllegalInsert3()
        throws Exception
    {
        try
        {
            m_variableManager.parseAndGetValue( m_context, "{$pagename" );
            fail( "Did not fail" );
        }
        catch( IllegalArgumentException e )
        {
            // OK.
        }
    }

    public void testIllegalInsert4()
        throws Exception
    {
        try
        {
            m_variableManager.parseAndGetValue( m_context, "{$}" );
            fail( "Did not fail" );
        }
        catch( IllegalArgumentException e )
        {
            // OK.
        }
    }

    public void testNonExistantVariable()
    {
        try
        {
            m_variableManager.parseAndGetValue( m_context, "{$no_such_variable}" );
            fail( "Did not fail" );
        }
        catch( NoSuchVariableException e )
        {
            // OK.
        }
    }

    public void testPageName()
        throws Exception
    {
        String res = m_variableManager.getValue( m_context, "pagename" );

        assertEquals( PAGE_NAME, res );
    }

    public void testPageName2()
        throws Exception
    {
        String res =  m_variableManager.parseAndGetValue( m_context, "{$  pagename  }" );

        assertEquals( PAGE_NAME, res );
    }

    public void testMixedCase()
        throws Exception
    {
        String res =  m_variableManager.parseAndGetValue( m_context, "{$PAGeNamE}" );

        assertEquals( PAGE_NAME, res );
    }

    public void testExpand1()
        throws Exception
    {
        String res = m_variableManager.expandVariables( m_context, "Testing {$pagename}..." );

        assertEquals( "Testing "+PAGE_NAME+"...", res );
    }

    public void testExpand2()
        throws Exception
    {
        String res = m_variableManager.expandVariables( m_context, "{$pagename} tested..." );

        assertEquals( PAGE_NAME+" tested...", res );
    }

    public void testExpand3()
        throws Exception
    {
        String res = m_variableManager.expandVariables( m_context, "Testing {$pagename}, {$applicationname}" );

        assertEquals( "Testing "+PAGE_NAME+", JSPWiki", res );
    }

    public void testExpand4()
        throws Exception
    {
        String res = m_variableManager.expandVariables( m_context, "Testing {}, {{{}" );

        assertEquals( "Testing {}, {{{}", res );
    }

    public void testTimeStamp1() throws Exception
    {
        // Yes I know there is a tiny chance that this fails if the minute
        // passes by between here and the "new Date()" in VariableManager
        //
        String format = "dd.MM.yyyy HH:mm";
        SimpleDateFormat df = new SimpleDateFormat( format );
        String dateString = df.format( new Date() );
        String res = m_variableManager.expandVariables( m_context, ">>>>>{$timestamp format=" + format + "}<<<<<" );
        assertEquals( ">>>>>" + dateString + "<<<<<", res );
    }

    public void testTimeStamp2() throws Exception
    {
        // test for default dateformat
        //
        Preferences prefs = Preferences.getPreferences( m_context.getHttpRequest() );
        String defaultDateFormat = prefs.getDateFormat( TimeFormat.DATETIME ).format( new Date() );

        String res = m_variableManager.expandVariables( m_context, ">>>>>{$timestamp}<<<<<" );
        assertEquals( ">>>>>" + defaultDateFormat + "<<<<<", res );
    }

    public void testTimeStamp3() throws Exception
    {
        // test with a wrong named parm
        //
        String res = m_variableManager.expandVariables( m_context, ">>>>>{$timestamp wrongparm=bla}<<<<<" );
        assertEquals( ">>>>>Unrecognized parameter:wrongparm<<<<<", res );
    }

    public void testTimeStamp4() throws Exception
    {
        // tests with an invalid date format
        //
        String format = "dd.MM.yyyy HH:mm";
        String res = m_variableManager.expandVariables( m_context, ">>>>>{$timestamp format=" + format + "INVALIDFORMAT}<<<<<" );
        assertEquals( ">>>>>No valid dateformat was provided: dd.MM.yyyy HH:mmINVALIDFORMAT<<<<<", res );
    }

    public static Test suite()
    {
        return new TestSuite( VariableManagerTest.class );
    }
}
