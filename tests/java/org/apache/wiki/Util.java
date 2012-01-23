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
package org.apache.wiki;

import java.util.*;

/**
 * Utilities for tests.
 */
public class Util
{
    /**
     * Check that a collection contains the required object.
     * @param items the collection to check
     * @param item the item to look for
     * @return <code>true</code> if the collection contains an item equal
     * to the supplied item; <code>false</code> otherwise
     */
    public static boolean collectionContains( Collection<String> container,
                                              String captive )
    {
        for ( String cap : container )
        {
            if( cap != null && captive.equals( cap ) )
                return true;
        }

        return false;
    }
}
