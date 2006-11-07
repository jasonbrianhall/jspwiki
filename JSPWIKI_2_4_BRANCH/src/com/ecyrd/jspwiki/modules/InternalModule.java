/* 
   JSPWiki - a JSP-based WikiWiki clone.

   Copyright (C) 2001-2006 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

package com.ecyrd.jspwiki.modules;

/**
 *  This is a simple interface which is implemented by a number of JSPWiki
 *  components to signal that they should not be included in things like
 *  module listings and so on.  Because JSPWiki reuses its internal module
 *  mechanisms for its own use as well (e.g. RenderingManager uses a PageFilter
 *  to catch page saves), it's sort of dumb to have these all appear in the
 *  "installed filters" list.
 *  <p>
 *  A plugin developer should never implement this interface.
 *  
 *  @author jalkanen
 *  @since 2.4
 */
public interface InternalModule
{
}
