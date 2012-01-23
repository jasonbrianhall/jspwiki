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
package org.apache.wiki.ui.stripes;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.controller.DispatcherServlet;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.mock.MockServletContext;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.action.InstallActionBean;
import org.apache.wiki.action.ViewActionBean;


public class FileBasedActionResolverTest extends TestCase
{
    private MockServletContext m_servletContext = null;
    private WikiEngine m_engine = null;

    public void setUp()
    {
        // Configure the filter and servlet
        MockServletContext servletContext = new MockServletContext( "test" );
        servletContext.setServlet(DispatcherServlet.class, "StripesDispatcher", null);
        
        // Add extension classes
        Map<String,String> filterParams = new HashMap<String,String>();
        filterParams.put("ActionResolver.Class", "org.apache.wiki.ui.stripes.FileBasedActionResolver");
        filterParams.put("ActionResolver.Packages", "org.apache.wiki.action");
        filterParams.put("Extension.Packages", "org.apache.wiki.ui.stripes");
        filterParams.put( "ExceptionHandler.Class", "org.apache.wiki.ui.stripes.WikiExceptionHandler" );
        servletContext.addFilter(StripesFilter.class, "StripesFilter", filterParams);
        
        // Set the configured servlet context
        m_servletContext = servletContext;

        // Set the admin password and base URL so that the Installer interceptor doesnt't trip
        m_engine = WikiEngine.getInstance( servletContext, null );
        Properties props = m_engine.getWikiProperties();
        props.setProperty( InstallActionBean.PROP_ADMIN_PASSWORD_HASH, "foobar" );
        props.setProperty( WikiEngine.PROP_BASEURL, "http://127.0.0.1" );
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        m_engine.shutdown();
    }
    
    public void testViewActionBean() throws Exception
    {
        // Test ViewActionBean short binding
        MockRoundtrip trip = new MockRoundtrip( m_servletContext, "/pages/Foo" );
        trip.execute();
        ViewActionBean bean = trip.getActionBean( ViewActionBean.class );
        assertNotNull( bean );
        assertNotNull( bean.getPage() );
        assertEquals( "Foo", bean.getPage().getName() );
    }
    
    public void testViewActionBeanNoPage() throws Exception
    {
        // Test ViewActionBean short binding
        MockRoundtrip trip = new MockRoundtrip( m_servletContext, "/pages" );
        trip.execute();
        ViewActionBean bean = trip.getActionBean( ViewActionBean.class );
        assertNotNull( bean );
        assertNotNull( bean.getPage() );
        assertEquals( "Main", bean.getPage().getName() );
    }

    public static Test suite()
    {
        return new TestSuite( FileBasedActionResolverTest.class );
    }
}
