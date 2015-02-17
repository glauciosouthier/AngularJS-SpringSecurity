package br.com.app;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.dontdrinkandroot.example.angularrestspringsecurity.entity.User;
import net.dontdrinkandroot.example.angularrestspringsecurity.rest.TokenUtils;
import net.dontdrinkandroot.example.angularrestspringsecurity.rest.resources.ModelService;
import net.dontdrinkandroot.example.angularrestspringsecurity.transfer.TokenTransfer;

import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.plugins.server.servlet.HttpServletInputMessage;
import org.jboss.resteasy.spi.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

public class AuthenticationFilter implements javax.servlet.Filter {
	protected String[] rolesAllowed;
	protected boolean denyAll;
	private HttpRequest request = null;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private static final String AUTHORIZATION_PROPERTY = "Authorization";
	private static final String AUTHENTICATION_SCHEME = "Basic";
	private static final ServerResponse ACCESS_DENIED = new ServerResponse(
			"Access denied for this resource", 401, new Headers<Object>());;
	private static final ServerResponse ACCESS_FORBIDDEN = new ServerResponse(
			"Nobody can access this resource", 403, new Headers<Object>());;
	private static final ServerResponse SERVER_ERROR = new ServerResponse(
			"INTERNAL SERVER ERROR", 500, new Headers<Object>());;

	@Override
	public void doFilter(ServletRequest request, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = this.getAsHttpRequest(request);

		final List<String> authentication = extractAuthBasicFromRequest(httpRequest);
		// Fetch authorization header
		final List<String> authorization = extractAuthTokenFromRequest(httpRequest);
		// .getRequestHeader(AUTHORIZATION_PROPERTY);

		// If no authorization information present; block access
		if (authentication == null
				&& (authorization == null || authorization.isEmpty())) {
			return ACCESS_DENIED;
		}

		if (authentication != null && authentication.size() > 0
				&& authentication.size() <= 2) {
			// first login
			if (authentication.get(0) == null) {
				// username is expected
				return ACCESS_DENIED;
			}
			User userDetails = getUserDao().findByName(authentication.get(0));

			if (userDetails != null
					&& verifyPassword(userDetails, authentication.get(1))) {
				return new TokenTransfer(TokenUtils.createToken(userDetails));
			}
			((HttpServletInputMessage) request).getHttpHeaders();
			return ACCESS_DENIED;
		}
		if (authorization != null && authorization.size() > 0) {
			final String username = authorization.get(0).split(":")[0];
			final String token = authorization.get(0);
			// Verifying Username and token
			System.out.println(username);
			System.out.println(token);
			return validateUser(method, username, token);
		}

		return ACCESS_DENIED;
		chain.doFilter(request, response);
	}

	private HttpServletRequest getAsHttpRequest(ServletRequest request) {
		if (!(request instanceof HttpServletRequest)) {
			throw new RuntimeException("Expecting an HTTP request");
		}

		return (HttpServletRequest) request;
	}

	private boolean isUserAllowed(final String username,
			final String authToken, final Set<String> rolesSet) {
		boolean isAllowed = false;

		UserDetails userDetails = ModelService.getInstance().getUserDao()
				.findByName(username);
		if (!TokenUtils.validateToken(authToken, userDetails)) {
			return false;
		}
		for (GrantedAuthority authority : userDetails.getAuthorities()) {
			if (rolesSet.contains(authority.toString())) {
				isAllowed = true;
			}
		}
		this.request.setAttribute("user", userDetails);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				userDetails, null, userDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
		return isAllowed;
	}

	private List<String> extractAuthTokenFromRequest(HttpServletRequest httpRequest)
	{
		List<String> ret=new LinkedList<String>();
		/* Get token from header */
		ret.add( httpRequest.getParameter("X-Auth-Token"));
		/* If token not found get it from request parameter */
		if (ret.isEmpty()) {
			ret.add( httpRequest.getParameter("token"));
		}

		return ret;
	}

	private List<String> extractAuthBasicFromRequest(HttpServletRequest request) {
		List<String> ret=new LinkedList<String>();
		/* Get token from header */
		String authToken = request.getHeader(
				"AUTHORIZATION_PROPERTY");
		if (authToken != null && !"".equals(authToken)) {
			String encodedUserPassword;
			String usernameAndPassword;
			try {
				// Get encoded username and password
				encodedUserPassword = authToken.replaceFirst(
						AUTHENTICATION_SCHEME + " ", "");
				usernameAndPassword = new String(
						Base64.decode(encodedUserPassword.getBytes()));
				ret.add((String) usernameAndPassword.split(":")[0]);
				ret.add((String) usernameAndPassword.split(":")[1]);
			} catch (Exception e) {
				return null;
			}
		}
		if (ret == null || ret.isEmpty()) {
			ret = new LinkedList<String>();
			ret.add((String) request.getParameterValues(
					"username")[0]);
			ret.add((String) request.getParameterValues(
					"password")[0]);
		}
		return ret;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}
}
