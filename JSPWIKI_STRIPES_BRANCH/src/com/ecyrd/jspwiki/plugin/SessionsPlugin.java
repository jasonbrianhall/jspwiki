/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.plugin;

import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;

/**
 *  <p>Displays information about active wiki sessions. The parameter
 *  <code>property</code> specifies what information is displayed.
 *  If omitted, the number of sessions is returned.
 *  Valid values for the <code>property</code> parameter
 *  include:</p>
 *  <ul>
 *    <li><code>users</code> - returns a comma-separated list of
 *    users</li>
 *    <li><code>distinctUsers</code> - will only show
 *    distinct users.
 *  </ul>
 *  @since 2.3.84
 *  @author Andrew Jaquith
 */
public class SessionsPlugin
    implements WikiPlugin
{
    public static final String PARAM_PROP = "property";
    
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        WikiEngine engine = context.getEngine();
        String prop = (String) params.get( PARAM_PROP );
        
        if ( "users".equals( prop ) )
        {
            Principal[] principals = WikiSession.userPrincipals( engine );
            StringBuffer s = new StringBuffer();
            for ( int i = 0; i < principals.length; i++ )
            {
                s.append(principals[i].getName() + ", ");
            }
            // remove the last comma and blank :
            return s.substring(0, s.length() - (s.length() > 2 ? 2 : 0) );
        }

        //
        // show each user session only once (with a counter that indicates the
        // number of sessions for each user)
        if ("distinctUsers".equals(prop))
        {
            Principal[] principals = WikiSession.userPrincipals(engine);
            // we do not assume that the principals are sorted, so first count
            // them :
            HashMap distinctPrincipals = new HashMap();
            for (int i = 0; i < principals.length; i++)
            {
                String principalName = principals[i].getName();

                if (distinctPrincipals.containsKey(principalName))
                {
                    // we already have an entry, increase the counter:
                    int numSessions = ((Integer) distinctPrincipals.get(principalName)).intValue();
                    // store the new value:
                    distinctPrincipals.put(principalName, new Integer(++numSessions));
                }
                else
                {
                    // first time we see this entry, add entry to HashMap with
                    // value 1
                    distinctPrincipals.put(principalName, new Integer(1));
                }
            }
            //
            //
            StringBuffer s = new StringBuffer();
            Iterator entries = distinctPrincipals.entrySet().iterator();
            while (entries.hasNext())
            {
                Map.Entry entry = (Map.Entry)entries.next();
                s.append((entry.getKey().toString() + "(" + entry.getValue().toString() + "), "));
            }
            // remove the last comma and blank :
            return s.substring(0, s.length() - 2);
        }

        return String.valueOf( WikiSession.sessions( engine ) );
    }
}