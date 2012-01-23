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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.wiki.action.WikiActionBean;
import org.apache.wiki.action.WikiContextFactory;
import org.apache.wiki.api.FilterException;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.SessionMonitor;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.acl.AclManager;
import org.apache.wiki.auth.acl.DefaultAclManager;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.content.*;
import org.apache.wiki.content.resolver.EnglishPluralsPageNameResolver;
import org.apache.wiki.content.resolver.PageNameResolver;
import org.apache.wiki.content.resolver.SpecialPageNameResolver;
import org.apache.wiki.diff.DifferenceManager;
import org.apache.wiki.event.WikiEngineEvent;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.filters.FilterManager;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.plugin.PluginManager;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.render.RenderingManager;
import org.apache.wiki.rss.RSSGenerator;
import org.apache.wiki.rss.RSSThread;
import org.apache.wiki.search.SearchManager;
import org.apache.wiki.search.SearchResult;
import org.apache.wiki.ui.EditorManager;
import org.apache.wiki.ui.TemplateManager;
import org.apache.wiki.ui.admin.AdminBeanManager;
import org.apache.wiki.ui.progress.ProgressManager;
import org.apache.wiki.ui.stripes.WikiActionBeanContext;
import org.apache.wiki.ui.stripes.WikiInterceptor;
import org.apache.wiki.url.StripesURLConstructor;
import org.apache.wiki.util.*;
import org.apache.wiki.workflow.*;


/**
 *  Provides Wiki services to the JSP page.
 *
 *  <P>
 *  This is the main interface through which everything should go.
 *
 *  <P>
 *  Using this class:  Always get yourself an instance from JSP page
 *  by using the WikiEngine.getInstance() method.  Never create a new
 *  WikiEngine() from scratch, unless you're writing tests.
 *  <p>
 *  There's basically only a single WikiEngine for each web application, and
 *  you should always get it using the WikiEngine.getInstance() method.
 */
public class WikiEngine
{
    private static final String ATTR_WIKIENGINE = "org.apache.wiki.WikiEngine";

    private static final Logger log = LoggerFactory.getLogger(WikiEngine.class);

    /** True, if log4j has been configured. */
    // FIXME: If you run multiple applications, the first application
    // to run defines where the log goes.  Not what we want.
//    private static boolean   c_configured = false;

    /** Stores properties. */
    private Properties       m_properties;

    /** Property for application name */
    public static final String PROP_APPNAME      = "jspwiki.applicationName";

    /** Property start for any interwiki reference. */
    public static final String PROP_INTERWIKIREF = "jspwiki.interWikiRef.";

    /** If true, then the user name will be stored with the page data.*/
    public static final String PROP_STOREUSERNAME= "jspwiki.storeUserName";

    /** The name for the base URL to use in all references. */
    public static final String PROP_BASEURL      = "jspwiki.baseURL";

    /** The name for the property which allows you to set the current reference
     *  style.  The value is {@value}.
     */
    public static final String PROP_REFSTYLE     = "jspwiki.referenceStyle";

    /** Property name for the "spaces in titles" -hack. */
    public static final String PROP_BEAUTIFYTITLE = "jspwiki.breakTitleWithSpaces";

    /** Property name for where the jspwiki work directory should be.
        If not specified, reverts to ${java.tmpdir}. */
    public static final String PROP_WORKDIR      = "jspwiki.workDir";

    /** The name of the cookie that gets stored to the user browser. */
    public static final String PREFS_COOKIE_NAME = "JSPWikiUserProfile";

    /** Property name for the "match english plurals" -hack. */
    public static final String PROP_MATCHPLURALS     = "jspwiki.translatorReader.matchEnglishPlurals";

    /** Property name for the template that is used. */
    public static final String PROP_TEMPLATEDIR  = "jspwiki.templateDir";

    /** Property name for the default front page. */
    public static final String PROP_FRONTPAGE    = "jspwiki.frontPage";

    /** Property name for setting the url generator instance */

    public static final String PROP_URLCONSTRUCTOR = "jspwiki.urlConstructor";

    /** If this property is set to false, all filters are disabled when translating. */
    public static final String PROP_RUNFILTERS   = "jspwiki.runFilters";

    /** Does the work in renaming pages. */
    private PageRenamer    m_pageRenamer = null;

    /** The name of the property containing the ACLManager implementing class.
     *  The value is {@value}. */
    public static final String PROP_ACL_MANAGER_IMPL = "jspwiki.aclManager";
    
    /** If this property is set to false, we don't allow the creation of empty pages */
    public static final String PROP_ALLOW_CREATION_OF_EMPTY_PAGES = "jspwiki.allowCreationOfEmptyPages";

    private static final Comparator<WikiPage> PAGE_TIME_COMPARATOR = PageTimeComparator.DEFAULT_PAGETIME_COMPARATOR;

    /** Should the user info be saved with the page data as well? */
    private boolean          m_saveUserInfo = true;

    /** Stores the base URL. */
    private String           m_baseURL;

    /** Store the file path to the basic URL.  When we're not running as
        a servlet, it defaults to the user's current directory. */
    private String           m_rootPath = System.getProperty("user.dir");

    /** Stores references between wikipages. */
    private ReferenceManager m_referenceManager = null;

    /** Stores the Plugin manager */
    private PluginManager    m_pluginManager;

    /** Stores the Variable manager */
    private VariableManager  m_variableManager;

    /** Stores the Attachment manager */
    private AttachmentManager m_attachmentManager = null;

    /** Stores the Page manager */
    private PageManager      m_pageManager = null;

    /** Stores the authorization manager */
    private AuthorizationManager m_authorizationManager = null;

    /** Stores the authentication manager.*/
    private AuthenticationManager      m_authenticationManager = null;

    /** Stores the ACL manager. */
    private AclManager       m_aclManager = null;

    private ContentManager   m_contentManager;
    
    /** Creates WikiContexts. */
    private WikiContextFactory m_contextFactory = null;

    private TemplateManager  m_templateManager = null;

    /** Does all our diffs for us. */
    private DifferenceManager m_differenceManager;

    /** Handlers page filters. */
    private FilterManager    m_filterManager;

    /** Stores the Search manager */
    private SearchManager    m_searchManager = null;

    /** Facade for managing users */
    private UserManager      m_userManager = null;

    /** Facade for managing users */
    private GroupManager     m_groupManager = null;

    private RenderingManager m_renderingManager;

    private EditorManager    m_editorManager;

    private InternationalizationManager m_internationalizationManager;

    private ProgressManager  m_progressManager;

    /** Constructs URLs */
    private StripesURLConstructor   m_urlConstructor;

    /** Generates RSS feed when requested. */
    private RSSGenerator     m_rssGenerator;

    /** The RSS file to generate. */
    private String           m_rssFile;

    /** Store the ServletContext that we're in.  This may be null if WikiEngine
        is not running inside a servlet container (i.e. when testing). */
    private ServletContext   m_servletContext = null;

    /** If true, all titles will be cleaned. */
    private boolean          m_beautifyTitle = false;

    /** Stores the template path.  This is relative to "templates". */
    private String           m_templateDir;

    /** The default front page name.  Defaults to "Main". */
    private String           m_frontPage;

    /** The time when this engine was started. */
    private Date             m_startTime;

    /** The location where the work directory is. */
    private String           m_workDir;

    /** Each engine has their own application id. */
    private String           m_appid = "";

    /** The spaces defined for this WikiEngine. */
    private String[] m_spaces;

    private boolean          m_isConfigured = false; // Flag.

    private List<PageNameResolver> m_nameResolvers = new ArrayList<PageNameResolver>();

    private SpecialPageNameResolver m_specialPageResolver;
    
    /** Each engine has its own workflow manager. */
    private WorkflowManager m_workflowMgr = null;

    private AdminBeanManager m_adminBeanManager;

    /** Stores wikiengine attributes. */
    private Map<String,Object> m_attributes = Collections.synchronizedMap(new HashMap<String,Object>());

    /**
     *  Gets a WikiEngine related to this servlet.  Since this method
     *  is only called from JSP pages (and JspInit()) to be specific,
     *  we throw a RuntimeException if things don't work.
     *
     *  @param config The ServletConfig object for this servlet.
     *
     *  @return A WikiEngine instance.
     *  @throws InternalWikiException in case something fails.  This
     *          is a RuntimeException, so be prepared for it.
     */

    // FIXME: It seems that this does not work too well, jspInit()
    // does not react to RuntimeExceptions, or something...

    public static synchronized WikiEngine getInstance( ServletConfig config )
        throws InternalWikiException
    {
        return getInstance( config.getServletContext(), null );
    }

    /**
     *  Gets a WikiEngine related to the servlet. Works like getInstance(ServletConfig),
     *  but does not force the Properties object. This method is just an optional way
     *  of initializing a WikiEngine for embedded JSPWiki applications; normally, you
     *  should use getInstance(ServletConfig).
     *
     *  @param config The ServletConfig of the webapp servlet/JSP calling this method.
     *  @param props  A set of properties, or null, if we are to load JSPWiki's default
     *                jspwiki.properties (this is the usual case).
     *
     *  @return One well-behaving WikiEngine instance.
     */
    public static synchronized WikiEngine getInstance( ServletConfig config,
                                                       Properties props )
    {
        return getInstance( config.getServletContext(), props );
    }

    /**
     *  Gets a WikiEngine related to the servlet. Works just like getInstance( ServletConfig )
     *
     *  @param context The ServletContext of the webapp servlet/JSP calling this method.
     *  @param props  A set of properties, or null, if we are to load JSPWiki's default
     *                jspwiki.properties (this is the usual case).
     *
     *  @return One fully functional, properly behaving WikiEngine.
     *  @throws InternalWikiException If the WikiEngine instantiation fails.
     */

    // FIXME: Potential make-things-easier thingy here: no need to fetch the wikiengine anymore
    //        Wiki.jsp.jspInit() [really old code]; it's probably even faster to fetch it
    //        using this method every time than go to pageContext.getAttribute().

    public static synchronized WikiEngine getInstance( ServletContext context,
                                                       Properties props )
        throws InternalWikiException
    {
        WikiEngine engine = (WikiEngine) context.getAttribute( ATTR_WIKIENGINE );

        if( engine == null )
        {
            String appid = Integer.toString(context.hashCode()); //FIXME: Kludge, use real type.

            context.log(" Assigning new engine to "+appid);
            try
            {
                if( props == null )
                {
                    props = PropertyReader.loadWebAppProps( context );
                }

                engine = new WikiEngine( context, appid, props );
                context.setAttribute( ATTR_WIKIENGINE, engine );
            }
            catch( Exception e )
            {
                context.log( "ERROR: Failed to create a Wiki engine: "+e.getMessage() );
                throw new InternalWikiException( "No wiki engine, check logs." );
            }

        }

        return engine;
    }


    /**
     *  Instantiate the WikiEngine using a given set of properties.
     *  Use this constructor for testing purposes only.
     *
     *  @param properties A set of properties to use to initialize this WikiEngine.
     *  @throws WikiException If the initialization fails.
     */
    public WikiEngine( Properties properties )
        throws WikiException
    {
        try
        {
            initialize( properties );
        }
        catch( WikiException e )
        {
            shutdown();
            throw e;
        }
    }

    /**
     *  Instantiate using this method when you're running as a servlet and
     *  WikiEngine will figure out where to look for the property
     *  file.
     *  Do not use this method - use WikiEngine.getInstance() instead.
     *
     *  @param context A ServletContext.
     *  @param appid   An Application ID.  This application is an unique random string which
     *                 is used to recognize this WikiEngine.
     *  @param props   The WikiEngine configuration.
     *  @throws WikiException If the WikiEngine construction fails.
     */
    protected WikiEngine( ServletContext context, String appid, Properties props )
        throws WikiException
    {
        super();
        m_servletContext = context;
        m_appid          = appid;

        // Stash the WikiEngine in the servlet context
        if ( context != null )
        {
            context.setAttribute( ATTR_WIKIENGINE,  this );
            m_rootPath = context.getRealPath("/");
        }
        
        try
        {
            //
            //  Note: May be null, if JSPWiki has been deployed in a WAR file.
            //
            initialize( props );
            if ( m_rootPath == null )
            {
                log.warn("Root path for this Wiki is null. This is normal if deployed as a WAR or executed in mock context.");
            }
            else
            {
                log.info("Root path for this Wiki is: '"+m_rootPath+"'");
            }
        }
        catch( Exception e )
        {
            String msg = Release.APPNAME+": Unable to load and setup properties from jspwiki.properties. "+e.getMessage();
            if ( context != null )
            {
                context.log( msg );
            }
            shutdown();
            throw new WikiException( msg, e );
        }
    }

    /**
     *  Does all the real initialization.
     */
    private void initialize( Properties props )
        throws WikiException
    {
        m_startTime  = new Date();
        m_properties = props;

        LoggerFactory.initialize( m_properties.getProperty( PROP_APPNAME ) );

        log.info("*******************************************");
        log.info(Release.APPNAME+" "+Release.getVersionString()+" starting. Whee!");
        
        fireEvent( WikiEngineEvent.INITIALIZING ); // begin initialization

        log.debug("Java version: "+System.getProperty("java.runtime.version"));
        log.debug("Java vendor: "+System.getProperty("java.vm.vendor"));
        log.debug("OS: "+System.getProperty("os.name")+" "+System.getProperty("os.version")+" "+System.getProperty("os.arch"));
        log.debug("Default server locale: "+Locale.getDefault());
        log.debug("Default server timezone: "+TimeZone.getDefault().getDisplayName(true, TimeZone.LONG));

        if( m_servletContext != null )
        {
            log.info("Servlet container: "+m_servletContext.getServerInfo() );
            if( m_servletContext.getMajorVersion() < 2 ||
                (m_servletContext.getMajorVersion() == 2 && m_servletContext.getMinorVersion() < 4) )
            {
                throw new InternalWikiException("I require a container which supports at least version 2.4 of Servlet specification");
            }
        }

        log.debug("Configuring WikiEngine...");

        //
        //  Create and find the default working directory.
        //
        m_workDir        = TextUtil.getStringProperty( props, PROP_WORKDIR, null );

        if( m_workDir == null )
        {
            m_workDir = System.getProperty("java.io.tmpdir", ".");
            m_workDir += File.separator+Release.APPNAME+"-"+m_appid;
        }

        try
        {
            File f = new File( m_workDir );
            f.mkdirs();

            //
            //  A bunch of sanity checks
            //
            if( !f.exists() ) throw new WikiException("Work directory does not exist: "+m_workDir);
            if( !f.canRead() ) throw new WikiException("No permission to read work directory: "+m_workDir);
            if( !f.canWrite() ) throw new WikiException("No permission to write to work directory: "+m_workDir);
            if( !f.isDirectory() ) throw new WikiException("jspwiki.workDir does not point to a directory: "+m_workDir);
        }
        catch( SecurityException e )
        {
            log.error("Unable to find or create the working directory: "+m_workDir,e);
            throw new IllegalArgumentException("Unable to find or create the working dir: "+m_workDir);
        }

        log.info("JSPWiki working directory is '"+m_workDir+"'");

        m_saveUserInfo   = TextUtil.getBooleanProperty( props,
                                                        PROP_STOREUSERNAME,
                                                        m_saveUserInfo );

        m_baseURL = TextUtil.getStringProperty( props, PROP_BASEURL, "" );
        if( !m_baseURL.endsWith( "/" ) )
        {
            m_baseURL = m_baseURL + "/";
        }


        m_beautifyTitle  = TextUtil.getBooleanProperty( props,
                                                        PROP_BEAUTIFYTITLE,
                                                        m_beautifyTitle );

        m_templateDir    = TextUtil.getStringProperty( props, PROP_TEMPLATEDIR, "default" );
        m_frontPage      = TextUtil.getStringProperty( props, PROP_FRONTPAGE,   "Main" );

        // Initialize the spaces for this wiki
        // TODO: make this configurable
        m_spaces = new String[] { ContentManager.DEFAULT_SPACE };

        //
        //  Initialize the important modules.  Any exception thrown by the
        //  managers means that we will not start up.
        //

        // FIXME: This part of the code is getting unwieldy.  We must think
        //        of a better way to do the startup-sequence.
        try
        {
            //  Initialize the WikiContextFactory -- this MUST be done after setting the baseURL
            m_urlConstructor = new StripesURLConstructor();
            m_urlConstructor.initialize( this, props );
            m_contextFactory  = new WikiContextFactory( this, props );

            // Initialize the JMX timer MBean
            WikiBackgroundThread.registerTimer( this );
            
            /**
             *  We treat the specialPageResolver in a slightly different way
             *  than others.
             */
            m_specialPageResolver = new SpecialPageNameResolver(this); 
            m_nameResolvers.add( m_specialPageResolver );
            m_nameResolvers.add( new EnglishPluralsPageNameResolver( this ) );
            
            m_contentManager    = (ContentManager)ClassUtil.getMappedObject(ContentManager.class.getName(), this );
            m_pageManager       = (PageManager)ClassUtil.getMappedObject(PageManager.class.getName(), this, props );
            m_pluginManager     = (PluginManager)ClassUtil.getMappedObject(PluginManager.class.getName(), this, props );
            m_differenceManager = (DifferenceManager)ClassUtil.getMappedObject(DifferenceManager.class.getName(), this, props );
            m_attachmentManager = (AttachmentManager)ClassUtil.getMappedObject(AttachmentManager.class.getName(), this, props );
            m_variableManager   = (VariableManager)ClassUtil.getMappedObject(VariableManager.class.getName(), props );
            // m_filterManager     = (FilterManager)ClassUtil.getMappedObject(FilterManager.class.getName(), this, props );
            m_renderingManager  = (RenderingManager) ClassUtil.getMappedObject(RenderingManager.class.getName());

            m_authenticationManager = (AuthenticationManager) ClassUtil.getMappedObject(AuthenticationManager.class.getName());
            m_authorizationManager  = (AuthorizationManager) ClassUtil.getMappedObject( AuthorizationManager.class.getName());
            m_userManager           = (UserManager) ClassUtil.getMappedObject(UserManager.class.getName());
            m_groupManager          = (GroupManager) ClassUtil.getMappedObject(GroupManager.class.getName());

            m_editorManager     = (EditorManager)ClassUtil.getMappedObject(EditorManager.class.getName(), this );
            m_editorManager.initialize( props );

            m_progressManager   = new ProgressManager();

            // Initialize the authentication, authorization, user and acl managers

            m_authenticationManager.initialize( this, props );
            m_authorizationManager.initialize( this, props );
            m_userManager.initialize( this, props );
            m_groupManager.initialize( this, props );
            m_aclManager = getAclManager();

            // Start the Workflow manager
            m_workflowMgr = (WorkflowManager)ClassUtil.getMappedObject(WorkflowManager.class.getName());
            m_workflowMgr.initialize(this, props);

            m_internationalizationManager = (InternationalizationManager)
                ClassUtil.getMappedObject(InternationalizationManager.class.getName(),this);

            m_templateManager   = (TemplateManager)
                ClassUtil.getMappedObject(TemplateManager.class.getName(), this, props );

            m_adminBeanManager = (AdminBeanManager)
                ClassUtil.getMappedObject(AdminBeanManager.class.getName(),this);

            // Since we want to use a page filters initialize() method
            // as a engine startup listener where we can initialize global event listeners,
            // it must be called lastly, so that all object references in the engine
            // are availabe to the initialize() method
            m_filterManager     = (FilterManager)
                ClassUtil.getMappedObject(FilterManager.class.getName(), this, props );

            // RenderingManager depends on FilterManager events.

            m_renderingManager.initialize( this, props );

            //
            //  ReferenceManager and SearchManager  must start after ContentManager does.
            //
            m_referenceManager = (ReferenceManager)
                ClassUtil.getMappedObject(ReferenceManager.class.getName() );
            m_referenceManager.initialize( this, props );
            
            m_searchManager     = (SearchManager)
                ClassUtil.getMappedObject(SearchManager.class.getName() );
            m_searchManager.initialize( this, props );
        }

        catch( RuntimeException e )
        {
            // RuntimeExceptions may occur here, even if they shouldn't.
            log.error( "Failed to start managers, stacktrace follows: ", e );
            throw new WikiException( "Failed to start managers: "+e.getMessage(), e );
        }
        catch( Exception e )
        {
            // Final catch-all for everything
            
            log.error( "JSPWiki could not start, due to an unknown exception while starting, stacktrace follows: ", e );
            throw new WikiException( "Failed to start; please check log files for better information.", e );
        }
        
        //
        //  Initialize the good-to-have-but-not-fatal modules.
        //
        try
        {
            if( TextUtil.getBooleanProperty( props,
                                             RSSGenerator.PROP_GENERATE_RSS,
                                             false ) )
            {
                m_rssGenerator = (RSSGenerator)ClassUtil.getMappedObject(RSSGenerator.class.getName(), this, props );
            }

            m_pageRenamer = (PageRenamer)ClassUtil.getMappedObject(PageRenamer.class.getName(), this, props );
        }
        catch( Exception e )
        {
            log.error( "Unable to start RSS generator - JSPWiki will still work, "+
                       "but there will be no RSS feed.", e );
        }

        // Start the RSS generator & generator thread
        if( m_rssGenerator != null )
        {
            m_rssFile = TextUtil.getStringProperty( props,
                    RSSGenerator.PROP_RSSFILE, "rss.rdf" );
            File rssFile=null;
            if (m_rssFile.startsWith(File.separator))
            {
                // honor absolute pathnames:
                rssFile = new File(m_rssFile );
            }
            else
            {
                // relative path names are anchored from the webapp root path:
                rssFile = new File( getRootPath(), m_rssFile );
            }
            int rssInterval = TextUtil.getIntegerProperty( props,
                    RSSGenerator.PROP_INTERVAL, 3600 );
            RSSThread rssThread = new RSSThread( this, rssFile, rssInterval );
            rssThread.start();
        }

        fireEvent( WikiEngineEvent.INITIALIZED ); // initialization complete

        log.info("WikiEngine configured.");
        m_isConfigured = true;
    }

    /**
     *  Throws an exception if a property is not found.
     *
     *  @param props A set of properties to search the key in.
     *  @param key   The key to look for.
     *  @return The required property
     *
     *  @throws NoRequiredPropertyException If the search key is not
     *          in the property set.
     */

    // FIXME: Should really be in some util file.
    public static String getRequiredProperty( Properties props, String key )
        throws NoRequiredPropertyException
    {
        String value = TextUtil.getStringProperty( props, key, null );

        if( value == null )
        {
            throw new NoRequiredPropertyException( "Required property not found",
                                                   key );
        }

        return value;
    }

    /**
     *  Returns the set of properties that the WikiEngine was initialized
     *  with.  Note that this method returns a direct reference, so it's possible
     *  to manipulate the properties.  However, this is not advised unless you
     *  really know what you're doing.
     *
     *  @return The wiki properties
     */

    public Properties getWikiProperties()
    {
        return m_properties;
    }

    /**
     *  Returns the JSPWiki working directory set with "jspwiki.workDir".
     *
     *  @since 2.1.100
     *  @return The working directory.
     */
    public String getWorkDir()
    {
        return m_workDir;
    }

    /**
     *  Don't use.
     *  @since 1.8.0
     *  @deprecated
     *  @return Something magical.
     */
    public String getPluginSearchPath()
    {
        // FIXME: This method should not be here, probably.
        return TextUtil.getStringProperty( m_properties, PluginManager.PROP_SEARCHPATH, null );
    }

    /**
     *  Returns the current template directory.
     *
     *  @since 1.9.20
     *  @return The template directory as initialized by the engine.
     */
    public String getTemplateDir()
    {
        return m_templateDir;
    }

    /**
     *  Returns the current TemplateManager.
     *
     *  @return A TemplateManager instance.
     */
    public TemplateManager getTemplateManager()
    {
        return m_templateManager;
    }

    /**
     *  Returns the base URL, telling where this Wiki actually lives. The baseURL
     *  is guaranteed to be returned with a trailing slash (/).
     *
     *  @since 1.6.1
     *  @return The Base URL.
     */

    public String getBaseURL()
    {
        return m_baseURL;
    }

    /**
     * Returns the spaces this wiki knows about.
     * @return an array of Strings representing the spaces defined for this wiki.
     */
    public String[] getSpaces()
    {
        return m_spaces;
    }

    /**
     *  Returns the moment when this engine was started.
     *
     *  @since 2.0.15.
     *  @return The start time of this wiki.
     */

    public Date getStartTime()
    {
        return (Date)m_startTime.clone();
    }

    /**
     * <p>
     * Returns the basic absolute URL to a page, without any modifications. You
     * may add any parameters to this.
     * </p>
     * <p>
     * Since 2.3.90 it is safe to call this method with <code>null</code>
     * pageName, in which case it will default to the front page.
     * </p>
     * @since 2.0.3
     * @param pageName The name of the page.  May be null, in which case defaults to the front page.
     * @return An absolute URL to the page.
     * @deprecated
     */
    public String getViewURL( String pageName )
    {
        if( pageName == null )
        {
            pageName = getFrontPage();
        }
        return getURLConstructor().makeURL( WikiContext.VIEW, pageName, true, null );
    }

    /**
     *  Returns the basic URL to an editor.  Please use WikiContext.getURL() or
     *  WikiEngine.getURL() instead.
     *
     *  @see #getURL(String, String, String, boolean)
     *  @see WikiContext#getURL(String, String)
     *  @deprecated
     *  
     *  @param pageName The name of the page.
     *  @return An URI.
     *
     *  @since 2.0.3
     */
    public String getEditURL( String pageName )
    {
        return m_urlConstructor.makeURL( WikiContext.EDIT, pageName, false, null );
    }

    /**
     *  Returns the basic attachment URL.Please use WikiContext.getURL() or
     *  WikiEngine.getURL() instead.
     *
     *  @see #getURL(String, String, String, boolean)
     *  @see WikiContext#getURL(String, String)
     *  @since 2.0.42.
     *  @param attName Attachment name
     *  @deprecated
     *  @return An URI.
     */
    public String getAttachmentURL( String attName )
    {
        return m_urlConstructor.makeURL( WikiContext.ATTACH, attName, false, null );
    }

    /**
     *  Returns an URL if a WikiContext is not available.
     *
     *  @param context The WikiContext (VIEW, EDIT, etc...)
     *  @param pageName Name of the page, as usual
     *  @param params List of parameters. May be null, if no parameters.
     *  @param absolute If true, will generate an absolute URL regardless of properties setting.
     *  @return An URL (absolute or relative).
     */
    public String getURL( String context, String pageName, String params, boolean absolute )
    {
        if( pageName == null ) pageName = getFrontPage();
        return m_urlConstructor.makeURL( context, pageName, absolute, params );
    }

    /**
     *  Returns the default front page, if no page is used.
     *
     *  @return The front page name.
     */
    // FIXME: This method should return a WikiPage
    // FIXME: This method should return the FQN of the defaultspace:frontpage page of the wiki.
    public String getFrontPage()
    {
        try
        {
            return getFrontPage(null).getPath().toString();
        }
        catch( ProviderException e )
        {
            return "ErrorFrontPageCannotBeDetermined";
        }
    }

    /**
     *  Returns the default front page for a particular space.  Always returns
     *  a valid WikiPage, even if the front page does not exist.
     *  
     *  @param space The space to get the front page for.
     *  @return A FQN of the page.
     * @throws ProviderException 
     *  @since 3.0
     */
    // FIXME: Does not yet support spaces
    public WikiPage getFrontPage( String space ) throws ProviderException
    {
        WikiPage p;
        WikiPath name = new WikiPath( space, m_frontPage );

        try
        {
            p = m_contentManager.getPage( name );
        }
        catch( PageNotFoundException e )
        {
            try
            {
                p = createPage( name );
            }
            catch( PageAlreadyExistsException e1 )
            {
                // This should not happen
                throw new ProviderException( e1.getMessage(), e1 );
            }
        }
        
        return p;
    }
    
    /**
     *  Returns the ServletContext that this particular WikiEngine was
     *  initialized with.  <B>It may return null</B>, if the WikiEngine is not
     *  running inside a servlet container!
     *
     *  @since 1.7.10
     *  @return ServletContext of the WikiEngine, or null.
     */

    public ServletContext getServletContext()
    {
        return m_servletContext;
    }

    /**
     *  This is a safe version of the Servlet.Request.getParameter() routine.
     *  Unfortunately, the default version always assumes that the incoming
     *  character set is ISO-8859-1, even though it was something else.
     *  This means that we need to make a new string using the correct
     *  encoding.
     *  <P>
     *  For more information, see:
     *     <A HREF="http://www.jguru.com/faq/view.jsp?EID=137049">JGuru FAQ</A>.
     *  <P>
     *  Incidentally, this is almost the same as encodeName(), below.
     *  I am not yet entirely sure if it's safe to merge the code.
     *
     *  @param request The servlet request
     *  @param name    The parameter name to get.
     *  @return The parameter value or null
     *  @since 1.5.3
     *  @deprecated JSPWiki now requires servlet API 2.3, which has a better
     *              way of dealing with this stuff.  This will be removed in
     *              the near future.
     */

    public String safeGetParameter( ServletRequest request, String name )
    {
        try
        {
            String res = request.getParameter( name );
            if( res != null )
            {
                res = new String(res.getBytes("ISO-8859-1"),
                                 getContentEncoding() );
            }

            return res;
        }
        catch( UnsupportedEncodingException e )
        {
            log.error( "Unsupported encoding", e );
            return "";
        }

    }

    /**
     *  Returns the query string (the portion after the question mark).
     *
     *  @param request The HTTP request to parse.
     *  @return The query string.  If the query string is null,
     *          returns an empty string.
     *
     *  @since 2.1.3
     */
    public String safeGetQueryString( HttpServletRequest request )
    {
        if (request == null)
        {
            return "";
        }

        try
        {
            String res = request.getQueryString();
            if( res != null )
            {
                res = new String(res.getBytes("ISO-8859-1"),
                                 getContentEncoding() );

                //
                // Ensure that the 'page=xyz' attribute is removed
                // FIXME: Is it really the mandate of this routine to
                //        do that?
                //
                int pos1 = res.indexOf("page=");
                if (pos1 >= 0)
                {
                    String tmpRes = res.substring(0, pos1);
                    int pos2 = res.indexOf("&",pos1) + 1;
                    if ( (pos2 > 0) && (pos2 < res.length()) )
                    {
                        tmpRes = tmpRes + res.substring(pos2);
                    }
                    res = tmpRes;
                }
            }

            return res;
        }
        catch( UnsupportedEncodingException e )
        {
            log.error( "Unsupported encoding", e );
            return "";
        }
    }

    /**
     *  Returns an URL to some other Wiki that we know.
     *
     *  @param  wikiName The name of the other wiki.
     *  @return null, if no such reference was found.
     */
    public String getInterWikiURL( String wikiName )
    {
        return TextUtil.getStringProperty(m_properties,PROP_INTERWIKIREF+wikiName,null);
    }

    /**
     *  Returns an unordered list of all supported InterWiki links.
     *
     *  @return An unordered list of Strings.
     */
    @SuppressWarnings("unchecked")
    public List<String> getAllInterWikiLinks()
    {
        List<String> links = new ArrayList<String>();

        for( Enumeration e = m_properties.propertyNames(); e.hasMoreElements(); )
        {
            String prop = (String) e.nextElement();

            if( prop.startsWith( PROP_INTERWIKIREF ) )
            {
                links.add( prop.substring( prop.lastIndexOf(".")+1 ) );
            }
        }

        return links;
    }

    /**
     *  Returns an unordered  list of all image types that get inlined.
     *
     *  @return An unordered list of Strings with a regexp pattern.
     */

    public List<String> getAllInlinedImagePatterns()
    {
        return JSPWikiMarkupParser.getImagePatterns( this );
    }

    /**
     *  <p>If the page is a special page, then returns a direct URI
     *  to that page.  Otherwise returns <code>null</code>.
     *  This method delegates requests to
     *  {@link org.apache.wiki.content.resolver.SpecialPageNameResolver#getSpecialPageURI}.
     *  </p>
     *  <p>
     *  Special pages are defined in jspwiki.properties using the jspwiki.specialPage
     *  setting.  They're typically used to give Wiki page names to e.g. custom JSP
     *  pages.
     *  </p>
     *
     *  @param original The page to check
     *  @return A reference to the page, or null, if there's no special page.
     */
    public URI getSpecialPageReference( String original )
    {
        URI uri = m_specialPageResolver.getSpecialPageURI( original );
        return uri;
    }

    /**
     *  Returns the name of the application.
     *
     *  @return A string describing the name of this application.
     */

    // FIXME: Should use servlet context as a default instead of a constant.
    public String getApplicationName()
    {
        String appName = TextUtil.getStringProperty(m_properties,PROP_APPNAME,Release.APPNAME);

        return MarkupParser.cleanLink( appName );
    }

    /**
     * Beautifies the wiki path. Delegates to {@link #beautifyTitle(String)}, except that
     * any paths that denote the default space ({@link ContentManager#DEFAULT_SPACE}
     * have their space prefixes omitted.
     * @param path the path to beautify
     * @return the result
     */
    public String beautifyTitle( WikiPath path )
    {
        if ( ContentManager.DEFAULT_SPACE.equals( path.getSpace() ) )
        {
            return beautifyTitle( path.getPath() );
        }
        return beautifyTitle( path.toString() );
    }

    /**
     *  Beautifies the title of the page by appending spaces in suitable
     *  places, if the user has so decreed in the properties when constructing
     *  this WikiEngine.  However, attachment names are only beautified by
     *  the name.
     *
     *  @param title The title to beautify
     *  @return A beautified title (or, if beautification is off,
     *          returns the title without modification)
     *  @since 1.7.11
     */
    public String beautifyTitle( String title )
    {
        if( m_beautifyTitle )
        {
            try
            {
                Attachment att;
                try
                {
                    att = m_attachmentManager.getAttachmentInfo(title);
                }
                catch( PageNotFoundException e )
                {
                    return TextUtil.beautifyString( title );
                }

                try
                {
                    String parent = TextUtil.beautifyString( att.getParent().getName() );
                    return parent + "/" + att.getFileName();
                }
                catch( PageNotFoundException e )
                {
                    return title;
                }
            }
            catch( ProviderException e )
            {
                return title;
            }
        }

        return title;
    }

    /**
     *  Beautifies the title of the page by appending non-breaking spaces
     *  in suitable places.  This is really suitable only for HTML output,
     *  as it uses the &amp;nbsp; -character.
     *
     *  @param title The title to beautify
     *  @return A beautified title.
     *  @since 2.1.127
     */
    public String beautifyTitleNoBreak( String title )
    {
        if( m_beautifyTitle )
        {
            return TextUtil.beautifyString( title, "&nbsp;" );
        }

        return title;
    }

    /**
     *  Returns true, if the requested page (or an alias) exists.  Will consider
     *  any version as existing.  Will also consider attachments.
     *
     *  @param page WikiName of the page.
     *  @return true, if page (or attachment) exists
     *  @see #pageExists(String, int)
     */
    public boolean pageExists( String page )
    {
        try
        {
            return pageExists( page, WikiProvider.LATEST_VERSION ); 
        }
        catch ( ProviderException e )
        {
            return false;
        }
    }

    /**
     *  Returns true, if the requested page (or an alias) exists with the
     *  requested version. A page "exists" if it has been
     *  previously saved to disk.
     *
     *  @param page Page name
     *  @param version Page version
     *  @return True, if page (or alias, or attachment) exists.
     *  @throws ProviderException If the provider fails
     */
    public boolean pageExists( String page, int version )
        throws ProviderException
    {
        // Resolve the page path
        WikiPath path = WikiPath.valueOf( page );
        WikiPath finalPath = getFinalPageName( path );
        finalPath = finalPath == null ? path : finalPath;

        // Delegate to ContentManager
        return m_contentManager.pageExists( finalPath, version );
    }

    /**
     *  Returns true, if the requested page (or an alias) exists, with the
     *  specified version in the WikiPage. A page "exists" if it has been
     *  previously saved to disk.
     *
     *  @param page A WikiPage object describing the name and version.
     *  @return true, if the page (or alias, or attachment) exists.
     *  @throws ProviderException If something goes badly wrong.
     *  @since 2.0
     *  @see #pageExists(String, int)
     */
    public boolean pageExists( WikiPage page )
        throws ProviderException
    {
        return pageExists( page.getPath().toString(), WikiProvider.LATEST_VERSION );
    }


    /**
     *  Turns a WikiName into something that can be
     *  called through using an URL, encoded in UTF-8.
     *
     *  @since 1.4.1
     *  @param pagename A name.  Can be actually any string.
     *  @return A properly encoded name.
     *  @see #decodeName(String)
     */
    public String encodeName( String pagename )
    {
        try
        {
            return URLEncoder.encode( pagename, "UTF-8" );
        }
        catch( UnsupportedEncodingException e )
        {
            throw new InternalWikiException("UTF-8 not a supported encoding!?!  Your platform is b0rked.");
        }
    }

    /**
     *  Decodes a UTF-8 URL-encoded request back to regular life.
     *
     *  @param pagerequest The UTF-8 URLencoded string to decode
     *  @return A decoded string.
     *  @see #encodeName(String)
     */
    public String decodeName( String pagerequest )
    {
        try
        {
            return URLDecoder.decode( pagerequest, "UTF-8" );
        }
        catch( UnsupportedEncodingException e )
        {
            throw new InternalWikiException("UTF-8 not a supported encoding!?!  Your platform is b0rked.");
        }
    }

    /**
     *  Returns the IANA name of the character set encoding we're
     *  supposed to be using right now.
     *
     *  @since 1.5.3
     *  @return Always returns {@code UTF-8}
     */
    public String getContentEncoding()
    {
        return "UTF-8";
    }

    /**
     * Returns the {@link org.apache.wiki.workflow.WorkflowManager} associated with this
     * WikiEngine. If the WIkiEngine has not been initialized, this method will return
     * <code>null</code>.
     * @return the task queue
     */
    public WorkflowManager getWorkflowManager()
    {
        return m_workflowMgr;
    }

    /**
     *  Returns the un-HTMLized text of the latest version of a page.
     *  This method also replaces the &lt; and &amp; -characters with
     *  their respective HTML entities, thus making it suitable
     *  for inclusion on an HTML page.  If you want to have the
     *  page text without any conversions, use getPureText().
     *
     *  @param page WikiName of the page to fetch.
     *  @return WikiText.
     */
    public String getText( String page )
    {
        return getText( page, WikiProvider.LATEST_VERSION );
    }

    /**
     *  Returns the un-HTMLized text of the given version of a page.
     *  This method also replaces the &lt; and &amp; -characters with
     *  their respective HTML entities, thus making it suitable
     *  for inclusion on an HTML page.  If you want to have the
     *  page text without any conversions, use getPureText().
     *
     *
     * @param page WikiName of the page to fetch
     * @param version  Version of the page to fetch
     * @return WikiText.
     */
    public String getText( String page, int version )
    {
        String result = getPureText( page, version );

        //
        //  Replace ampersand first, or else all quotes and stuff
        //  get replaced as well with &quot; etc.
        //
        /*
        result = TextUtil.replaceString( result, "&", "&amp;" );
        */

        result = TextUtil.replaceEntities( result );

        return result;
    }

    /**
     *  Returns the un-HTMLized text of the given version of a page in
     *  the given context.  USE THIS METHOD if you don't know what
     *  doing.
     *  <p>
     *  This method also replaces the &lt; and &amp; -characters with
     *  their respective HTML entities, thus making it suitable
     *  for inclusion on an HTML page.  If you want to have the
     *  page text without any conversions, use getPureText().
     *
     *  @since 1.9.15.
     *  @param context The WikiContext
     *  @param page    A page reference (not an attachment)
     *  @return The page content as HTMLized String.
     *  @see   #getPureText(WikiPage)
     */
    public String getText( WikiContext context, WikiPage page )
    {
        return getText( page.getName(), page.getVersion() );
    }


    /**
     *  Returns the pure text of a page, no conversions.  Use this
     *  if you are writing something that depends on the parsing
     *  of the page.  Note that you should always check for page
     *  existence through pageExists() before attempting to fetch
     *  the page contents.
     *
     *  @param page    The name of the page to fetch.
     *  @param version If WikiPageProvider.LATEST_VERSION, then uses the
     *  latest version.
     *  @return The page contents.  If the page does not exist,
     *          returns an empty string.
     *  @deprecated Please use {@link WikiPage#getContentAsString()}
     */
    // FIXME: Should throw an exception on unknown page/version?
    public String getPureText( String page, int version )
    {
        String result = null;

        try
        {
            WikiPage p = m_contentManager.getPage( WikiPath.valueOf( page ), version );
            result = p.getContentAsString();
        }
        catch( PageNotFoundException e )
        {
        }
        catch( ProviderException e )
        {
        }
        finally
        {
            if( result == null )
                result = "";
        }

        return result;
    }

    /**
     *  Returns the pure text of a page, no conversions.  Use this
     *  if you are writing something that depends on the parsing
     *  the page. Note that you should always check for page
     *  existence through pageExists() before attempting to fetch
     *  the page contents.
     *
     *  @param page A handle to the WikiPage
     *  @return String of WikiText.
     *  @throws ProviderException If the page content cannot be read. 
     *  @throws PageNotFoundException If the page cannot be located
     *  @since 2.1.13.
     *  @deprecated Please use {@link WikiPage#getContentAsString()}
     */
    public String getPureText( WikiPage page ) throws PageNotFoundException, ProviderException
    {
        return page.getContentAsString();
    }

    /**
     *  Returns the converted HTML of the page using a different
     *  context than the default context.
     *
     *  @param  context A WikiContext in which you wish to render this page in.
     *  @param  page WikiPage reference.
     *  @return HTML-rendered version of the page.
     */

    public String getHTML( WikiContext context, WikiPage page )
    {
        String pagedata = null;

        pagedata = getPureText( page.getName(), page.getVersion() );

        String res = textToHTML( context, pagedata );

        return res;
    }

    /**
     *  Returns the converted HTML of the page.
     *
     *  @param page WikiName of the page to convert.
     *  @return HTML-rendered version of the page.
     * @throws ProviderException 
     * @throws PageNotFoundException 
     */
    public String getHTML( String page ) 
        throws PageNotFoundException, ProviderException
    {
        return getHTML( page, WikiProvider.LATEST_VERSION );
    }

    /**
     *  Returns the converted HTML of the page's specific version.
     *  The version must be a positive integer, otherwise the current
     *  version is returned.
     *
     *  @param pagename WikiName of the page to convert.
     *  @param version Version number to fetch
     *  @return HTML-rendered page text.
     * @throws ProviderException 
     * @throws PageNotFoundException 
     */
    public String getHTML( String pagename, int version ) 
        throws PageNotFoundException, ProviderException
    {
        WikiPage page = getPage( pagename, version );

        WikiContext context = m_contextFactory.newViewContext( page );

        String res = getHTML( context, page );

        return res;
    }

    /**
     *  Converts raw page data to HTML.
     *
     *  @param pagedata Raw page data to convert to HTML
     *  @param context  The WikiContext in which the page is to be rendered
     *  @return Rendered page text
     */
    public String textToHTML( WikiContext context, String pagedata )
    {
        String result = "";

        boolean runFilters = "true".equals(m_variableManager.getValue(context,PROP_RUNFILTERS,"true"));

        StopWatch sw = new StopWatch();
        sw.start();
        try
        {
            if( runFilters )
                pagedata = m_filterManager.doPreTranslateFiltering( context, pagedata );

            result = m_renderingManager.getHTML( context, pagedata );

            if( runFilters )
                result = m_filterManager.doPostTranslateFiltering( context, result );
        }
        catch( FilterException e )
        {
            // FIXME: Don't yet know what to do
        }
        sw.stop();
        if( log.isDebugEnabled() )
            log.debug("Page "+context.getRealPage().getName()+" rendered, took "+sw );

        return result;
    }

    /**
     * Restarts the WikiEngine.
     */
    public void restart() throws WikiException
    {
        // Shut down the wiki
        shutdown();
        
        // Restart logging
        // FIXME: use introspection instead
        LogManager.resetConfiguration();
        ClassLoader cl = this.getClass().getClassLoader();
        URL log4jprops = cl.getResource( "log4j.properties" );
        if (log4jprops != null) {
            PropertyConfigurator.configure(log4jprops);
        }
        
        // Restart the wiki
        m_properties = PropertyReader.loadWebAppProps( m_servletContext );
        initialize( m_properties );
    }

    /**
     * Protected method that signals that the WikiEngine will be
     * shut down by the servlet container. It is called by
     * {@link SessionMonitor#contextDestroyed(javax.servlet.ServletContextEvent)}.
     * When this method is called, it fires a "shutdown"
     * WikiEngineEvent to all registered listeners.
     */
    public void shutdown()
    {
        fireEvent( WikiEngineEvent.SHUTDOWN );
        if( m_filterManager != null ) m_filterManager.destroy();
        LoggerFactory.unRegisterAllLoggerMBeans();
        if( m_contentManager != null ) m_contentManager.shutdown();
        WikiBackgroundThread.unregisterTimer( this );
    }

    /**
     *  Just convert WikiText to HTML.
     *
     *  @param context The WikiContext in which to do the conversion
     *  @param pagedata The data to render
     *  @param localLinkHook Is called whenever a wiki link is found
     *  @param extLinkHook   Is called whenever an external link is found
     *
     *  @return HTML-rendered page text.
     */

    public String textToHTML( WikiContext context,
                              String pagedata,
                              StringTransmutator localLinkHook,
                              StringTransmutator extLinkHook )
    {
        return textToHTML( context, pagedata, localLinkHook, extLinkHook, null, true, false );
    }

    /**
     *  Just convert WikiText to HTML.
     *
     *  @param context The WikiContext in which to do the conversion
     *  @param pagedata The data to render
     *  @param localLinkHook Is called whenever a wiki link is found
     *  @param extLinkHook   Is called whenever an external link is found
     *  @param attLinkHook   Is called whenever an attachment link is found
     *  @return HTML-rendered page text.
     */

    public String textToHTML( WikiContext context,
                              String pagedata,
                              StringTransmutator localLinkHook,
                              StringTransmutator extLinkHook,
                              StringTransmutator attLinkHook )
    {
        return textToHTML( context, pagedata, localLinkHook, extLinkHook, attLinkHook, true, false );
    }

    /**
     *  Helper method for doing the HTML translation.
     *
     *  @param context The WikiContext in which to do the conversion
     *  @param pagedata The data to render
     *  @param localLinkHook Is called whenever a wiki link is found
     *  @param extLinkHook   Is called whenever an external link is found
     *  @param parseAccessRules Parse the access rules if we encounter them
     *  @param justParse Just parses the pagedata, does not actually render.  In this case,
     *                   this methods an empty string.
     *  @return HTML-rendered page text.

     */
    private String textToHTML( WikiContext context,
                               String pagedata,
                               StringTransmutator localLinkHook,
                               StringTransmutator extLinkHook,
                               StringTransmutator attLinkHook,
                               boolean            parseAccessRules,
                               boolean            justParse )
    {
        String result = "";

        if( pagedata == null )
        {
            log.error("NULL pagedata to textToHTML()");
            return null;
        }

        boolean runFilters = "true".equals(m_variableManager.getValue(context,PROP_RUNFILTERS,"true"));

        try
        {
            StopWatch sw = new StopWatch();
            sw.start();

            if( runFilters )
                pagedata = m_filterManager.doPreTranslateFiltering( context, pagedata );

            MarkupParser mp = m_renderingManager.getParser( context, pagedata );
            mp.addLocalLinkHook( localLinkHook );
            mp.addExternalLinkHook( extLinkHook );
            mp.addAttachmentLinkHook( attLinkHook );

            if( !parseAccessRules ) mp.disableAccessRules();

            WikiDocument doc = mp.parse();

            //
            //  In some cases it's better just to parse, not to render
            //
            if( !justParse )
            {
                result = m_renderingManager.getHTML( context, doc );

                if( runFilters )
                    result = m_filterManager.doPostTranslateFiltering( context, result );
            }

            sw.stop();

            if( log.isDebugEnabled() )
                log.debug("Page "+context.getRealPage().getName()+" rendered, took "+sw );
        }
        catch( IOException e )
        {
            log.error("Failed to scan page data: ", e);
        }
        catch( FilterException e )
        {
            // FIXME: Don't yet know what to do
        }

        return result;
    }

    /**
     *  Writes the WikiText of a page into the
     *  page repository. If the <code>jspwiki.properties</code> file contains
     *  the property <code>jspwiki.approver.workflow.saveWikiPage</code> and
     *  its value resolves to a valid user, {@link org.apache.wiki.auth.authorize.Group}
     *  or {@link org.apache.wiki.auth.authorize.Role}, this method will
     *  place a {@link org.apache.wiki.workflow.Decision} in the approver's
     *  workflow inbox and throw a {@link org.apache.wiki.workflow.DecisionRequiredException}.
     *  If the submitting user is authenticated and the page save is rejected,
     *  a notification will be placed in the user's decision queue.
     *
     *  @since 2.1.28
     *  @param context The current WikiContext
     *  @param text    The Wiki markup for the page.
     *  @throws WikiException if the save operation encounters an error during the
     *  save operation. If the page-save operation requires approval, the exception will
     *  be of type {@link org.apache.wiki.workflow.DecisionRequiredException}. Individual
     *  PageFilters, such as the {@link org.apache.wiki.filters.CreoleFilter} may also
     *  throw a {@link org.apache.wiki.filters.RedirectException}.
     *  @throws PageNotFoundException 
     */
    public void saveText( WikiContext context, String text )
        throws WikiException
    {
        // Check if page data actually changed; bail if not
        WikiPage page = context.getPage();
        String oldText = page.getContentAsString();
        String proposedText = TextUtil.normalizePostData( text );
        if ( oldText != null && oldText.equals( proposedText ) )
        {
            return;
        }

        if( oldText == null ) oldText = ""; // FIXME: This is not pretty.
        
        // Check if creation of empty pages is allowed; bail if not
        boolean allowEmpty = TextUtil.getBooleanProperty( m_properties, 
                                                          PROP_ALLOW_CREATION_OF_EMPTY_PAGES, 
                                                          false );
        if ( !allowEmpty && !pageExists( page ) && text.trim().equals( "" ) )  
        {
            return;
        }
        
        // Create approval workflow for page save; add the diffed, proposed
        // and old text versions as Facts for the approver (if approval is required)
        // If submitter is authenticated, any reject messages will appear in his/her workflow inbox.
        WorkflowBuilder builder = WorkflowBuilder.getBuilder( this );
        Principal submitter = context.getCurrentUser();
        Task prepTask = new ContentManager.PreSaveWikiPageTask( context, proposedText );
        Task completionTask = new ContentManager.SaveWikiPageTask();
        String diffText = m_differenceManager.makeDiff( context, oldText, proposedText );
        boolean isAuthenticated = context.getWikiSession().isAuthenticated();
        Fact[] facts = new Fact[5];
        facts[0] = new Fact( ContentManager.FACT_PAGE_NAME, page.getName() );
        facts[1] = new Fact( ContentManager.FACT_DIFF_TEXT, diffText );
        facts[2] = new Fact( ContentManager.FACT_PROPOSED_TEXT, proposedText );
        facts[3] = new Fact( ContentManager.FACT_CURRENT_TEXT, oldText);
        facts[4] = new Fact( ContentManager.FACT_IS_AUTHENTICATED, Boolean.valueOf( isAuthenticated ) );
        String rejectKey = isAuthenticated ? ContentManager.SAVE_REJECT_MESSAGE_KEY : null;
        Workflow workflow = builder.buildApprovalWorkflow( submitter,
                                                           ContentManager.SAVE_APPROVER,
                                                           prepTask,
                                                           ContentManager.SAVE_DECISION_MESSAGE_KEY,
                                                           facts,
                                                           completionTask,
                                                           rejectKey );
        m_workflowMgr.start(workflow);

        // Let callers know if the page-save requires approval
        if ( workflow.getCurrentStep() instanceof Decision )
        {
            throw new DecisionRequiredException( "The page contents must be approved before they become active." );
        }
    }

    /**
     *  Returns the number of pages in this Wiki
     *  @return The total number of pages.
     */
    public int getPageCount()
    {
        try
        {
            return m_contentManager.getTotalPageCount(null);
        }
        catch( ProviderException e )
        {
            return -1;
        }
    }

    /**
     *  Returns the provider name.
     *  @return The full class name of the current page provider.
     */

    public String getCurrentProvider()
    {
        return m_contentManager.getProvider();
    }

    /**
     *  Return information about current provider.  This method just calls
     *  the corresponding PageManager method, which in turn calls the
     *  provider method.
     *
     *  @return A textual description of the current provider.
     *  @since 1.6.4
     */
    public String getCurrentProviderInfo()
    {
        return m_contentManager.getProviderDescription();
    }

    /**
     *  Returns a list of WikiPages, sorted in time
     *  order of last change (i.e. first object is the most
     *  recently changed).  This method also includes attachments.
     *
     *  @param space The wiki space for which you want to get
     *  recent changes
     *  @return a list of wiki pages, sorted
     */
    // FIXME: Should really get a Date object and do proper comparisons.
    //        This is terribly wasteful.
    public List<WikiPage> getRecentChanges(String space)
    {
        try
        {
            List<WikiPage>   pages = m_contentManager.getAllPages(space);
            Collections.sort( pages, PAGE_TIME_COMPARATOR );
            return pages;
        }
        catch( ProviderException e )
        {
            log.error( "Unable to fetch all pages: ",e);
            return null;
        }
    }

    /**
     *  <p>Searches for objects using the WikiEngine's configured
     *  search provider. How the query is processed depends on the
     *  on the search provider; each has its own search syntax .
     *  The returned list is sorted in order of relevance (i.e., highest
     *  quality hits first).</p>
     *
     *  @param query the query string
     *  @return A list of SearchResult objects
     *  @throws ProviderException If the searching failed
     *  @throws IOException If the searching failed
     */
    //
    // FIXME: Should also have attributes attached.
    //
    public List<SearchResult> findPages( String query )
        throws ProviderException, IOException
    {
        List<SearchResult> results = m_searchManager.findPages( query );
        return results;
    }

    /**
     *  Creates a new WikiPage object without saving it to the repository.
     *  To save the page, call {@link WikiPage#save()}.
     *  
     *  @param name the WikiName of the object to create
     *  @return a new WikiPage object
     *  @throws PageAlreadyExistsException if the page already exists in the repository.
     *  @throws ProviderException if the backend fails
     *  @since 3.0
     */
    public WikiPage createPage( WikiPath name ) throws PageAlreadyExistsException, ProviderException
    {
        return m_contentManager.addPage( name, ContentManager.JSPWIKI_CONTENT_TYPE );
    }
    
    /**
     *  A shortcut for createPage( WikiName.valueOf(fqn) );
     *  
     *  @param fqn the fully qualified name of a wikipage
     *  @return a new page
     *  @throws PageAlreadyExistsException if the page already exists in the repository
     *  @throws ProviderException if the backend fails
     *  @since 3.0
     */
    public WikiPage createPage( String fqn ) throws PageAlreadyExistsException, ProviderException
    {
        return createPage( WikiPath.valueOf( fqn ) );
    }
    
    /**
     *  Finds the corresponding WikiPage object based on the page name. 
     *
     *  @param pagereq The name of the page to look for.
     *  @return A WikiPage object, or null, if the page by the name could not be found.
     *  @throws ProviderException 
     *  @throws PageNotFoundException 
     */

    public WikiPage getPage( String pagereq ) 
        throws PageNotFoundException, ProviderException
    {
        return getPage( WikiPath.valueOf( pagereq ) );
    }

    public WikiPage getPage( WikiPath name )
        throws PageNotFoundException, ProviderException
    {            
        return m_contentManager.getPage( name );
    }
    
    /**
     *  Finds the corresponding WikiPage object base on the page name and version.
     *
     *  @param pagereq The name of the page to look for.
     *  @param version The version number to look for.  May be WikiProvider.LATEST_VERSION,
     *  in which case it will look for the latest version (and this method then becomes
     *  the equivalent of getPage(String).
     *
     *  @return A WikiPage object
     *  @throws ProviderException 
     *  @throws PageNotFoundException If the page cannot be found. 
     *  @since 1.6.7.
     */

    public WikiPage getPage( String pagereq, int version ) 
        throws PageNotFoundException, ProviderException
    {
        WikiPage p = m_contentManager.getPage( WikiPath.valueOf( pagereq ), version );
        
        return p;
    }


    /**
     *  Returns a list of WikiPages containing the
     *  version history of a page.
     *
     *  @param page Name of the page to look for
     *  @return an ordered list of WikiPages, each corresponding to a different
     *          revision of the page.
     * @throws ProviderException 
     * @throws PageNotFoundException 
     */
    public List<WikiPage> getVersionHistory( String page ) 
        throws PageNotFoundException, ProviderException
    {
        return m_contentManager.getVersionHistory( WikiPath.valueOf( page ) );
    }

    /**
     *  Returns a diff of two versions of a page.
     *  <p>
     *  Note that the API was changed in 2.6 to provide a WikiContext object!
     *
     *  @param context The WikiContext of the page you wish to get a diff from
     *  @param version1 Version number of the old page.  If
     *         WikiPageProvider.LATEST_VERSION (-1), then uses current page.
     *  @param version2 Version number of the new page.  If
     *         WikiPageProvider.LATEST_VERSION (-1), then uses current page.
     *
     *  @return A HTML-ized difference between two pages.  If there is no difference,
     *          returns an empty string.
     */
    public String getDiff( WikiContext context, int version1, int version2 )
    {
        String page = context.getPage().getName();
        String page1 = getPureText( page, version1 );
        String page2 = getPureText( page, version2 );

        // Kludge to make diffs for new pages to work this way.

        if( version1 == WikiProvider.LATEST_VERSION )
        {
            page1 = "";
        }

        String diff  = m_differenceManager.makeDiff( context, page1, page2 );

        return diff;
    }

    /**
     *  Returns this object's ReferenceManager.
     *  @return The current ReferenceManager instance.
     *
     *  @since 1.6.1
     */
    public ReferenceManager getReferenceManager()
    {
        return m_referenceManager;
    }

    /**
     *  Returns the current rendering manager for this wiki application.
     *
     *  @since 2.3.27
     * @return A RenderingManager object.
     */
    public RenderingManager getRenderingManager()
    {
        return m_renderingManager;
    }

    /**
     *  Returns the current plugin manager.
     *  @since 1.6.1
     *  @return The current PluginManager instance
     */

    public PluginManager getPluginManager()
    {
        return m_pluginManager;
    }

    /**
     *  Returns the current variable manager.
     *  @return The current VariableManager.
     */

    public VariableManager getVariableManager()
    {
        return m_variableManager;
    }

    /**
     *  Shortcut to getVariableManager().getValue(). However, this method does not
     *  throw a NoSuchVariableException, but returns null in case the variable does
     *  not exist.
     *
     *  @param context WikiContext to look the variable in
     *  @param name Name of the variable to look for
     *  @return Variable value, or null, if there is no such variable.
     *  @since 2.2
     */
    public String getVariable( WikiContext context, String name )
    {
        try
        {
            return m_variableManager.getValue( context, name );
        }
        catch( NoSuchVariableException e )
        {
            return null;
        }
    }

    /**
     *  Returns the current PageManager which is responsible for storing
     *  and managing WikiPages.
     *
     *  @return The current PageManager instance.
     *  @deprecated
     */
    public PageManager getPageManager()
    {
        return m_pageManager;
    }

    /**
     * Returns the WikiContextFactory for this wiki engine.
     * @return the factory
     */
    public WikiContextFactory getWikiContextFactory()
    {
        return m_contextFactory;
    }

    /**
     *  Returns the current AttachmentManager, which is responsible for
     *  storing and managing attachments.
     *
     *  @since 1.9.31.
     *  @return The current AttachmentManager instance
     *  @deprecated
     */
    public AttachmentManager getAttachmentManager()
    {
        return m_attachmentManager;
    }

    /**
     *  Returns the currently used authorization manager.
     *
     *  @return The current AuthorizationManager instance
     */
    public AuthorizationManager getAuthorizationManager()
    {
        return m_authorizationManager;
    }

    /**
     *  Returns the currently used authentication manager.
     *
     *  @return The current AuthenticationManager instance.
     */
    public AuthenticationManager getAuthenticationManager()
    {
        return m_authenticationManager;
    }

    /**
     *  Returns the manager responsible for the filters.
     *  @since 2.1.88
     *  @return The current FilterManager instance
     */
    public FilterManager getFilterManager()
    {
        return m_filterManager;
    }

    /**
     *  Returns the manager responsible for searching the Wiki.
     *  @since 2.2.21
     *  @return The current SearchManager instance
     */
    public SearchManager getSearchManager()
    {
        return m_searchManager;
    }

    /**
     *  Returns the progress manager we're using
     *  @return A ProgressManager
     *  @since 2.6
     */
    public ProgressManager getProgressManager()
    {
        return m_progressManager;
    }

    /**
     *  <p>Factory method to create a named WikiContext from a supplied HTTP request.
     *  This method is designed to be called <em>only</em> from within JSP scriptlets;
     *  core classes in JSPWiki itself are expected to use either
     *  {@link org.apache.wiki.action.WikiContextFactory#newContext(HttpServletRequest, HttpServletResponse, String)}
     *  or
     *  {@link org.apache.wiki.action.WikiContextFactory#newViewContext(HttpServletRequest, HttpServletResponse, WikiPage)}
     *  instead.</p>
     *  <p>Note to JSP authors: JSPs that are bound to a {@link org.apache.wiki.action.WikiActionBean}
     *  and/or contain a <code>&lt;stripes:useActionBean&gt;</code> tag will automatically
     *  cause a WikiActionBean and {@link org.apache.wiki.ui.stripes.WikiActionBeanContext}
     *  (a WikiContext object) to be created and bound in request scope. This WikiContext
     *  instance will already exist by the time this method is called. So that the Stripes-provided
     *  WikiContext is not overwritten inadvertently, this method uses the following algorithm
     *  to safely return the WikiContext:</p>
     *  <ul>
     *  <li>If the request scope is non-null, it is examined for the presence of an ActionBean bound using the
     *  key {@link org.apache.wiki.ui.stripes.WikiInterceptor#ATTR_ACTIONBEAN})</li>
     *  <li>If an ActionBean is found, its WikiActionBeanContext is returned
     *  (via {@link org.apache.wiki.action.WikiActionBean#getContext()}</li>
     *  <li>Otherwise, a new WikiContext is created by delegating to
     *  {@link org.apache.wiki.action.WikiContextFactory#newContext(HttpServletRequest, HttpServletResponse, String)}</li>
     *  </ul>
     *  <p>JSPs that are bound to WikiActionBeans can also recover the Stripes-stashed
     *  WikiActionBean by calling
     *  {@link org.apache.wiki.action.WikiContextFactory#findContext(javax.servlet.jsp.PageContext)}
     *  instead of this method.</p>
     *  @param request the HTTP request
     *  @param requestContext the named context to use
     *  @return the WikiActionBeanContext previously stashed by Stripes, or a new WikiContext if one was not found
     *  @see org.apache.wiki.action.WikiContextFactory#newContext(HttpServletRequest, HttpServletResponse, String)
     *  @see org.apache.wiki.action.WikiContextFactory#newViewContext(HttpServletRequest, HttpServletResponse, WikiPage)
     *  @since 2.1.15.
     *  @deprecated this method is retained for backwards compatibility with JSPs
     */
    // FIXME: We need to have a version which takes a fixed page
    //        name as well, or check it elsewhere.
    public WikiContext createContext( HttpServletRequest request,
                                      String requestContext )
    {
        if( !m_isConfigured )
        {
            throw new InternalWikiException("WikiEngine has not been properly started.  It is likely that the configuration is faulty.  Please check all logs for the possible reason.");
        }
        
        // Recycle/return existing WikiActionBeanContext if Stripes put ActionBean in request scope already
        WikiActionBeanContext context;
        if ( request != null )
        {
            try
            {
                WikiActionBean actionBean = WikiInterceptor.findActionBean( request );
                context = actionBean.getContext();
                context.setRequestContext( requestContext );
            }
            catch ( IllegalStateException e )
            {
                // No actionBean previously stashed -- no worries. We will just create a fresh one
            }
        }
        
        // Build the wiki context... dummy reply and response objects will be added by WikiContextFactory
        try
        {
            context = m_contextFactory.newContext( request, (HttpServletResponse)null, requestContext );
            
            // Stash WikiEngine as a request attribute (can be
            // used later as ${wikiEngine} in EL markup)
            request.setAttribute( WikiContextFactory.ATTR_WIKIENGINE, this );

            // Stash the WikiSession as a request attribute
            WikiSession wikiSession = SessionMonitor.getInstance( this ).find( request.getSession() );
            request.setAttribute( WikiContextFactory.ATTR_WIKISESSION, wikiSession );
            
            // Stash the action bean/wiki context, and return it!
            WikiContextFactory.saveContext( request, context );
            return context;
        }
        catch ( WikiException e )
        {
            log.error( "Could not create context: " + e.getMessage() );
            return null;
        }
    }

    /**
     *  Deletes a page or an attachment completely, including all versions.  If the page
     *  does not exist, does nothing.
     *
     * @param pageName The name of the page.
     * @throws ProviderException If something goes wrong.
     */
    public void deletePage( String pageName )
        throws ProviderException
    {
        WikiPage p;

        try
        {
            p = getPage( pageName );
            
            m_contentManager.deletePage( p );
        }
        catch(PageNotFoundException e) {}
    }

    /**
     *  Deletes a specific version of a page or an attachment.
     *
     *  @param page The page object.
     *  @throws ProviderException If something goes wrong.
     */
    public void deleteVersion( WikiPage page )
        throws ProviderException
    {
        m_contentManager.deleteVersion( page );
    }

    /**
     *  Returns the URL of the global RSS file.  May be null, if the
     *  RSS file generation is not operational.
     *  @since 1.7.10
     *  @return The global RSS url
     */
    public String getGlobalRSSURL()
    {
        if( m_rssGenerator != null && m_rssGenerator.isEnabled() )
        {
            return getBaseURL()+m_rssFile;
        }

        return null;
    }

    /**
     *  Returns the root path.  The root path is where the WikiEngine is
     *  located in the file system.
     *
     *  @since 2.2
     *  @return A path to where the Wiki is installed in the local filesystem.
     */
    public String getRootPath()
    {
        return m_rootPath;
    }

    /**
     * @since 2.2.6
     * @return the URL constructor
     */
    public StripesURLConstructor getURLConstructor()
    {
        return m_urlConstructor;
    }

    /**
     * Returns the RSSGenerator. If the property <code>jspwiki.rss.generate</code>
     * has not been set to <code>true</code>, this method will return <code>null</code>,
     * <em>and callers should check for this value.</em>
     * @since 2.1.165
     * @return the RSS generator
     */
    public RSSGenerator getRSSGenerator()
    {
        return m_rssGenerator;
    }

    /**
     * Renames, or moves, a wiki page. Can also alter referring wiki
     * links to point to the renamed page.
     *
     * @param context           The context during which this rename takes
     *                          place.
     * @param renameFrom        Name of the source page.
     * @param renameTo          Name of the destination page.
     * @param changeReferrers   In previous versions of JSPWiki, this parameter
     *                          indicated whether any referring links to the
     *                          renamed page should be changed also. In 3.0,
     *                          this parameter is ignored, and always assumed to
     *                          be {@code true}
     *
     * @return The name of the page that the source was renamed to.
     *
     * @throws WikiException    In the case of an error, such as the destination
     *                          page already existing.
     */
    public String renamePage( WikiContext context,
                              String renameFrom,
                              String renameTo,
                              boolean changeReferrers)
        throws WikiException
    {
        return m_contentManager.renamePage(context, renameFrom, renameTo );
    }

    /**
     *  Returns the PageRenamer employed by this WikiEngine.
     *  @since 2.5.141
     *  @return The current PageRenamer instance.
     */
    public PageRenamer getPageRenamer()
    {
        return m_pageRenamer;
    }

    /**
     *  Returns the UserManager employed by this WikiEngine.
     *  @since 2.3
     *  @return The current UserManager instance.
     */
    public UserManager getUserManager()
    {
        return m_userManager;
    }

    /**
     *  Returns the GroupManager employed by this WikiEngine.
     *  @since 2.3
     *  @return The current GroupManager instance
     */
    public GroupManager getGroupManager()
    {
        return m_groupManager;
    }

    /**
     *  Returns the current AdminBeanManager.
     *
     *  @return The current AdminBeanManager
     *  @since  2.6
     */
    public AdminBeanManager getAdminBeanManager()
    {
        return m_adminBeanManager;
    }

    /**
     *  Returns the AclManager employed by this WikiEngine.
     *  The AclManager is lazily initialized.
     *  <p>
     *  The AclManager implementing class may be set by the
     *  System property {@link #PROP_ACL_MANAGER_IMPL}.
     *  </p>
     *
     * @since 2.3
     * @return The current AclManager.
     */
    public AclManager getAclManager()
    {
        if( m_aclManager == null )
        {
            try
            {
                String s = m_properties.getProperty( PROP_ACL_MANAGER_IMPL,
                                                     DefaultAclManager.class.getName() );
                m_aclManager = (AclManager)ClassUtil.getMappedObject(s); // TODO: I am not sure whether this is the right call
                m_aclManager.initialize( this, m_properties );
            }
            catch ( WikiException we )
            {
                log.error( "unable to instantiate class for AclManager: " + we.getMessage() );
                throw new InternalWikiException("Cannot instantiate AclManager, please check logs.");
            }
        }
        return m_aclManager;
    }

    /**
     *  Returns the Content Manager in use.
     *  
     *  @return The Content Manager.
     */
    public ContentManager getContentManager()
    {
        return m_contentManager;
    }
    

    /**
     *  Returns the DifferenceManager so that texts can be compared.
     *  @return the difference manager
     */
    public DifferenceManager getDifferenceManager()
    {
        return m_differenceManager;
    }

    /**
     *  Returns the current EditorManager instance.
     *
     *  @return The current EditorManager.
     */
    public EditorManager getEditorManager()
    {
        return m_editorManager;
    }

    /**
     *  Returns the current i18n manager.
     *
     *  @return The current Intertan... Interante... Internatatializ... Whatever.
     */
    public InternationalizationManager getInternationalizationManager()
    {
        return m_internationalizationManager;
    }

    /**
     * Registers a WikiEventListener with this instance.
     * @param listener the event listener
     */
    public final synchronized void addWikiEventListener( WikiEventListener listener )
    {
        WikiEventManager.addWikiEventListener( this, listener );
    }

    /**
     * Un-registers a WikiEventListener with this instance.
     * @param listener the event listener
     */
    public final synchronized void removeWikiEventListener( WikiEventListener listener )
    {
        WikiEventManager.removeWikiEventListener( this, listener );
    }

    /**
     * Fires a WikiEngineEvent to all registered listeners.
     * @param type  the event type
     */
    protected final void fireEvent( int type )
    {
        if ( WikiEventManager.isListening(this) )
        {
            WikiEventManager.fireEvent(this,new WikiEngineEvent(this,type));
        }
    }

    /**
     * Adds an attribute to the engine for the duration of this engine.  The
     * value is not persisted.
     *
     * @since 2.4.91
     * @param key the attribute name
     * @param value the value
     */
    public void setAttribute( String key, Object value )
    {
        m_attributes.put( key, value );
    }

    /**
     *  Gets an attribute from the engine.
     *
     *  @param key the attribute name
     *  @return the value
     */
    public Object getAttribute( String key )
    {
        return m_attributes.get( key );
    }

    /**
     *  Removes an attribute.
     *
     *  @param key The key of the attribute to remove.
     *  @return The previous attribute, if it existed.
     */
    public Object removeAttribute( String key )
    {
        return m_attributes.remove( key );
    }

    /**
     *  Returns a WatchDog for current thread.
     *
     *  @return The current thread WatchDog.
     *  @since 2.4.92
     */
    public WatchDog getCurrentWatchDog()
    {
        return WatchDog.getCurrentWatchDog(this);
    }
    
    public void release()
    {
        m_contentManager.release();
    }
    
    /**
     *  Resolves a wiki path, trying all resolution algorithms as specified by the
     *  {@link PageNameResolver} classes. If the path resolves to a different
     *  path, that path is returned. Otherwise, <code>null</code> is returned.
     *  
     *  @param page the page name.
     *  @return The rewritten page name.  May also return null in case there
     *          were problems.
     */
    public final WikiPath getFinalPageName( WikiPath page ) throws ProviderException
    {
        // If the original name resolves, return it
        if ( getContentManager().pageExists( page  ) )
        {
            return page;
        }

        // Otherwise try resolving it
        for( PageNameResolver resolver : m_nameResolvers )
        {
            WikiPath resolvedPath = resolver.resolve( page );
            if ( resolvedPath != null )
            {
                return resolvedPath;
            }
        }
        
        return null;
    }

}
