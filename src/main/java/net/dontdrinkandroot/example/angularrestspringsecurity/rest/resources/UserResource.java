package net.dontdrinkandroot.example.angularrestspringsecurity.rest.resources;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import net.dontdrinkandroot.example.angularrestspringsecurity.rest.TokenUtils;
import net.dontdrinkandroot.example.angularrestspringsecurity.transfer.TokenTransfer;
import net.dontdrinkandroot.example.angularrestspringsecurity.transfer.UserTransfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Path("/user")
public class UserResource {

	@Autowired
	private UserDetailsService userService;

	@Autowired
	@Qualifier("authenticationManager")
	private AuthenticationManager authManager;
	@Autowired
	private PasswordEncoder passwordEncoder;

	/**
	 * Retrieves the currently logged in user.
	 * 
	 * @return A transfer containing the username and the roles.
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/get")
	public UserTransfer getUser() {
		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		Object principal = authentication.getPrincipal();
		if (principal instanceof String
				&& ((String) principal).equals("anonymousUser")) {
			throw new WebApplicationException(401);
		}
		UserDetails userDetails = (UserDetails) principal;

		return new UserTransfer(userDetails.getUsername(),
				this.createRoleMap(userDetails));
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{username}")
	public UserTransfer getUserByName(@PathParam("username") String username) {
		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		Object principal = authentication.getPrincipal();
		if (principal instanceof String
				&& ((String) principal).equals("anonymousUser")) {
			throw new WebApplicationException(401);
		}
		UserDetails userDetails = (UserDetails) principal;
		if (!userDetails.getUsername().equals(username)) {
			throw new WebApplicationException(401);
		}
		return new UserTransfer(userDetails.getUsername(),
				this.createRoleMap(userDetails));
	}

	/**
	 * Authenticates a user and creates an authentication token.
	 * 
	 * @param username
	 *            The name of the user.
	 * @param password
	 *            The password of the user.
	 * @return A transfer containing the authentication token.
	 */
	@Path("/authenticate")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public TokenTransfer authenticate(@FormParam("username") String username,
			@FormParam("password") String password) {
		if(username==null || "".equals(username)||password==null || "".equals(password)){
			throw new BadCredentialsException("Bad credencials");
		}
	
		UserDetails userDetails = userService.loadUserByUsername(username);
		if(userDetails==null){
			throw new UsernameNotFoundException("Username not found");
		}
		if(!passwordEncoder.matches(password, userDetails.getPassword())){
			throw new BadCredentialsException("Bad credencials");
		}
		
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				username, password);
		
		SecurityContextHolder.getContext().setAuthentication(authentication);

		return new TokenTransfer(TokenUtils.createToken(userDetails));
	}

	private Map<String, Boolean> createRoleMap(UserDetails userDetails) {
		Map<String, Boolean> roles = new HashMap<String, Boolean>();
		for (GrantedAuthority authority : userDetails.getAuthorities()) {
			roles.put(authority.getAuthority(), Boolean.TRUE);
		}

		return roles;
	}

}