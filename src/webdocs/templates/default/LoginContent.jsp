<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.action.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/stripes.tld" prefix="stripes" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%
    String postURL = "";
    WikiActionBean ctx = WikiContext.findContext( pageContext );
    AuthenticationManager mgr = ctx.getEngine().getAuthenticationManager();

    if( mgr.isContainerAuthenticated() )
    {
        postURL = "j_security_check";
    }
    else
    {
        postURL = "/Login.action";
    }

    boolean supportsCookieAuthentication = mgr.allowsCookieAuthentication();
%>
<wiki:TabbedSection defaultTab="${param.tab}">

<%-- Login functionality --%>
<wiki:UserCheck status="notauthenticated">
<wiki:Tab id="logincontent" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "login.tab")%>'>
<%--<wiki:Include page='LoginTab.jsp'/>--%>

<stripes:form action="<%=postURL%>"
          id="login"
       class="wikiform"
      method="post" acceptcharset="${wikiEngine.contentEncoding}" >

<div class="center">

  <h3><fmt:message key="login.heading.login"><fmt:param><wiki:Variable var="applicationname" /></fmt:param></fmt:message></h3>

  <div class="formhelp"><fmt:message key="login.help"></fmt:message></div>

  <table>
    <tr>
      <td colspan="2" class="formhelp">
      <div class="error">
        <stripes:errors beanclass="com.ecyrd.jspwiki.action.LoginActionBean"/>
      </div>
      </td>
    </tr>
    <tr>
      <td><label for="j_username"><fmt:message key="login.login"/></label></td>
      <td>
        <stripes:text size="24" name="j_username" id="j_username"><wiki:Variable var="uid" default=""/></stripes:text>
      </td>
    </tr>
    <tr>
      <td><label for="j_password"><fmt:message key="login.password"/></label></td>
      <td><stripes:password size="24" name="j_password" id="j_password" /></td>
    </tr>
    <% if( supportsCookieAuthentication ) { %>
    <tr>
      <td><label for="j_remember"><fmt:message key="login.remember"/></label></td>
      <td><stripes:checkbox name="j_remember" id="j_remember" /></td>
    </tr>
    <% } %>
    <tr>
      <td>&nbsp;</td>
      <td>
        <stripes:submit name="login"><fmt:message key="login.submit.login"/></stripes:submit>
      </td>
    </tr>
    </table>

    <div class="formhelp">
      <fmt:message key="login.lostpw"/>
      <a href="#" onclick="$('menu-lostpassword').fireEvent('click');"
                    title="<fmt:message key='login.lostpw.title'/>" >
        <fmt:message key="login.lostpw.getnew"/>
      </a>
    </div>
    <div class="formhelp">
      <fmt:message key="login.nopassword"/>
      <a href="#" onclick="$('menu-profile').fireEvent('click');"
                    title="<fmt:message key='login.registernow.title'/>" >
        <fmt:message key="login.registernow">
          <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
        </fmt:message>
      </a>
    </div>

</div>
</stripes:form>

</wiki:Tab>

<%-- Lost pasword functionality --%>
<wiki:Tab id="lostpassword" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "login.lostpw.tab")%>'>

<div class="center">
<stripes:form action="/LostPassword.jsp"
          id="lostpw"
       class="wikiform"
      method="post" acceptcharset="${wikiEngine.contentEncoding}" >

  <h3><fmt:message key="login.lostpw.heading" /></h3>

  <c:choose>
  <c:when test="${passwordreset == 'done' }">
      <wiki:Messages div="information" topic="resetpw" prefix="" />
      <p>
        <fmt:message key="login.lostpw.reset.login">
          <fmt:param><a href="<wiki:Link jsp='Login.action' />"><fmt:message key="login.lostpw.reset.clickhere"/></a></fmt:param>
        </fmt:message>
      </p>
  </c:when>
  <c:otherwise>

  <div class="formhelp"><fmt:message key="login.lostpw.help"></fmt:message></div>

  <table>
    <c:if test="${param.tab eq 'lostpassword'}" >
    <tr>
      <td colspan="2" class="formhelp">
        <div class="error">
          <stripes:errors beanclass="com.ecyrd.jspwiki.action.NoneActionBean"/>
        </div>
        <wiki:Messages div="information" topic="resetpwok" />
      </td>
    </tr>
    </c:if>
    <tr>
      <td><label for="name"><fmt:message key="login.lostpw.name"/></label></td>
      <td><stripes:text size="24" name="name" id="name" /></td>
    </tr>
    <tr>
      <td>&nbsp;</td>
      <td>
        <stripes:submit name="resetPassword"><fmt:message key="login.lostpw.submit"/></stripes:submit>
      </td>
    </tr>
  </table>

  <div class="formhelp">
    <fmt:message key="login.invite"/>
    <a href="#" onclick="$('menu-logincontent').fireEvent('click');"
                  title="<fmt:message key='login.title'/>" >
      <fmt:message key="login.heading.login"><fmt:param><wiki:Variable var="applicationname" /></fmt:param></fmt:message>
    </a>
  </div>
  <div class="formhelp">
    <fmt:message key="login.nopassword"/>
    <%--<a href="UserPreferences.jsp?tab=profile">--%>
    <a href="#" onclick="$('menu-profile').fireEvent('click');"
                  title="<fmt:message key='login.registernow.title'/>" >
      <fmt:message key="login.registernow">
        <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
      </fmt:message>
    </a>
  </div>

  </c:otherwise>
  </c:choose>

</stripes:form>
</div>

</wiki:Tab>
</wiki:UserCheck>

<%-- Register new user profile --%>
<wiki:Permission permission='editProfile'>
<wiki:Tab id="profile" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "login.register.tab")%>' >
  <wiki:Include page='ProfileTab.jsp'/>
</wiki:Tab>
</wiki:Permission>

<wiki:Tab id="loginhelp" title='<%=LocaleSupport.getLocalizedMessage(pageContext,"login.tab.help")%>' >
  <wiki:InsertPage page="LoginHelp" />

  <wiki:NoSuchPage page="LoginHelp">
  <div class="error">
    <fmt:message key="login.loginhelpmissing">
       <fmt:param><wiki:EditLink page="LoginHelp">LoginHelp</wiki:EditLink></fmt:param>
    </fmt:message>
  </div>
  </wiki:NoSuchPage>

</wiki:Tab>

</wiki:TabbedSection>