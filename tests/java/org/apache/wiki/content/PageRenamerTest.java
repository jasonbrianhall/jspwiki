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
package org.apache.wiki.content;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.providers.ProviderException;


public class PageRenamerTest extends TestCase
{
    TestEngine m_engine;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        
        Properties props = new Properties();
        
        props.load( TestEngine.findTestProperties() );

        props.setProperty( WikiEngine.PROP_MATCHPLURALS, "true" );
        
        TestEngine.emptyWorkDir();
        m_engine = new TestEngine(props);
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();

        try
        {
            m_engine.emptyRepository();
        }
        finally
        {
            m_engine.shutdown();
        }
    }

    private List<WikiPath> findReferrers(String path) throws ProviderException
    {
        return m_engine.getReferenceManager().getReferredBy( WikiPath.valueOf(path) );
    }
    
    public void testSimpleRename()
        throws Exception
    {
        // Count the number of existing references
        int pageCount = m_engine.getPageCount();
        
        m_engine.saveText("SimpleRename", "the big lazy dog thing" );
        
        WikiPage p = m_engine.getPage("SimpleRename");
        
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( p );
        
        m_engine.renamePage(context, "SimpleRename", "FooRename", false);
        
        WikiPage newpage = m_engine.getPage("FooRename");
        
        assertNotNull( "no new page", newpage );
        try
        {
            m_engine.getPage("SimpleRename");
        
            fail( "old page not gone" );
        }
        catch( PageNotFoundException e ) {} // Expected 
        
        // Refmgr
        
        Set<String> pages = m_engine.getReferenceManager().findCreated();
        
        assertTrue( "FooRename does not exist", pages.contains( "Main:FooRename" ) );
        assertFalse( "SimpleRename exists", pages.contains( "Main:SimpleRename" ) );
        assertEquals( "wrong list size", pageCount+1, pages.size() );
        m_engine.deletePage( "Main:FooRename" );
    }
    
    public void testReferrerChange()
       throws Exception
    {
        m_engine.saveText("ReferrerChange", "foofoo" );
        m_engine.saveText("ReferrerChange2", "[ReferrerChange]");
        
        WikiPage p = m_engine.getPage("ReferrerChange");
        
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( p );
        
        m_engine.renamePage(context, "ReferrerChange", "FooReferrerChange", true);
        
        // Verify that reference to ReferrerChange was renamed
        String data = m_engine.getPage( "ReferrerChange2" ).getContentAsString();
        assertEquals( "no rename", "[FooReferrerChange]", data.trim() );
        
        Collection<WikiPath> refs = findReferrers("ReferrerChange");
        
        assertEquals( 0, refs.size() );
        
        refs = findReferrers( "FooReferrerChange" );
        assertEquals( "new size", 1, refs.size() );
        assertEquals( "wrong ref", WikiPath.valueOf( "Main:ReferrerChange2" ), refs.iterator().next() );
    }

    public void testReferrerChangeCC()
        throws Exception
    {
        m_engine.saveText("TestPage", "foofoo" );
        m_engine.saveText("TestPage2", "TestPage");
     
        WikiPage p = m_engine.getPage("TestPage");
     
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( p );
     
        m_engine.renamePage(context, "TestPage", "FooTest", true);
     
        String data = m_engine.getPage("TestPage2").getContentAsString();
     
        assertEquals( "no rename", "FooTest", data.trim() );
        Collection<WikiPath> refs = findReferrers("TestPage");
        
        assertEquals( 0, refs.size() );
        
        refs = findReferrers( "FooTest" );
        assertEquals( "new size", 1, refs.size() );
        assertEquals( "wrong ref", WikiPath.valueOf( "Main:TestPage2" ), refs.iterator().next() );
    }
    
    public void testReferrerChangeAnchor()
        throws Exception
    {
        m_engine.saveText("TestPage", "foofoo" );
        m_engine.saveText("TestPage2", "[TestPage#heading1]");
     
        WikiPage p = m_engine.getPage("TestPage");
     
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( p );
     
        m_engine.renamePage(context, "TestPage", "FooTest", true);
     
        String data = m_engine.getPage( "TestPage2" ).getContentAsString();
     
        assertEquals( "no rename", "[FooTest#heading1]", data.trim() );
        Collection<WikiPath> refs = findReferrers("TestPage");
        
        assertEquals( 0, refs.size() );
        
        refs = findReferrers( "FooTest" );
        assertEquals( "new size", 1, refs.size() );
        assertEquals( "wrong ref", WikiPath.valueOf( "Main:TestPage2" ), refs.iterator().next() );
    }
    
    public void testReferrerChangeMultilink()
        throws Exception
    {
        m_engine.saveText("TestPage", "foofoo" );
        m_engine.saveText("TestPage2", "[TestPage] [TestPage] [linktext|TestPage] TestPage [linktext|TestPage] [TestPage#Anchor] [TestPage] TestPage [TestPage]");
     
        WikiPage p = m_engine.getPage("TestPage");
     
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( p );
     
        m_engine.renamePage(context, "TestPage", "FooTest", true);
     
        String data = m_engine.getPage( "TestPage2" ).getContentAsString();
     
        assertEquals( "no rename", 
                      "[FooTest] [FooTest] [linktext|FooTest] FooTest [linktext|FooTest] [FooTest#Anchor] [FooTest] FooTest [FooTest]", 
                      data.trim() );

        Collection<WikiPath> refs = findReferrers("TestPage");
        
        assertEquals( 0, refs.size() );
        
        refs = findReferrers( "FooTest" );
        assertEquals( "new size", 1, refs.size() );
        assertEquals( "wrong ref", WikiPath.valueOf( "Main:TestPage2" ), refs.iterator().next() );
    }
    
    public void testReferrerNoWikiName()
        throws Exception
    {
        m_engine.saveText("Test","foo");
        m_engine.saveText("TestPage2", "[Test] [Test#anchor] test Test [test] [link|test] [link|test]");
        
        WikiPage p = m_engine.getPage("Test");
        
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( p );
     
        m_engine.renamePage(context, "Test", "TestPage", true);
        
        String data = m_engine.getPage( "TestPage2" ).getContentAsString();
        
        assertEquals( "wrong data", "[TestPage] [TestPage#anchor] test Test [TestPage] [link|TestPage] [link|TestPage]", data.trim() );
    }

    public void testAttachmentChange()
        throws Exception
    {
        m_engine.saveText("TestPage", "foofoo" );
        m_engine.saveText("TestPage2", "[TestPage/foo.txt] [linktext|TestPage/bar.jpg]");
 
        m_engine.addAttachment("TestPage", "foo.txt", "testing".getBytes() );
        m_engine.addAttachment("TestPage", "bar.jpg", "pr0n".getBytes() );
        WikiPage p = m_engine.getPage("TestPage");
 
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( p );
 
        m_engine.renamePage(context, "TestPage", "RenamedTest", true);
 
        String data = m_engine.getPage( "TestPage2" ).getContentAsString();
 
        assertEquals( "no rename", 
                      "[RenamedTest/foo.txt] [linktext|RenamedTest/bar.jpg]", 
                      data.trim() );

        WikiPage att = m_engine.getAttachmentManager().getAttachmentInfo("RenamedTest/foo.txt");
        assertNotNull("footext",att);
        
        att = m_engine.getAttachmentManager().getAttachmentInfo("RenamedTest/bar.jpg");
        assertNotNull("barjpg",att);
        
        try
        {
            att = m_engine.getAttachmentManager().getAttachmentInfo("TestPage/bar.jpg");
            fail("testpage/bar.jpg exists");
        }
        catch( PageNotFoundException e ) {}
        
        try
        {
            att = m_engine.getAttachmentManager().getAttachmentInfo("TestPage/foo.txt");
            fail("testpage/foo.txt exists");
        }
        catch( PageNotFoundException e ) {}
        
        Collection<WikiPath> refs = findReferrers("TestPage/bar.jpg");
    
        assertTrue( "oldpage", refs.isEmpty() );
    
        refs = findReferrers( "RenamedTest/bar.jpg" );
        assertEquals( "new size", 1, refs.size() );
        assertEquals( "wrong ref", WikiPath.valueOf("Main:TestPage2"), refs.iterator().next() );
    }

    public void testSamePage() throws Exception
    {
        m_engine.saveText( "TestPage", "[TestPage]");
        
        rename( "TestPage", "FooTest" );

        WikiPage p = m_engine.getPage( "FooTest" );
        
        assertNotNull( "no page", p );
        
        assertEquals("[FooTest]", m_engine.getText("FooTest").trim() );
    }

    public void testBrokenLink1() throws Exception
    {
        m_engine.saveText( "TestPage", "hubbub");
        m_engine.saveText( "TestPage2", "[TestPage|]" );
        
        rename( "TestPage", "FooTest" );

        WikiPage p = m_engine.getPage( "FooTest" );
        
        assertNotNull( "no page", p );
        
        // Should be no change
        assertEquals("[TestPage|]", m_engine.getText("TestPage2").trim() );
    }

    public void testBrokenLink2() throws Exception
    {
        m_engine.saveText( "TestPage", "hubbub");
        m_engine.saveText( "TestPage2", "[|TestPage]" );
        
        WikiPage p;
        rename( "TestPage", "FooTest" );

        p = m_engine.getPage( "FooTest" );
        
        assertNotNull( "no page", p );
        
        assertEquals("[|FooTest]", m_engine.getText("TestPage2").trim() );
    }

    private void rename( String src, String dst ) throws WikiException, PageNotFoundException
    {
        WikiPage p = m_engine.getPage(src);

        WikiContext context = m_engine.getWikiContextFactory().newViewContext( p );
        
        m_engine.renamePage(context, src, dst, true);
    }

    public void testBug25() throws Exception
    {
        String src = "[Cdauth/attach.txt] [link|Cdauth/attach.txt] [cdauth|Cdauth/attach.txt]"+
                     "[CDauth/attach.txt] [link|CDauth/attach.txt] [cdauth|CDauth/attach.txt]"+
                     "[cdauth/attach.txt] [link|cdauth/attach.txt] [cdauth|cdauth/attach.txt]";
        
        String dst = "[CdauthNew/attach.txt] [link|CdauthNew/attach.txt] [cdauth|CdauthNew/attach.txt]"+
                     "[CdauthNew/attach.txt] [link|CdauthNew/attach.txt] [cdauth|CdauthNew/attach.txt]"+
                     "[CdauthNew/attach.txt] [link|CdauthNew/attach.txt] [cdauth|CdauthNew/attach.txt]";
        
        m_engine.saveText( "Cdauth", "xxx" );
        m_engine.saveText( "TestPage", src );
        
        m_engine.addAttachment( "Cdauth", "attach.txt", "Puppua".getBytes() );
        
        rename( "Cdauth", "CdauthNew" );
        
        assertEquals( dst, m_engine.getText("TestPage").trim() );
    }
    
    public void testBug21() throws Exception
    {
        String src = "[Link to TestPage2|TestPage2]";
        
        m_engine.saveText( "TestPage", src );
        m_engine.saveText( "TestPage2", "foo" );
        
        rename ("TestPage2", "Test");
        
        assertEquals( "[Link to Test|Test]", m_engine.getText( "TestPage" ).trim() );
    }

    public void testExtendedLinks() throws Exception
    {
        String src = "[Link to TestPage2|TestPage2|target='_new']";
        
        m_engine.saveText( "TestPage", src );
        m_engine.saveText( "TestPage2", "foo" );
        
        rename ("TestPage2", "Test");
        
        assertEquals( "[Link to Test|Test|target='_new']", m_engine.getText( "TestPage" ).trim() );
    }
    
    public void testBug85_case1() throws Exception 
    {
        // renaming a non-existing page
        // This fails under 2.5.116, cfr. with http://bugs.jspwiki.org/show_bug.cgi?id=85
        // m_engine.saveText( "TestPage", "blablahblahbla" );
        try
        {
            rename("TestPage123", "Main8887");
            rename("Main8887", "TestPage123"); 
        }
        catch (NullPointerException npe)
        {
            npe.printStackTrace();
            System.out.println("NPE: Bug 85 caught?");
            fail();
        }
        catch( PageNotFoundException e )
        {
            // Expected
        }
    }
   
    public void testBug85_case2() throws Exception 
    {
        try
        {
            // renaming a non-existing page, but we call m_engine.saveText() before renaming 
            // this does not fail under 2.5.116
            m_engine.saveText( "TestPage1234", "blablahblahbla" );
            rename("TestPage1234", "Main8887");
            rename("Main8887", "TestPage1234");
        }
        catch (NullPointerException npe)
        {
            npe.printStackTrace();
            System.out.println("NPE: Bug 85 caught?");
            fail();
        }
    }
    
    public void testBug85_case3() throws Exception 
    {
        try
        {
            // renaming an existing page
            // this does not fail under 2.5.116
            // m_engine.saveText( "Main", "blablahblahbla" );
            rename("Main", "Main8887");
            rename("Main8887", "Main");
        }
        catch (NullPointerException npe)
        {
            npe.printStackTrace();
            System.out.println("NPE: Bug 85 caught?");
            fail();
        }
        catch( PageNotFoundException e )
        {
            // Expected
        }
    }
    
    public void testBug85_case4() throws Exception 
    {
        try
        {
            // renaming an existing page, and we call m_engine.saveText() before renaming
            // this does not fail under 2.5.116
            m_engine.saveText( "Main", "blablahblahbla" );
            rename("Main", "Main8887");
            rename("Main8887", "Main");
        }
        catch (NullPointerException npe)
        {
            npe.printStackTrace();
            System.out.println("NPE: Bug 85 caught?");
            fail();
        }
    }
    
    public void testRenameOfEscapedLinks() throws Exception
    {
        String src = "[[Link to TestPage2|TestPage2|target='_new']";
        
        m_engine.saveText( "TestPage", src );
        m_engine.saveText( "TestPage2", "foo" );
        
        rename ("TestPage2", "Test");
        
        assertEquals( "[[Link to TestPage2|TestPage2|target='_new']", m_engine.getText( "TestPage" ).trim() );
    }

    public void testRenameOfEscapedLinks2() throws Exception
    {
        String src = "~[Link to TestPage2|TestPage2|target='_new']";
        
        m_engine.saveText( "TestPage", src );
        m_engine.saveText( "TestPage2", "foo" );
        
        rename ("TestPage2", "Test");
        
        assertEquals( "~[Link to TestPage2|TestPage2|target='_new']", m_engine.getText( "TestPage" ).trim() );
    }

    /**
     * Test for a referrer containing blanks
     * 
     * @throws Exception
     */
    public void testReferrerChangeWithBlanks() throws Exception
    {
        m_engine.saveText( "TestPageReferred", "bla bla bla som content" );
        m_engine.saveText( "TestPageReferring", "[Test Page Referred]" );

        rename( "TestPageReferred", "TestPageReferredNew" );

        String data = m_engine.getPureText( "TestPageReferring", WikiProvider.LATEST_VERSION );
        assertEquals( "page not renamed", "[Test Page Referred|TestPageReferredNew]", data.trim() );

        Collection<WikiPath> refs = findReferrers( "TestPageReferred" );
        assertEquals( 0, refs.size() );

        refs = findReferrers( "TestPageReferredNew" );
        assertEquals( "new size", 1, refs.size() );
        assertEquals( "wrong ref", WikiPath.valueOf( "TestPageReferring" ), refs.iterator().next() );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-398 */
    public void testReferrerChangeWithBlanks2() throws Exception
    {
        m_engine.saveText( "RenameTest", "[link one] [link two]" );
        m_engine.saveText( "Link one", "Leonard" );
        m_engine.saveText( "Link two", "Cohen" );

        rename( "Link one", "Link uno" );
       
        String data = m_engine.getPureText( "RenameTest", WikiProvider.LATEST_VERSION );
        assertEquals( "page not renamed", "[link one|Link uno] [link two]", data.trim() );

        Collection<WikiPath> refs = findReferrers( "Link one" );
        assertEquals( 0, refs.size() );

        refs = findReferrers( "Link uno" );
        assertEquals( "new size", 1, refs.size() );
        assertEquals( "wrong ref", WikiPath.valueOf( "RenameTest" ), refs.iterator().next() );
    }

    public static Test suite()
    {
        return new TestSuite( PageRenamerTest.class );
    }

}
