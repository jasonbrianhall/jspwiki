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

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.content.PageRenamer;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.tags.BreadcrumbsTag;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiRequestContext;


/**
 * <p>
 * Renames a {@link org.apache.wiki.api.WikiPage}. This ActionBean parses and extracts two request
 * parameters:
 * </p>
 * <h3>Request parameters</h3>
 * <ul>
 * <li><code>page</code> - the existing page name. Returned by
 * {@link #getPage()}
 * <li><code>renameTo</code> - the proposed new name for the page. This
 * parameter is required, and it is a validation error if not</li>
 * <li><code>changeReferences</code> - whether to change all referring pages'
 * references to this page also. If not supplied, this parameter defaults to
 * <code>false</code></li>
 * </ul>
 * <h3>Actions</h3>
 * <ul>
 * <li><code>rename</code> - executes the rename action, using the value of
 * field <code>renameTo</code> as the new name</li>
 * </ul>
 * <h3>Special validation</h3>
 * <p>
 * Before the <code>rename</code> handler executes, the validation method
 * {@link #validateBeforeRename(ValidationErrors)} checks to make sure that the
 * proposed page name (supplied by {@link #setRenameTo(String)} is not already
 * used by an existing wiki page.
 * </p>
 * 
 */
@UrlBinding( "/Rename.jsp" )
public class RenameActionBean extends AbstractPageActionBean
{
    private static final Logger log = LoggerFactory.getLogger( RenameActionBean.class );

    private String m_renameTo = null;

    /**
     * Returns the proposed new name for the page; defaults to <code>null</code>
     * if not set.
     * 
     * @return the proposed new page name
     */
    public String getRenameTo()
    {
        return m_renameTo;
    }

    /**
     * Handler method that renames the current wiki page. If the rename
     * operation does not succeed for any reason, this method throws a
     * {@link org.apache.wiki.api.WikiException}.
     * 
     * @return a redirection to the renamed wiki page
     * @throws WikiException if the page cannot be renamed
     */
    @HandlesEvent( "rename" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.path}", actions = PagePermission.RENAME_ACTION )
    @WikiRequestContext( "rename" )
    public Resolution rename() throws WikiException
    {
        WikiEngine engine = getContext().getEngine();
        String renameFrom = getContext().getPage().getName();
        HttpServletRequest request = getContext().getRequest();
        log.info( "Page rename request for page '" + renameFrom + "' to new name '" + m_renameTo + "' from "
                  + request.getRemoteAddr() + " by " + request.getRemoteUser() );
        PageRenamer renamer = engine.getPageRenamer();
        String renamedTo = renamer.renamePage( getContext(), renameFrom, m_renameTo );
        
        BreadcrumbsTag.deleteFromBreadCrumb( request, renameFrom );
        log.info( "Page successfully renamed to '" + renamedTo + "'" );
        
        return new RedirectResolution( ViewActionBean.class ).addParameter( "page", renamedTo );
    }

    /**
     * Sets the new name for the page, which will be set when the
     * {@link #rename()} handler is executed.
     * 
     * @param pageName the new page name
     */
    @Validate( required = true, minlength = 1, maxlength = 100, expression = "page.name != renameTo" )
    public void setRenameTo( String pageName )
    {
        m_renameTo = pageName;
    }

    /**
     * Before the {@link #rename()} handler method executes, this method
     * validates that the proposed new name does not collide with an existing
     * page.
     * 
     * @param errors the current set of validation errors for this ActionBean
     */
    @ValidationMethod( on = "rename" )
    public void validateBeforeRename( ValidationErrors errors )
    {
        if( getContext().getEngine().pageExists( m_renameTo ) )
        {
            errors.add( "renameTo", new LocalizableError( "rename.exists" ) );
        }
    }

}
