package br.com.app;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;

import net.dontdrinkandroot.example.angularrestspringsecurity.dao.user.UserDao;
import net.dontdrinkandroot.example.angularrestspringsecurity.entity.User;
import net.dontdrinkandroot.example.angularrestspringsecurity.rest.TokenUtils;
import net.dontdrinkandroot.example.angularrestspringsecurity.rest.resources.ModelService;
import net.dontdrinkandroot.example.angularrestspringsecurity.transfer.TokenTransfer;

import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.plugins.server.servlet.HttpServletInputMessage;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.AcceptedByMethod;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

/**
 * This interceptor verify the access permissions for a user based on username
 * and passowrd provided in request
 * */
@Provider
@ServerInterceptor
public class SecurityInterceptor implements PreProcessInterceptor,
		AcceptedByMethod {
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
			"INTERNAL SERVER ERROR", 500, new Headers<Object>());
	private static final String AUTH_HEADER_NAME = "X-Auth-Token";
	private static final long TEN_DAYS = 1000 * 60 * 60 * 24 * 10;

	public boolean accept(Class declaring, Method method) {
		if (declaring == null || method == null)
			return false;
		RolesAllowed allowed = (RolesAllowed) declaring
				.getAnnotation(RolesAllowed.class);
		RolesAllowed methodAllowed = method.getAnnotation(RolesAllowed.class);
		if (methodAllowed != null)
			allowed = methodAllowed;
		if (allowed != null) {
			rolesAllowed = allowed.value();
		}
		denyAll = (declaring.isAnnotationPresent(DenyAll.class)
				&& method.isAnnotationPresent(RolesAllowed.class) == false && method
				.isAnnotationPresent(PermitAll.class) == false)
				|| method.isAnnotationPresent(DenyAll.class);
		Boolean permit = rolesAllowed != null || denyAll;
		//logger.error("Class: " + declaring.getSimpleName() + " Method: "
		//		+ method.getName() + " permit: " + permit);
		return permit;
	}

	@Override
	public ServerResponse preProcess(HttpRequest request,
			ResourceMethod methodInvoked) throws Failure,
			WebApplicationException {
		this.request = request;
		//logRequest(request);

		Method method = methodInvoked.getMethod();
		Class clazz = methodInvoked.getClass();

		// Access allowed for all
		if (method.isAnnotationPresent(PermitAll.class)) {
			return null;
		}
		// Access denied for all
		if (method.isAnnotationPresent(DenyAll.class)) {
			return ACCESS_FORBIDDEN;
		}
		final List<String> authentication = extractAuthBasicFromRequest(request);
		// Fetch authorization header
		final List<String> authorization = extractAuthTokenFromRequest(request);
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
				UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				SecurityContextHolder.getContext().setAuthentication(auth);
				// addAuthentication(HttpServletResponse response, new
				// TokenTransfer(TokenUtils.createToken(userDetails)));
				// return new
				// TokenTransfer(TokenUtils.createToken(userDetails));
				return null;
			}
			return ACCESS_DENIED;
		}
		if (authorization != null && authorization.size() > 0) {
			final String username = authorization.get(0).split(":")[0];
			final String token = authorization.get(0);
			// Verifying Username and token
			//System.out.println(username);
			System.out.println(token);
			return validateUser(method, username, token);
		}

		return ACCESS_DENIED;
	}

	public void addAuthentication(HttpServletResponse response,
			TokenTransfer token) {
		response.addHeader(AUTH_HEADER_NAME, token.getToken());
	}

	public Boolean verifyPassword(User user, String pass) {
		StandardPasswordEncoder codec = (StandardPasswordEncoder) ModelService
				.getInstance().getPasswordEncoder();
		if (user != null) {
			return codec.matches(pass, user.getPassword());
		}
		return false;
	}

	private UserDao getUserDao() {
		return ModelService.getInstance().getUserDao();
	}

	private void logRequest(HttpRequest request) {
		HttpServletInputMessage httpServletRequest = (HttpServletInputMessage) request;
		logger.error(request.getHttpMethod());
		logger.error(request.getUri().getRequestUri().toString());
		for (String key : request.getFormParameters().keySet()) {
			logger.error(key + " = "
					+ request.getFormParameters().get(key).toString());
		}
		for (String key : request.getDecodedFormParameters().keySet()) {
			logger.error(key + " = "
					+ request.getDecodedFormParameters().get(key).toString());
		}
		for (String key : request.getHttpHeaders().getRequestHeaders().keySet()) {
			logger.error(key
					+ " = "
					+ request.getHttpHeaders().getRequestHeaders().get(key)
							.toString());
		}
		for (String key : httpServletRequest.getFormParameters().keySet()) {
			logger.error(key
					+ " = "
					+ httpServletRequest.getFormParameters().get(key)
							.toString());
		}
	}

	private ServerResponse validateUser(Method method, final String username,
			final String token) {
		Set<String> rolesSet = null;
		// Verify user access
		if (method.isAnnotationPresent(RolesAllowed.class)) {
			RolesAllowed rolesAnnotation = method
					.getAnnotation(RolesAllowed.class);
			rolesSet = new HashSet<String>(Arrays.asList(rolesAnnotation
					.value()));

			// Is user valid?
			if (!isUserAllowed(username, token, rolesSet)) {
				return ACCESS_DENIED;
			}
		}
		return null;
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

	private List<String> extractAuthTokenFromRequest(HttpRequest request) {
		/* Get token from header */
		List<String> authToken = request.getHttpHeaders().getRequestHeader(
				AUTH_HEADER_NAME);
		/* If token not found get it from request parameter */
		if (authToken == null || authToken.isEmpty()) {
			try {
				authToken = new LinkedList<String>();
				authToken = request.getHttpHeaders().getRequestHeader("token");
			} catch (Exception e) {

			}
		}
		if (authToken == null || authToken.isEmpty()) {
			try {
				authToken = new LinkedList<String>();
				authToken.addAll(request.getFormParameters().get("token"));
			} catch (Exception e) {

			}
		}
		if (authToken == null || authToken.isEmpty()) {
			try {
				authToken = new LinkedList<String>();
				authToken.add(request.getUri().getQueryParameters().get("token").get(0));
			} catch (Exception e) {

			}
		}
		return authToken;
	}

	private List<String> extractAuthBasicFromRequest(HttpRequest request) {
		/* Get token from header */
		List<String> authToken = request.getHttpHeaders().getRequestHeader(
				"AUTHORIZATION_PROPERTY");
		if (authToken != null && !authToken.isEmpty()) {
			String encodedUserPassword;
			String usernameAndPassword;
			try {
				// Get encoded username and password
				encodedUserPassword = authToken.get(0).replaceFirst(
						AUTHENTICATION_SCHEME + " ", "");
				usernameAndPassword = new String(
						Base64.decode(encodedUserPassword.getBytes()));
				authToken = new LinkedList<String>();
				String username = (String) usernameAndPassword.split(":")[0];
				if (username != null && !"".equals(username))
					authToken.add(username);
				String password = (String) usernameAndPassword.split(":")[1];
				if (password != null && !"".equals(password))
					authToken.add(password);
			} catch (Exception e) {
				return null;
			}
		}
		if (authToken == null || authToken.isEmpty()) {
			authToken = new LinkedList<String>();
			String username = (String) request.getFormParameters().getFirst(
					"username");
			if (username != null && !"".equals(username))
				authToken.add(username);
			String password = (String) request.getFormParameters().getFirst(
					"password");
			if (password != null && !"".equals(password))
				authToken.add(password);
		}
		return authToken;
	}
}