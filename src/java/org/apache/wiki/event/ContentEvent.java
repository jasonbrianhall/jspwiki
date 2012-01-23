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
package org.apache.wiki.event;

import java.io.Serializable;

import org.apache.wiki.content.WikiPath;

/**
 * Events fired by {@link org.apache.wiki.content.ContentManager} when nodes are
 * created, saved or deleted.
 */
// FIXME: The parameters expected by the different events need to be documented.
public class ContentEvent extends WikiPageEvent
{
    private static final long serialVersionUID = -6577147048708900469L;

    /**
     * Indicates that a node has been requested to be deleted, but it has not
     * yet been removed from the repository.
     */
    public static final int NODE_DELETE_REQUEST = 220;

    /**
     * Indicates that a node was successfully deleted.
     */
    public static final int NODE_DELETED = 221;

    /**
     *  Indicates that a node was successfully renamed.
     *  Parameters are:
     *  <ul>
     *   <li>pagename = page which has been renamed
     *   <li>first argument (String) = the new name of the page (FQN)
     *   <li>second argument (Boolean) = true, if the referrers should be changed.
     *  </ul>
     */
    public static final int NODE_RENAMED = 211;

    /**
     * Indicates a node was successfully saved.
     */
    public static final int NODE_SAVED = 201;

    /**
     * Constructs an instance of this event.
     * 
     * @param src the Object that is the source of the event.
     * @param type the type of the event (see the enumerated int values defined
     *            in {@link org.apache.wiki.event.WikiEvent}).
     * @param pagename the WikiPage being acted upon.
     * @param args additional arguments passed to the event.
     */
    public ContentEvent( Object src, int type, WikiPath pagename, Serializable... args )
    {
        super( src, type, pagename, args );
    }
}
