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
package org.apache.wiki.content.lock;

import java.io.Serializable;
import java.util.Date;

import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.WikiPath;

/**
 *  Describes a lock acquired by an user on a page.  For the most part,
 *  the regular developer does not have to instantiate this class.
 *  <p>
 *  The PageLock keeps no reference to a WikiPage because otherwise it could
 *  keep a reference to a page for a long time.
 *
 */
public class PageLock
    implements Serializable
{
    private static final long serialVersionUID = 0L;
    
    private WikiPath m_page;
    private String   m_locker;
    private Date     m_lockAcquired;
    private Date     m_lockExpiry;

    /**
     *  Creates a new PageLock.  The lock is not attached to any objects at this point.
     *  
     *  @param page     WikiPage which is locked.
     *  @param locker   The username who locked this page (for display purposes).
     *  @param acquired The timestamp when the lock is acquired
     *  @param expiry   The timestamp when the lock expires.
     */
    public PageLock( WikiPage page, 
                     String locker,
                     Date acquired,
                     Date expiry )
    {
        m_page         = page.getPath();
        m_locker       = locker;
        m_lockAcquired = (Date)acquired.clone();
        m_lockExpiry   = (Date)expiry.clone();
    }

    /**
     *  Returns the name of the page which is locked.
     *  
     *  @return The name of the page.
     */
    public WikiPath getPath()
    {
        return m_page;
    }

    /**
     *  Returns the locker name.
     *  
     *  @return The name of the locker.
     */
    public String getLocker()
    {
        return m_locker;
    }

    /**
     *  Returns the timestamp on which this lock was acquired.
     *  
     *  @return The acquisition time.
     */
    public Date getAcquisitionTime()
    {
        return m_lockAcquired;
    }

    /**
     *  Returns the timestamp on which this lock will expire.
     *  
     *  @return The expiry date.
     */
    public Date getExpiryTime()
    {
        return m_lockExpiry;
    }

    /**
     *  Returns the amount of time left in minutes, rounded up to the nearest
     *  minute (so you get a zero only at the last minute).
     *  
     *  @return Time left in minutes.
     */
    public long getTimeLeft()
    {
        long time = m_lockExpiry.getTime() - new Date().getTime();

        return (time / (1000L * 60)) + 1;
    }

    /**
     * Returns true if the lock is expired.
     * 
     * @return true if lock is expired
     */
    public boolean isExpired()
    {
        return m_lockExpiry.before( new Date() );
    }
    
    /**
     * Returns all available information on the lock
     */
    public String toString() 
    {
        return "pagename:" + getPath() + ",locker:" + getLocker() + ",acquired:" + getAcquisitionTime() + ",expiry:" + getExpiryTime();
    }
}
