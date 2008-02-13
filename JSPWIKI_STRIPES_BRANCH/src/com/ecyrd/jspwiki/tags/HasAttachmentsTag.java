/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.tags;

import java.io.IOException;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  Includes body if page has attachments.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class HasAttachmentsTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine = m_actionBean.getEngine();
        AttachmentManager mgr = engine.getAttachmentManager();

        try
        {
            if( m_page != null && engine.pageExists(m_page) && mgr.attachmentsEnabled() )
            {
                if( mgr.hasAttachments(m_page) )
                {
                    return EVAL_BODY_INCLUDE;
                }
            }
        }
        catch( ProviderException e )
        {
            log.fatal("Provider failed while trying to check for attachements",e);
            // FIXME: THrow something.
        }

        return SKIP_BODY;
    }
}
