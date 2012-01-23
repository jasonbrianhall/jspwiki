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
/*
 * (C) Janne Jalkanen 2005
 * 
 */

package org.apache.wiki.action;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.mock.MockRoundtrip;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;


public class WikiContextFactoryTest extends TestCase
{
    TestEngine m_engine;
    WikiContextFactory resolver;

    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        props.put( WikiEngine.PROP_MATCHPLURALS, "yes" );
        m_engine = new TestEngine( props );
        resolver = m_engine.getWikiContextFactory();
        m_engine.saveText( "SinglePage", "This is a test." );
        m_engine.saveText( "PluralPages", "This is a test." );
    }
    
    protected void tearDown() throws Exception
    {
        if ( m_engine.pageExists( "TestPage" ) )
        {
            m_engine.deletePage( "TestPage" );
        }
        m_engine.shutdown();
    }
    
    public void testNewActionBean() throws WikiException
    {
        WikiContext context;
        MockRoundtrip trip = m_engine.guestTrip( ViewActionBean.class );
        MockHttpServletRequest request = trip.getRequest();
        MockHttpServletResponse response = trip.getResponse();
        
        // Supplying an EditActionBean means the EDIT action
        context = resolver.newContext( request, response, WikiContext.EDIT );
        assertEquals( WikiContext.EDIT, context.getRequestContext() );
        assertNull( context.getPage() );
        
        // Change the context to "preview"
        context.setRequestContext( WikiContext.PREVIEW );
        assertEquals( WikiContext.PREVIEW, context.getRequestContext() );
        
        // Change the context to "diff"
        context.setRequestContext( WikiContext.DIFF);
        assertEquals( WikiContext.DIFF, context.getRequestContext() );
        
        // Try changing the context to "comment" (but, this is an error)
        try
        {
            context.setRequestContext( WikiContext.COMMENT);
        }
        catch ( IllegalArgumentException e )
        {
            // Excellent. This what we expect.
        }
        
        // Supplying the PrefsActionBean means the PREFS context
        context = resolver.newContext( request, response, WikiContext.PREFS );
        assertEquals( WikiContext.PREFS, context.getRequestContext() );
        
        // Supplying the GroupActionBean means the VIEW_GROUP context
        context = resolver.newContext( request, response, WikiContext.VIEW_GROUP );
        assertEquals( WikiContext.VIEW_GROUP, context.getRequestContext() );
    }
    
    public void testNewActionBeanByJSP() throws WikiException
    {
        WikiContext context;
        MockRoundtrip trip = m_engine.guestTrip( ViewActionBean.class );
        MockHttpServletRequest request = trip.getRequest();
        MockHttpServletResponse response = trip.getResponse();
        MockHttpSession session = (MockHttpSession)request.getSession();

        // Request for "UserPreference.jsp" should resolve to PREFS action
        request = new MockHttpServletRequest( m_engine.getServletContext().getServletContextName(), "/UserPreferences.jsp");
        request.setSession( session );
        context = resolver.newContext( request, response, WikiContext.PREFS );
        assertEquals( WikiContext.PREFS, context.getRequestContext() );
        
        // We don't care about JSPs not mapped to actions, because the bean we get only depends on the class we pass
        // FIXME: this won't work because WikiActionBeanResolver doesn't keep a cache of URLBindings 
        request = new MockHttpServletRequest( m_engine.getServletContext().getServletContextName(), "/NonExistent.jsp");
        request.setSession( session );
        context = resolver.newContext( request, response, WikiContext.EDIT );
        assertEquals( WikiContext.EDIT, context.getRequestContext() );
        assertNull( context.getPage() );
    }
    
    public void testActionBeansWithParams() throws Exception
    {
        WikiContext context;
        WikiPage page = m_engine.getPage( "SinglePage" );
        MockRoundtrip trip = m_engine.guestTrip( ViewActionBean.class );
        MockHttpServletRequest request = trip.getRequest();
        MockHttpServletResponse response = trip.getResponse();
        MockHttpSession session = (MockHttpSession)request.getSession();
        
        // Passing an EDIT request with page param yields an ActionBean with a non-null page property
        request = new MockHttpServletRequest( m_engine.getServletContext().getServletContextName(), "/Edit.jsp");
        request.setSession( session );
        request.getParameterMap().put( "page", new String[]{"SinglePage"} );
        context = resolver.newContext( request, response, WikiContext.EDIT );
        assertEquals( WikiContext.EDIT, context.getRequestContext() );
        assertEquals( page, context.getPage() );
        
        // Passing a VIEW request with page=Search yields an ordinary page name, not a special page or JSP
        // FIXME: this won't work because WikiActionBeanResolver doesn't keep a cache of URLBindings 
        request = new MockHttpServletRequest( m_engine.getServletContext().getServletContextName(), "/Wiki.jsp");
        request.setSession( session );
        request.getParameterMap().put( "page", new String[]{"Search"} );
        context = resolver.newContext( request, response, WikiContext.VIEW );
        assertEquals( WikiContext.VIEW, context.getRequestContext() );
        
        // Passing a VIEW_GROUP request with group="Art" gets a ViewGroupActionBean
        request = new MockHttpServletRequest( m_engine.getServletContext().getServletContextName(), "/Wiki.jsp");
        request.setSession( session );
        request.getParameterMap().put( "group", new String[]{"Art"} );
        context = resolver.newContext( request, response, WikiContext.VIEW_GROUP );
        assertEquals( WikiContext.VIEW_GROUP, context.getRequestContext() );
    }

    public static Test suite()
    {
        return new TestSuite( WikiContextFactoryTest.class );
    }
}
