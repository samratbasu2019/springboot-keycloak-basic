package com.example.productapp;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakSecurityComponents;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.IDToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;

@SpringBootApplication
public class ProductAppApplication {
	public static void main(String[] args) {
		SpringApplication.run(ProductAppApplication.class, args);
	}
}

@Controller
class ProductController {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProductController.class);
	@GetMapping(path = "/api/products")
	public String getProducts(Model model, HttpServletRequest request){
		KeycloakAuthenticationToken principal = (KeycloakAuthenticationToken) request.getUserPrincipal();
		String userId = principal.getAccount().getKeycloakSecurityContext().getIdToken().getSubject();
		String email = principal.getAccount().getKeycloakSecurityContext().getIdToken().getEmail();
		IDToken token = principal.getAccount().getKeycloakSecurityContext().getIdToken();
		Map<String, Object> customClaims = token.getOtherClaims();
		String institutionID = null, phoneNumber = null;
		if (customClaims.containsKey("institutionID")) {
			institutionID = String.valueOf(customClaims.get("institutionID"));
		}
		if (customClaims.containsKey("phoneNumber")) {
			phoneNumber = String.valueOf(customClaims.get("phoneNumber"));
		}
		LOGGER.info("Logged-in userid is {} email {} institutionID {} phoneNumber {}", userId,
				email, institutionID, phoneNumber);

		model.addAttribute("products", Arrays.asList("b2c Service","Career Path","Payment"));
		return "products";
	}

	@GetMapping(path = "/logout")
	public String logout(HttpServletRequest request) throws ServletException {
		request.logout();
		return "/";
	}
}

@Configuration
@EnableWebSecurity
@ComponentScan(basePackageClasses = KeycloakSecurityComponents.class)
class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter
{
	/**
	 * Registers the KeycloakAuthenticationProvider with the authentication manager.
	 */
	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
		keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
		auth.authenticationProvider(keycloakAuthenticationProvider);
	}

	/**
	 * Defines the session authentication strategy.
	 */
	@Bean
	@Override
	protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
		return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
	}

	@Bean
	public KeycloakConfigResolver KeycloakConfigResolver() {return new KeycloakSpringBootConfigResolver();}

	@Override
	protected void configure(HttpSecurity http) throws Exception
	{
		super.configure(http);
		http
				.csrf().disable()
				.authorizeRequests()
				.antMatchers("/api/products").hasRole("b2c")
				.anyRequest().permitAll();
	}
}

