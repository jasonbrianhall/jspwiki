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

package org.apache.wiki.content.inspect;

import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.util.CryptoUtil;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.ContentManager;

/**
 */
public class SpamInspectionPlanTest extends TestCase
{
    public static Test suite()
    {
        return new TestSuite( SpamInspectionPlanTest.class );
    }

    private Properties m_props = new Properties();

    private TestEngine m_engine;

    private static final float PERFECT_SCORE = 0f;

    public SpamInspectionPlanTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        m_props.load( TestEngine.findTestProperties() );
        m_props.setProperty( InspectionPlan.PROP_BANTIME, "1" );
    }

    public void tearDown() throws Exception
    {
        m_engine.shutdown();
    }

    /**
     * Tests whether the {@link BanListInspector works}. The weight of this test
     * is 2f.
     * 
     * @throws Exception
     */
    public void testBanListInspector() throws Exception
    {
        initDefaultPlanProperties();
        m_engine = new TestEngine( m_props );

        // Create a new HTTP request. Make sure to add the BotTrap/UTF-8
        // parameters.
        MockHttpServletRequest request = new MockHttpServletRequest( "/", "/" );
        MockHttpSession session = new MockHttpSession( m_engine.getServletContext() );
        request.setSession( session );
        setupSpamParams( request );

        // Add the IP address to the ban list.
        Inspection inspection = createInspection( request );
        inspection.getPlan().getReputationManager().banHost( request );

        // Running the inspection should cause the BanListInspector to fail
        String newText = "Sample text";
        inspection.inspect( Change.getChange( "page", newText ) );
        assertEquals( -2f, inspection.getScore( Topic.SPAM ) );
    }

    /**
     * Tests whether the {@link ChangeRateInspector} works for detecting
     * excessive number of changes per minute. The weight of this test is 4f.
     * 
     * @throws Exception
     */
    public void testChangeRateInspectorVelocity() throws Exception
    {
        initDefaultPlanProperties();
        m_engine = new TestEngine( m_props );

        // Create a new HTTP request. Make sure to add the BotTrap/UTF-8
        // parameters.
        MockHttpServletRequest request = new MockHttpServletRequest( "/", "/" );
        MockHttpSession session = new MockHttpSession( m_engine.getServletContext() );
        request.setSession( session );
        setupSpamParams( request );

        // Running inspection with simple text change should be fine
        Inspection inspection = createInspection( request );
        String newText = "Sample text";
        inspection.inspect( Change.getChange( "page", newText ) );
        assertEquals( PERFECT_SCORE, inspection.getScore( Topic.SPAM ) );

        // Now, make 100 more (slightly different) changes
        for( int i = 0; i < 100; i++ )
        {
            inspection = createInspection( request );
            newText = "Sample text change " + i;
            inspection.inspect( Change.getChange( "page", newText ) );
        }
        // Our change-rate check should fail
        assertEquals( -4f, inspection.getScore( Topic.SPAM ) );
    }

    /**
     * Tests whether the {@link LinkCountInspector} works for detecting
     * excessive number of changes per minute. The weight of this test is 4f.
     * 
     * @throws Exception
     */
    public void testLinkCountInspector() throws Exception
    {
        initDefaultPlanProperties();
        m_engine = new TestEngine( m_props );

        // Create a new HTTP request. Make sure to add the BotTrap/UTF-8
        // parameters.
        MockHttpServletRequest request = new MockHttpServletRequest( "/", "/" );
        MockHttpSession session = new MockHttpSession( m_engine.getServletContext() );
        request.setSession( session );
        setupSpamParams( request );

        // Running inspection with simple text change should be fine
        Inspection inspection = createInspection( request );
        String newText = "Sample text";
        inspection.inspect( Change.getChange( "page", newText ) );
        assertEquals( PERFECT_SCORE, inspection.getScore( Topic.SPAM ) );

        // Now, make a change with 3 URLs in it (1 more than limit)
        newText = "http://www.jspwiki.org mailto:janne@ecyrd.com https://www.freshcookies.org";
        inspection.inspect( Change.getChange( "page", newText ) );

        // Link-count check should fail
        assertEquals( -8f, inspection.getScore( Topic.SPAM ) );
    }

    /**
     * Tests whether the {@link ChangeRateInspector} works for detecting
     * excessive number of changes per minute. The weight of this test is 4f.
     * 
     * @throws Exception
     */
    public void testChangeRateInspectorSimilarity() throws Exception
    {
        initDefaultPlanProperties();
        m_engine = new TestEngine( m_props );

        // Create a new HTTP request. Make sure to add the BotTrap/UTF-8
        // parameters.
        MockHttpServletRequest request = new MockHttpServletRequest( "/", "/" );
        MockHttpSession session = new MockHttpSession( m_engine.getServletContext() );
        request.setSession( session );
        setupSpamParams( request );

        // Running inspection with simple text change should be fine
        Inspection inspection = createInspection( request );
        String newText = "Sample text";
        inspection.inspect( Change.getChange( "page", newText ) );
        assertEquals( PERFECT_SCORE, inspection.getScore( Topic.SPAM ) );

        // Now, make 50 more identical changes
        newText = "Identical text change";
        for( int i = 0; i < 50; i++ )
        {
            inspection = createInspection( request );
            inspection.inspect( Change.getChange( "page", newText ) );
        }
        inspection.inspect( Change.getChange( "page", newText ) );
        // Our similarity check should fail
        assertEquals( -4f, inspection.getScore( Topic.SPAM ) );
    }
    
    /**
     * Tests the BotTrapInspector. This inspector looks checks to see if 1) the
     * UTF-8 parameter is supplied intact and not mangled; 2) that a "trap"
     * parameter that is meant to be empty isn't filled in by random data by a
     * bot; and that 3) that an encrypted "token" parameter is present. The
     * weight of this test is 16f.
     * 
     * @throws Exception
     */
    public void testBotTrapInspector() throws Exception
    {
        initDefaultPlanProperties();
        m_engine = new TestEngine( m_props );

        // Create a new HTTP request. Make sure to add the BotTrap/UTF-8
        // parameters.
        MockHttpServletRequest request = new MockHttpServletRequest( "/", "/" );
        MockHttpSession session = new MockHttpSession( m_engine.getServletContext() );
        request.setSession( session );
        setupSpamParams( request );

        // Running the inspection with a simple text message should not trip
        // anything
        Inspection inspection = createInspection( request );
        String newText = "Sample text";
        inspection.inspect( Change.getChange( "page", newText ) );
        assertEquals( PERFECT_SCORE, inspection.getScore( Topic.SPAM ) );

        // Removing the UTF-8 token suggests we have a bot
        request.getParameterMap().remove( BotTrapInspector.REQ_ENCODING_CHECK );
        inspection = createInspection( request );
        inspection.inspect( Change.getChange( "page", newText ) );
        assertEquals( -16f, inspection.getScore( Topic.SPAM ) );

        // Removing the encrypted spam param suggests we have a bot
        setupSpamParams( request );
        request.getParameterMap().remove( BotTrapInspector.REQ_SPAM_PARAM );
        inspection = createInspection( request );
        inspection.inspect( Change.getChange( "page", newText ) );
        assertEquals( -16f, inspection.getScore( Topic.SPAM ) );

        // Supplying a non-null value for the first (trap) spam param should
        // trigger the bot trap
        setupSpamParams( request );
        request.getParameterMap().put( BotTrapInspector.REQ_TRAP_PARAM, new String[] { "botSuppliedValue" } );
        inspection = createInspection( request );
        inspection.inspect( Change.getChange( "page", newText ) );
        assertEquals( -16f, inspection.getScore( Topic.SPAM ) );

        // Removing the second (token) spam param should trip it also
        setupSpamParams( request );
        request.getParameterMap().remove( "TOKENA" );
        inspection = createInspection( request );
        inspection.inspect( Change.getChange( "page", newText ) );
        assertEquals( -16f, inspection.getScore( Topic.SPAM ) );

        // / Re-run the inspection with all parameters intact
        setupSpamParams( request );
        inspection.inspect( Change.getChange( "page", newText ) );
        assertEquals( PERFECT_SCORE, inspection.getScore( Topic.SPAM ) );
    }

    public void testGetInspectionPlan() throws Exception
    {
        m_engine = new TestEngine( m_props );

        InspectionPlan plan = SpamInspectionPlan.getInspectionPlan( m_engine );
        Inspector[] inspectors = plan.getInspectors();
        assertEquals( 7, inspectors.length );
        assertEquals( UserInspector.class, inspectors[0].getClass() );
        assertEquals( BanListInspector.class, inspectors[1].getClass() );
        assertEquals( ChangeRateInspector.class, inspectors[2].getClass() );
        assertEquals( LinkCountInspector.class, inspectors[3].getClass() );
        assertEquals( BotTrapInspector.class, inspectors[4].getClass() );
        assertEquals( AkismetInspector.class, inspectors[5].getClass() );
        assertEquals( PatternInspector.class, inspectors[6].getClass() );
    }

    public void testGetScoreLimit() throws Exception
    {
        m_engine = new TestEngine( m_props );

        SpamInspectionPlan plan = SpamInspectionPlan.getInspectionPlan( m_engine );
        // the value in the test properties file
        assertEquals( -0.5f, plan.getSpamLimit() );
    }

    public void testGetWeight() throws Exception
    {
        String key = SpamInspectionPlan.PROP_INSPECTOR_WEIGHT_PREFIX + BanListInspector.class.getCanonicalName();
        m_props.put( key, "-2.0f" );
        m_engine = new TestEngine( m_props );
        SpamInspectionPlan plan = SpamInspectionPlan.getInspectionPlan( m_engine );
        assertEquals( -2.0f, plan.getWeight( m_props, BanListInspector.class ) );
        assertEquals( SpamInspectionPlan.DEFAULT_WEIGHT, plan.getWeight( m_props, UserInspector.class ) );
    }

    public void testSetScoreLimit() throws Exception
    {
        m_props.put( SpamInspectionPlan.PROP_SCORE_LIMIT, "-2.0f" );
        m_engine = new TestEngine( m_props );
        SpamInspectionPlan plan = SpamInspectionPlan.getInspectionPlan( m_engine );
        assertEquals( -2.0f, plan.getSpamLimit() );
    }

    private Inspection createInspection( HttpServletRequest request ) throws Exception
    {
        InspectionPlan plan = SpamInspectionPlan.getInspectionPlan( m_engine );
        plan.getReputationManager().unbanHost( request );
        WikiPage page = m_engine.getFrontPage( ContentManager.DEFAULT_SPACE );
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( request, null, page );
        Inspection inspection = new Inspection( context, plan );
        return inspection;
    }
    
    private void initDefaultPlanProperties()
    {
        // Make sure all of the Inspectors always run (disable the score limits)
        m_props.put( SpamInspectionPlan.PROP_SCORE_LIMIT, "-1000f" );

        // Define predictable weights for each Inspector
        setWeight( UserInspector.class, 1f );
        setWeight( BanListInspector.class, 2f );
        setWeight( ChangeRateInspector.class, 4f );
        setWeight( LinkCountInspector.class, 8f );
        setWeight( BotTrapInspector.class, 16f );
        setWeight( AkismetInspector.class, 0f );
        setWeight( PatternInspector.class, 32f );

        // Set predictable defaults for various Inspectors
        m_props.put( LinkCountInspector.PROP_MAXURLS, "2" );
        m_props.put( ChangeRateInspector.PROP_PAGECHANGES, "100" );
        m_props.put( ChangeRateInspector.PROP_SIMILARCHANGES, "50" );
    }

    private void setupSpamParams( MockHttpServletRequest request )
    {
        Map<String, String[]> parameters = request.getParameterMap();
        parameters.put( BotTrapInspector.REQ_TRAP_PARAM, new String[0] );
        parameters.put( "TOKENA", new String[] { request.getSession().getId() } );
        String paramValue = CryptoUtil.encrypt( "TOKENA" );
        parameters.put( BotTrapInspector.REQ_SPAM_PARAM, new String[] { paramValue } );
        parameters.put( BotTrapInspector.REQ_ENCODING_CHECK, new String[] { "\u3041" } );
    }

    private void setWeight( Class<? extends Inspector> inspectorClass, float weight )
    {
        String key = SpamInspectionPlan.PROP_INSPECTOR_WEIGHT_PREFIX + inspectorClass.getCanonicalName();
        m_props.setProperty( key, String.valueOf( weight ) );
    }
}
