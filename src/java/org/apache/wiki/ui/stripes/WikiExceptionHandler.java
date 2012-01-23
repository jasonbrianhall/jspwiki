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

package org.apache.wiki.ui.stripes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.exception.DefaultExceptionHandler;

import org.apache.wiki.action.MessageActionBean;

/**
 * Stripes ExceptionHandler that catches exceptions of various types and returns
 * appropriate Resolutions.
 */
public class WikiExceptionHandler extends DefaultExceptionHandler
{

    /**
     * Catches any Exceptions not handled by other methods in this class,
     * prints a stack trace, and forwards to {@link MessageActionBean}.
     * It also adds the caught exception into page scope as an attribute
     * with the standard error name {@link PageContext#EXCEPTION}.
     * 
     * @param exception the exception caught by StripesFilter
     * @param req the current HTTP request
     * @param res the current HTTP response
     * @return always returns a ForwardResolution to the handler method
     *         {@link MessageActionBean#error()}
     */
    public Resolution catchAll( Throwable exception, HttpServletRequest req, HttpServletResponse res )
    {
        exception.printStackTrace();
        req.setAttribute( PageContext.EXCEPTION, exception );
        return new ForwardResolution( MessageActionBean.class, "error" );
    }
}
