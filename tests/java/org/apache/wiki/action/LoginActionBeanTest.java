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
package org.apache.wiki.action;

import java.util.Properties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.action.LoginActionBean;
import org.apache.wiki.auth.SessionMonitor;
import org.apache.wiki.auth.Users;
import org.apache.wiki.auth.login.CookieAssertionLoginModule;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.validation.ValidationErrors;


public class LoginActionBeanTest extends TestCase
{
    public static Test suite()
    {
        return new TestSuite( LoginActionBeanTest.class );
    }

    TestEngine m_engine;

    public void setUp()
    {
        // Start the WikiEngine, and stash reference
        Properties props = new Properties();
        try
        {
            props.load( TestEngine.findTestProperties() );
            m_engine = new TestEngine( props );
        }
        catch( Exception e )
        {
            throw new RuntimeException( "Could not set up TestEngine: " + e.getMessage() );
        }
    }

    public void tearDown()
    {
        m_engine.shutdown();
    }

    public void testLogin() throws Exception
    {
        MockRoundtrip trip;
        LoginActionBean bean;
        ValidationErrors errors;

        // Verify that the initial user is anonymous
        trip = m_engine.guestTrip( "/Login.jsp" );
        HttpServletRequest request = trip.getRequest();
        WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( request.getSession() );
        assertNotSame( Users.JANNE, wikiSession.getLoginPrincipal().getName() );

        // Log in
        trip.setParameter( "j_username", Users.JANNE );
        trip.setParameter( "j_password", Users.JANNE_PASS );
        trip.execute( "login" );

        // Verify we logged in correctly
        bean = trip.getActionBean( LoginActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( Users.JANNE, wikiSession.getLoginPrincipal().getName() );
        assertEquals( "/Wiki.jsp", trip.getDestination() );

        // Should be just one cookie (the JSPWiki asserted name cookie)
        Cookie[] cookies = trip.getResponse().getCookies();
        assertEquals( 1, cookies.length );
        assertEquals( CookieAssertionLoginModule.PREFS_COOKIE_NAME, cookies[0].getName() );
        assertEquals( "Janne+Jalkanen", cookies[0].getValue() );
    }

    public void testLoginNoParams() throws Exception
    {
        MockRoundtrip trip;
        LoginActionBean bean;
        ValidationErrors errors;

        // Verify that the initial user is anonymous
        trip = m_engine.guestTrip( "/Login.jsp" );
        HttpServletRequest request = trip.getRequest();
        WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( request.getSession() );
        assertNotSame( Users.JANNE, wikiSession.getLoginPrincipal().getName() );

        // Log in; should see two errors (because we did not specify a username
        // or password)
        trip.execute( "login" );
        bean = trip.getActionBean( LoginActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 2, errors.size() );

        // Log in again with just a password; should see one error
        trip = m_engine.guestTrip( "/Login.jsp" );
        trip.setParameter( "j_password", Users.JANNE_PASS );
        trip.execute( "login" );
        bean = trip.getActionBean( LoginActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 1, errors.size() );

        // Log in again with just a username; should see one error
        trip = m_engine.guestTrip( "/Login.jsp" );
        trip.setParameter( "j_username", Users.JANNE );
        trip.execute( "login" );
        bean = trip.getActionBean( LoginActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 1, errors.size() );
    }

    public void testLoginRedirect() throws Exception
    {
        MockRoundtrip trip;
        LoginActionBean bean;
        ValidationErrors errors;

        // Verify that the initial user is anonymous
        trip = m_engine.guestTrip( "/Login.jsp" );
        HttpServletRequest request = trip.getRequest();
        WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( request.getSession() );
        assertNotSame( Users.JANNE, wikiSession.getLoginPrincipal().getName() );

        // Log in
        trip.setParameter( "j_username", Users.JANNE );
        trip.setParameter( "j_password", Users.JANNE_PASS );
        trip.setParameter( "redirect", "Foo" );
        trip.execute( "login" );

        // Verify we logged in correctly
        bean = trip.getActionBean( LoginActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( Users.JANNE, wikiSession.getLoginPrincipal().getName() );
        assertEquals( "/Wiki.jsp?page=Foo", trip.getDestination() );
    }

    public void testLoginRememberMe() throws Exception
    {
        MockRoundtrip trip;
        LoginActionBean bean;
        ValidationErrors errors;

        // Verify that the initial user is anonymous
        trip = m_engine.guestTrip( "/Login.jsp" );
        HttpServletRequest request = trip.getRequest();
        WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( request.getSession() );
        assertNotSame( Users.JANNE, wikiSession.getLoginPrincipal().getName() );

        // Log in
        trip.setParameter( "j_username", Users.JANNE );
        trip.setParameter( "j_password", Users.JANNE_PASS );
        trip.setParameter( "remember", "true" );
        trip.execute( "login" );

        // Verify we logged in correctly
        bean = trip.getActionBean( LoginActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( Users.JANNE, wikiSession.getLoginPrincipal().getName() );
        assertEquals( "/Wiki.jsp", trip.getDestination() );

        // Should be two cookies (the JSPWiki asserted name cookie plus the
        // Remember Me? cookie)
        Cookie[] cookies = trip.getResponse().getCookies();
        assertEquals( 2, cookies.length );
    }

    public void testLogout() throws Exception
    {
        MockRoundtrip trip;
        LoginActionBean bean;
        ValidationErrors errors;

        // Start with an authenticated user
        trip = m_engine.authenticatedTrip( Users.JANNE, Users.JANNE_PASS, LoginActionBean.class );
        HttpServletRequest request = trip.getRequest();
        WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( request.getSession() );
        assertEquals( Users.JANNE, wikiSession.getLoginPrincipal().getName() );

        // Log out
        trip.execute( "logout" );

        // Verify we logged out correctly
        bean = trip.getActionBean( LoginActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertNotSame( Users.JANNE, wikiSession.getLoginPrincipal().getName() );
        assertEquals( "/Wiki.jsp", trip.getDestination() );

        // Should be just one cookie (the JSPWiki asserted name cookie), and it
        // should be empty
        Cookie[] cookies = trip.getResponse().getCookies();
        assertEquals( 1, cookies.length );
        assertEquals( CookieAssertionLoginModule.PREFS_COOKIE_NAME, cookies[0].getName() );
        assertEquals( "", cookies[0].getValue() );
    }

}
