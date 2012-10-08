/* 
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
package org.apache.wiki.dav.methods;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.wiki.dav.DavPath;
import org.apache.wiki.dav.DavProvider;

/**
 *
 *  @since 
 */
public class PropPatchMethod 
    extends DavMethod
{

    /**
     * Constructs a new DAV PropPatchMethod.
     * @param provider the DAV provider
     */
    public PropPatchMethod( DavProvider provider )
    {
        super( provider );
    }

    public void execute( HttpServletRequest req, HttpServletResponse res, DavPath dp ) throws ServletException, IOException
    {
        res.sendError( HttpServletResponse.SC_UNAUTHORIZED, "JSPWiki is read-only" );
    }

}
