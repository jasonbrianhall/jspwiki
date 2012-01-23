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
package org.apache.wiki.ui.migrator;

import java.io.File;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class JspParserTest extends TestCase
{
    public JspParserTest( String s )
    {
        super( s );
    }
    
    public void testDeclaration() throws Exception
    {
        String s = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><html/>";
        
        // Parse the contents
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );
        
        // Verify 2 nodes total
        List<Node> nodes = doc.getNodes();
        assertEquals( 2, nodes.size() );
        
        // First node is XML declaration
        Tag node = (Tag)nodes.get( 0 );
        assertEquals( "xml", node.getName() );
        assertEquals( null, node.getValue() );
        assertEquals( NodeType.DECLARATION, node.getType() );
        assertEquals( 2, node.getAttributes().size() );
    }
    
    public void testParseDoctype() throws Exception
    {
        String s = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
        
        // Parse the contents
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );
        
        // Verify three nodes total
        List<Node> nodes = doc.getNodes();
        assertEquals( 1, nodes.size() );
        Node node = nodes.get( 0 );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( NodeType.DOCTYPE, node.getType() );
        assertEquals( "html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"", node.getValue() );
        assertEquals( s, node.toString() );
    }

    public void testMeta() throws Exception
    {
        String s = "<META name=\"Author\" content=\"Dave Raggett\">";
        
        // Parse the contents
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );
        
        // Verify three nodes total
        List<Node> nodes = doc.getNodes();
        assertEquals( 1, nodes.size() );
        Tag tag = (Tag)nodes.get( 0 );
        assertEquals( "META", tag.getName() );
        assertEquals( NodeType.META, tag.getType() );
        assertEquals( 2, tag.getAttributes().size() );
        assertEquals( "name", tag.getAttribute( "name" ).getName() );
        assertEquals( "Author", tag.getAttribute( "name" ).getValue() );
        assertEquals( "content", tag.getAttribute( "content" ).getName() );
        assertEquals( "Dave Raggett", tag.getAttribute( "content" ).getValue() );
        assertEquals( s, tag.toString() );
    }
    
    public void testLink() throws Exception
    {
        String s = "<LINK rel=\"Start\" title=\"First\" type=\"text/html\" href=\"http://start.html\">";
        
        // Parse the contents
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );
        
        // Verify three nodes total
        List<Node> nodes = doc.getNodes();
        assertEquals( 1, nodes.size() );
        Tag tag = (Tag)nodes.get( 0 );
        assertEquals( "LINK", tag.getName() );
        assertEquals( NodeType.LINK, tag.getType() );
        assertEquals( 4, tag.getAttributes().size() );
        assertEquals( "rel", tag.getAttribute( "rel" ).getName() );
        assertEquals( "Start", tag.getAttribute( "rel" ).getValue() );
        assertEquals( "title", tag.getAttribute( "title" ).getName() );
        assertEquals( "First", tag.getAttribute( "title" ).getValue() );
        assertEquals( "type", tag.getAttribute( "type" ).getName() );
        assertEquals( "text/html", tag.getAttribute( "type" ).getValue() );
        assertEquals( "href", tag.getAttribute( "href" ).getName() );
        assertEquals( "http://start.html", tag.getAttribute( "href" ).getValue() );
    }
    
    public void testNestedAttributes() throws Exception
    {
        String s = "<a <b test=\"c\">selected=\"d\"</b> >Foo</a>";
        
        // Parse the contents
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );
        
        // Verify three nodes total
        List<Node> nodes = doc.getNodes();
        assertEquals( 3, nodes.size() );
        
        // First node is a start tag
        Node node;
        node = nodes.get( 0 );
        assertEquals( "a", node.getName() );
        assertEquals( NodeType.START_TAG, node.getType() );
        assertEquals( "<a <b test=\"c\">selected=\"d\"</b> >", node.toString() );
        
        // Second node is a Text node
        node = nodes.get( 1 );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( "Foo", node.getValue() );
        
        // Third node is an end tag
        node = nodes.get( 2 );
        assertEquals( "a", node.getName() );
        assertEquals( NodeType.END_TAG, node.getType() );
        
        // First and third node are children of the root
        assertEquals( 2, doc.getRoot().getChildren().size() );
        assertEquals( NodeType.START_TAG, doc.getRoot().getChildren().get( 0 ).getType() );
        assertEquals( NodeType.END_TAG, doc.getRoot().getChildren().get( 1 ).getType() );
        
        // Test first node: should have 3 attributes (2 dynamic)
        Tag tag = (Tag)nodes.get( 0 );
        assertEquals( 3, tag.getAttributes().size() );
        Attribute attribute;
        attribute = tag.getAttributes().get( 0 );
        assertEquals( null, attribute.getName() );
        assertEquals( NodeType.DYNAMIC_ATTRIBUTE, attribute.getType() );
        assertEquals( "<b test=\"c\">", attribute.getValue() );
        assertEquals( "<b test=\"c\">", attribute.toString() );
        attribute = tag.getAttributes().get( 1 );
        assertEquals( "selected", attribute.getName() );
        assertEquals( "d", attribute.getValue() );
        assertEquals( "selected=\"d\"", attribute.toString() );
        attribute = tag.getAttributes().get( 2 );
        assertEquals( null, attribute.getName() );
        assertEquals( NodeType.DYNAMIC_ATTRIBUTE, attribute.getType() );
        assertEquals( "</b>", attribute.getValue() );
        assertEquals( "</b>", attribute.toString() );
    }

    public void testAttributes() throws Exception
    {
        String s = "<a b=\"cd\"/>";

        // Parse the contents
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );

        // Results in one node
        List<Node> nodes = doc.getNodes();
        assertEquals( 1, nodes.size() );
        Tag node;

        // Verify HTML start tag
        node = (Tag) nodes.get( 0 );
        assertEquals( "a", node.getName() );
        assertEquals( null, node.getValue() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.EMPTY_ELEMENT_TAG, node.getType() );
        assertEquals( "<a b=\"cd\" />", node.toString() );

        // Verify attributes
        assertEquals( 1, node.getAttributes().size() );
        Attribute attribute = node.getAttribute( "b" );
        assertEquals( "b", attribute.getName() );
        assertEquals( "cd", attribute.getValue() );
        assertEquals( 3, attribute.getStart() );
        assertEquals( 9, attribute.getEnd() );
    }

    public void testCdata() throws Exception
    {
        String s = "  <![CDATA[ foo ]]>  ";

        // Parse the contents
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );

        // Results in 3 nodes
        List<Node> nodes = doc.getNodes();
        assertEquals( 3, nodes.size() );
        Node node;

        // Verify text tag (0)
        node = nodes.get( 0 );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( "  ", node.getValue() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( "  ", node.toString() );

        // Verify CDATA tag (1)
        node = nodes.get( 1 );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( " foo ", node.getValue() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.CDATA, node.getType() );
        assertEquals( "<![CDATA[ foo ]]>", node.toString() );
        
        // Verify text tag (2)
        node = nodes.get( 2 );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( "  ", node.getValue() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( "  ", node.toString() );
    }
    
    public void testEmptyElementTag() throws Exception
    {
        String s = "<foo />";

        // Parse the contents
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );

        // Results in one node
        List<Node> nodes = doc.getNodes();
        assertEquals( 1, nodes.size() );
        Tag node;

        // Verify HTML combined tag
        node = (Tag) nodes.get( 0 );
        assertEquals( "foo", node.getName() );
        assertEquals( null, node.getValue() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.EMPTY_ELEMENT_TAG, node.getType() );
        assertEquals( "<foo/>", node.toString() );
    }

    public void testParseHtmlComment() throws Exception
    {
        String s = "<!-- This is a comment -->";

        // Parse the contents
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );

        // Results in one node
        List<Node> nodes = doc.getNodes();
        assertEquals( 1, nodes.size() );
        Text node;

        // Verify comment
        node = (Text) nodes.get( 0 );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( " This is a comment ", node.getValue() );
        assertEquals( 0, node.getChildren().size() );
    }

    public void testParseDirective() throws Exception
    {
        String s = "<%@ page import=\"org.apache.log4j.*\" %>";

        // Parse the contents of the file
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );

        // Results in one node
        List<Node> nodes = doc.getNodes();
        assertEquals( 1, nodes.size() );
        Tag node;
        Node attribute;

        // Verify directive
        node = (Tag) nodes.get( 0 );
        assertEquals( "page", node.getName() );
        assertEquals( 1, node.getAttributes().size() );
        attribute = node.getAttributes().get( 0 );
        assertEquals( "import", attribute.getName() );
        assertEquals( "org.apache.log4j.*", attribute.getValue() );
        assertEquals( "<%@ page import=\"org.apache.log4j.*\" %>", node.toString());
    }
    
    public void testParseDirectiveNoLeadingSpace() throws Exception
    {
        String s = "<%@page import=\"org.apache.log4j.*\"%>";

        // Parse the contents of the file
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );

        // Results in one node
        List<Node> nodes = doc.getNodes();
        assertEquals( 1, nodes.size() );
        Tag node;
        Node attribute;

        // Verify directive
        node = (Tag) nodes.get( 0 );
        assertEquals( "page", node.getName() );
        assertEquals( 1, node.getAttributes().size() );
        attribute = node.getAttributes().get( 0 );
        assertEquals( "import", attribute.getName() );
        assertEquals( "org.apache.log4j.*", attribute.getValue() );
        assertEquals( "<%@ page import=\"org.apache.log4j.*\" %>", node.toString());
    }

    public void testParse() throws Exception
    {
        File src = new File( "tests/etc/TestMigratorForm.jsp" );
        String s = JspMigrator.readSource( src );

        // Parse the contents of the file
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );

        // Should result in 16 nodes parsed (8 tags/directives + 8
        // text/whitespace nodes
        Node node;
        Node attribute;
        List<Node> nodes = doc.getNodes();
        assertEquals( 16, nodes.size() );
        int i = 0;

        // Test line 1 aka nodes 0+1
        node = nodes.get( i );
        assertEquals( 1, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 0, node.getStart() );
        assertEquals( 867, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.JSP_COMMENT, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 8, node.getSiblings().size() );
        i++;
        node = nodes.get( i );
        assertEquals( 20, node.getLine() );
        assertEquals( 5, node.getColumn() );
        assertEquals( 867, node.getStart() );
        assertEquals( 868, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 8, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;
        
        // Test line 21 aka nodes 2+3
        node = nodes.get( i );
        assertEquals( 21, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 868, node.getStart() );
        assertEquals( 947, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.JSP_DIRECTIVE, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 8, node.getSiblings().size() );
        assertEquals( "taglib", node.getName() );
        assertEquals( 2, ((Tag) node).getAttributes().size() );
        attribute = ((Tag) node).getAttributes().get( 0 );
        assertEquals( "uri", attribute.getName() );
        assertEquals( "http://stripes.sourceforge.net/stripes.tld", attribute.getValue() );
        attribute = ((Tag) node).getAttributes().get( 1 );
        assertEquals( "prefix", attribute.getName() );
        assertEquals( "stripes", attribute.getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 21, node.getLine() );
        assertEquals( 80, node.getColumn() );
        assertEquals( 947, node.getStart() );
        assertEquals( 948, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 8, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;

        // Test line 22 aka nodes 4+5
        node = nodes.get( i );
        assertEquals( 22, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 948, node.getStart() );
        assertEquals( 1057, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.EMPTY_ELEMENT_TAG, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 8, node.getSiblings().size() );
        assertEquals( "stripes:useActionBean", node.getName() );
        assertEquals( 3, ((Tag) node).getAttributes().size() );
        attribute = ((Tag) node).getAttributes().get( 0 );
        assertEquals( "beanclass", attribute.getName() );
        assertEquals( "org.apache.wiki.action.LoginActionBean", attribute.getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 22, node.getLine() );
        assertEquals( 110, node.getColumn() );
        assertEquals( 1057, node.getStart() );
        assertEquals( 1058, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 8, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;

        // Test line 23 aka nodes 6+7
        node = nodes.get( i );
        assertEquals( 23, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 1058, node.getStart() );
        assertEquals( 1125, node.getEnd() );
        assertEquals( 4, node.getChildren().size() );
        assertEquals( NodeType.START_TAG, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 8, node.getSiblings().size() );
        assertEquals( "stripes:layout-render", node.getName() );
        assertEquals( 1, ((Tag) node).getAttributes().size() );
        attribute = ((Tag) node).getAttributes().get( 0 );
        assertEquals( "name", attribute.getName() );
        assertEquals( "/templates/default/DefaultLayout.jsp", attribute.getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 23, node.getLine() );
        assertEquals( 68, node.getColumn() );
        assertEquals( 1125, node.getStart() );
        assertEquals( 1128, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.START_TAG, node.getParent().getType() );
        assertEquals( 3, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;

        // Test line 24 aka nodes 8+9
        node = nodes.get( i );
        assertEquals( 24, node.getLine() );
        assertEquals( 3, node.getColumn() );
        assertEquals( 1128, node.getStart() );
        assertEquals( 1169, node.getEnd() );
        assertEquals( 3, node.getChildren().size() );
        assertEquals( NodeType.START_TAG, node.getType() );
        assertEquals( NodeType.START_TAG, node.getParent().getType() );
        assertEquals( 3, node.getSiblings().size() );
        assertEquals( "stripes:layout-component", node.getName() );
        assertEquals( 1, ((Tag) node).getAttributes().size() );
        attribute = ((Tag) node).getAttributes().get( 0 );
        assertEquals( "name", attribute.getName() );
        assertEquals( "content", attribute.getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 24, node.getLine() );
        assertEquals( 44, node.getColumn() );
        assertEquals( 1169, node.getStart() );
        assertEquals( 1174, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.START_TAG, node.getParent().getType() );
        assertEquals( 2, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;

        // Test line 25 aka nodes 10+11
        node = nodes.get( i );
        assertEquals( 25, node.getLine() );
        assertEquals( 5, node.getColumn() );
        assertEquals( 1174, node.getStart() );
        assertEquals( 1232, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.EMPTY_ELEMENT_TAG, node.getType() );
        assertEquals( NodeType.START_TAG, node.getParent().getType() );
        assertEquals( 2, node.getSiblings().size() );
        assertEquals( "jsp:include", node.getName() );
        assertEquals( 1, ((Tag) node).getAttributes().size() );
        attribute = ((Tag) node).getAttributes().get( 0 );
        assertEquals( "page", attribute.getName() );
        assertEquals( "/templates/default/LoginContent.jsp", attribute.getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 25, node.getLine() );
        assertEquals( 63, node.getColumn() );
        assertEquals( 1232, node.getStart() );
        assertEquals( 1235, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.START_TAG, node.getParent().getType() );
        assertEquals( 2, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;
        
        // Test line 26 aka nodes 12+13
        node = nodes.get( i );
        assertEquals( 26, node.getLine() );
        assertEquals( 3, node.getColumn() );
        assertEquals( 1235, node.getStart() );
        assertEquals( 1262, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.END_TAG, node.getType() );
        assertEquals( NodeType.START_TAG, node.getParent().getType() );
        assertEquals( 3, node.getSiblings().size() );
        assertEquals( "stripes:layout-component", node.getName() );
        assertEquals( 0, ((Tag) node).getAttributes().size() );
        i++;
        node = nodes.get( i );
        assertEquals( 26, node.getLine() );
        assertEquals( 30, node.getColumn() );
        assertEquals( 1262, node.getStart() );
        assertEquals( 1263, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.START_TAG, node.getParent().getType() );
        assertEquals( 3, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;

        // Test line 27 aka nodes 14+15
        node = nodes.get( i );
        assertEquals( 27, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 1263, node.getStart() );
        assertEquals( 1287, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.END_TAG, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 8, node.getSiblings().size() );
        assertEquals( "stripes:layout-render", node.getName() );
        assertEquals( 0, ((Tag) node).getAttributes().size() );
        i++;
        node = nodes.get( i );
        assertEquals( 27, node.getLine() );
        assertEquals( 25, node.getColumn() );
        assertEquals( 1287, node.getStart() );
        assertEquals( 1288, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 8, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;
    }

    public void testParseNestedTags()
    {
        String s = "  <foo attribute1=\"1\">  <bar attribute2=\"2\" attribute3=\"3\"/>  </foo>  ";
        JspDocument doc = new JspParser().parse( s );

        // Total number of nodes (depth-first search) is 7
        List<Node> nodes = doc.getNodes();
        Node node;
        Node attribute;
        assertEquals( 7, nodes.size() );

        // First, check the root node. Should have 4 children (2 text nodes + 2
        // html nodes)
        node = doc.getRoot();
        assertEquals( 4, node.getChildren().size() );

        // AbstractNode 0 is whitespace
        node = nodes.get( 0 );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( "  ", node.getValue() );
        assertEquals( doc.getRoot(), node.getParent() );
        assertEquals( 0, node.getStart() );
        assertEquals( 2, node.getEnd() );

        // AbstractNode 1 is <foo> with 1 attribute
        node = nodes.get( 1 );
        assertEquals( NodeType.START_TAG, node.getType() );
        assertEquals( "<foo attribute1=\"1\">", node.toString() );
        assertEquals( "foo", node.getName() );
        assertEquals( "  <bar attribute2=\"2\" attribute3=\"3\" />  ", node.getValue() );
        assertEquals( 2, node.getStart() );
        assertEquals( 22, node.getEnd() );

        // AbstractNode 1: attributes test
        assertEquals( 1, ((Tag) node).getAttributes().size() );
        attribute = ((Tag) node).getAttributes().get( 0 );
        assertEquals( "attribute1", attribute.getName() );
        assertEquals( "1", attribute.getValue() );
        assertEquals( 7, attribute.getStart() );
        assertEquals( 21, attribute.getEnd() );

        // AbstractNode 1 also has 3 child elements: <bar> plus two whitespace
        // nodes
        assertEquals( 3, node.getChildren().size() );

        // Check AbstractNode 1, child 0 -- should be whitespace
        node = nodes.get( 1 ).getChildren().get( 0 );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( "  ", node.getValue() );
        assertEquals( nodes.get( 1 ), node.getParent() );
        assertEquals( 22, node.getStart() );
        assertEquals( 24, node.getEnd() );

        // Check AbstractNode 1, child 1 -- should be <bar>
        node = nodes.get( 1 ).getChildren().get( 1 );
        assertEquals( NodeType.EMPTY_ELEMENT_TAG, node.getType() );
        assertEquals( "<bar attribute2=\"2\" attribute3=\"3\" />", node.toString() );
        assertEquals( "bar", node.getName() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( 24, node.getStart() );
        assertEquals( 60, node.getEnd() );
        assertEquals( 2, ((Tag) node).getAttributes().size() );
        assertEquals( "attribute2", ((Tag) node).getAttributes().get( 0 ).getName() );
        assertEquals( "2", ((Tag) node).getAttributes().get( 0 ).getValue() );
        assertEquals( "attribute3", ((Tag) node).getAttributes().get( 1 ).getName() );
        assertEquals( "3", ((Tag) node).getAttributes().get( 1 ).getValue() );

        // Check AbstractNode 1, child 2 -- should be whitespace
        node = nodes.get( 1 ).getChildren().get( 2 );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( "  ", node.getValue() );
        assertEquals( nodes.get( 1 ), node.getParent() );
        assertEquals( 60, node.getStart() );
        assertEquals( 62, node.getEnd() );

        // AbstractNode 5 (</foo) has no attributes
        node = nodes.get( 5 );
        assertEquals( NodeType.END_TAG, node.getType() );
        assertEquals( null, node.getValue() );
        assertEquals( "foo", node.getName() );
        assertEquals( 0, ((Tag) node).getAttributes().size() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( 62, node.getStart() );
        assertEquals( 68, node.getEnd() );

        // AbstractNode 6 is whitespace
        node = nodes.get( 6 );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( "  ", node.getValue() );
        assertEquals( doc.getRoot(), node.getParent() );
        assertEquals( 68, node.getStart() );
        assertEquals( 70, node.getEnd() );

        // The children of AbstractNode 1 == Nodes 2, 3 and 4 from
        // doc.getNodes()
        node = nodes.get( 1 );
        assertEquals( nodes.get( 2 ), node.getChildren().get( 0 ) );
        assertEquals( nodes.get( 3 ), node.getChildren().get( 1 ) );
        assertEquals( nodes.get( 4 ), node.getChildren().get( 2 ) );

    }

    public void testParseNestedExpression()
    {
        String s = "  <wiki:Include page=\"<%=contentPage%>\" var=\'Foo\' />  ";
        JspDocument doc = new JspParser().parse( s );

        List<Node> nodes = doc.getNodes();
        Node node;
        assertEquals( 3, nodes.size() );

        // AbstractNode 1: text node
        node = nodes.get( 0 );
        assertEquals( 1, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 0, node.getStart() );
        assertEquals( 2, node.getEnd() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( "  ", node.getValue() );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 2, node.getSiblings().size() );
        assertEquals( 0, node.getChildren().size() );

        // AbstractNode 2: HTML tag with attribute containing JSP expression
        node = nodes.get( 1 );
        assertEquals( 1, node.getLine() );
        assertEquals( 3, node.getColumn() );
        assertEquals( 2, node.getStart() );
        assertEquals( 52, node.getEnd() );
        assertEquals( NodeType.EMPTY_ELEMENT_TAG, node.getType() );
        assertEquals( "<wiki:Include page=\"<%=contentPage%>\" var=\'Foo\' />", node.toString() );
        assertEquals( "wiki:Include", node.getName() );
        assertEquals( null, node.getValue() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 2, node.getSiblings().size() );
        assertEquals( 0, node.getChildren().size() );

        // AbstractNode 2: test attributes
        assertEquals( 2, ((Tag) node).getAttributes().size() );
        Node attribute;
        attribute = ((Tag) node).getAttributes().get( 0 );
        assertEquals( NodeType.ATTRIBUTE, attribute.getType() );
        assertEquals( "page", attribute.getName() );
        assertEquals( "<%=contentPage%>", attribute.getValue() );
        assertEquals( 1, attribute.getChildren().size() );
        assertEquals( NodeType.JSP_EXPRESSION, attribute.getChildren().get( 0 ).getType() );
        assertEquals( "contentPage", attribute.getChildren().get( 0 ).getValue() );

        attribute = ((Tag) node).getAttributes().get( 1 );
        assertEquals( NodeType.ATTRIBUTE, attribute.getType() );
        assertEquals( "var", attribute.getName() );
        assertEquals( "Foo", attribute.getValue() );
        assertEquals( 1, attribute.getChildren().size() );
        assertEquals( NodeType.TEXT, attribute.getChildren().get( 0 ).getType() );
        assertEquals( "Foo", attribute.getChildren().get( 0 ).getValue() );

        // AbstractNode 3: text
        node = nodes.get( 2 );
        assertEquals( 1, node.getLine() );
        assertEquals( 53, node.getColumn() );
        assertEquals( 52, node.getStart() );
        assertEquals( 54, node.getEnd() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( "  ", node.getValue() );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 2, node.getSiblings().size() );
        assertEquals( 0, node.getChildren().size() );
    }

    public static Test suite()
    {
        return new TestSuite( JspParserTest.class );
    }
}
