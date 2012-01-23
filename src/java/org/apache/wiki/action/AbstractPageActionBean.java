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

import java.util.Collections;
import java.util.List;

import net.sourceforge.stripes.action.DontBind;
import net.sourceforge.stripes.validation.Validate;

import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.util.PageTimeComparator;

/**
 * Abstract {@link WikiActionBean} subclass used by all ActionBeans that use and
 * process {@link org.apache.wiki.api.WikiPage} objects bound to the
 * <code>page</code> request parameter. In particular, this subclass contains
 * special processing logic that ensures that, the <code>page</code> properties
 * of this object and its related {@link org.apache.wiki.WikiContext} are set to
 * the same value. When {@link #setPage(WikiPage)} is called by, for example,
 * the Stripes controller, the underlying
 * {@link org.apache.wiki.ui.stripes.WikiActionBeanContext#setPage(WikiPage)}
 * method is called also.
 */
public class AbstractPageActionBean extends AbstractActionBean
{
    private List<WikiPage> m_attachments = null;

    private List<WikiPage> m_history = null;

    private PageTimeComparator HISTORY_COMPARATOR = new PageTimeComparator( PageTimeComparator.Order.DESCENDING );

    /**
     * Lists the attachments for this WikiPage.
     * 
     * @return the attachments, possibly as a zero-length list
     * @throws ProviderException if the provider cannot fetch the history
     */
    @DontBind
    public List<WikiPage> getAttachments() throws ProviderException
    {
        if( m_attachments == null )
        {
            AttachmentManager mgr = getContext().getEngine().getAttachmentManager();
            m_attachments = Collections.unmodifiableList( mgr.listAttachments( getPage() ) );
        }
        return m_attachments;
    }

    /**
     * Returns the page history.
     * 
     * @return the history
     * @throws ProviderException if the provider cannot fetch the history
     * @throws PageNotFoundException if the page doesn't exist
     */
    @DontBind
    public List<WikiPage> getHistory() throws ProviderException, PageNotFoundException
    {
        if( m_history == null )
        {
            List<WikiPage> history = getContext().getEngine().getVersionHistory( getPage().getName() );
            Collections.sort( history, HISTORY_COMPARATOR );
            m_history = Collections.unmodifiableList( history );
        }
        return m_history;
    }

    /**
     * Returns the WikiPage; defaults to <code>null</code>.
     * 
     * @return the page
     */
    public WikiPage getPage()
    {
        return getContext().getPage();
    }

    /**
     * Sets the WikiPage property for this ActionBean, and also sets the
     * WikiActionBeanContext's page property to the same value by calling
     * {@link org.apache.wiki.ui.stripes.WikiActionBeanContext#setPage(WikiPage)}
     * . Note that because of the {@link Validate} annotation, the
     * <code>page</code> field will be required by any executing event handler
     * method. To change this property, subclasses should override this method
     * and supply a <code>Validate( required = false )</code> annotation.
     * 
     * @param page the wiki page.
     */
    @Validate( required = true )
    public void setPage( WikiPage page )
    {
        getContext().setPage( page );
    }
}
