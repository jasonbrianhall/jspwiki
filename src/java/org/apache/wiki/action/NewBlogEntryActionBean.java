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

import org.apache.wiki.auth.permissions.WikiPermission;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiRequestContext;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;


@UrlBinding("/NewBlogEntry.jsp")
public class NewBlogEntryActionBean extends AbstractActionBean
{
    @HandlesEvent("create")
    @HandlerPermission(permissionClass=WikiPermission.class, target="${engine.applicationName}", actions=WikiPermission.CREATE_PAGES_ACTION)
    @WikiRequestContext("newBlogEntry")
    public Resolution create()
    {
        return null;
    }
}