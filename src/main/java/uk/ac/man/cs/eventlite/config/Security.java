package uk.ac.man.cs.eventlite.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class Security extends WebSecurityConfigurerAdapter {

	public static final String ADMIN_ROLE = "ADMINISTRATOR";
	public static final String ORGANISER_PERMISSION = "MANAGE_EVENTS";
	public static final String ORGANISER_ROLE = "EVENT_ORGANISER";

	// List the mappings/methods for which no authorisation is required.
	// By default we allow all GETs and full access to the H2 console.
	private static final RequestMatcher[] NO_AUTH = { new AntPathRequestMatcher("/webjars/**", "GET"),
			new AntPathRequestMatcher("/**", "GET"), new AntPathRequestMatcher("/h2-console/**") };

	// restrictive mappings for /event/
	// Authorisation required for accessing addEvent page and sending POST request to events
	private static final RequestMatcher[] ORGANISER_AUTH = {
			new AntPathRequestMatcher("/events/addEvent", "GET"),
			//regex matching text starting with "/events/" followed by an ID number and any GET query strings after
			new RegexRequestMatcher("(\\/events\\/)(\\d+)(\\??)(.*)", "GET"),
			new AntPathRequestMatcher("/events", "POST"),
			new AntPathRequestMatcher("/events", "DELETE"),
			new AntPathRequestMatcher("/events", "DELETE"),
			new AntPathRequestMatcher("/venues/addVenue", "GET"),
			new RegexRequestMatcher("(\\/venues\\/)(\\d+)(\\??)(.*)", "GET"),
			new AntPathRequestMatcher("/venues", "POST"),
			new AntPathRequestMatcher("/venues", "DELETE"),
			new AntPathRequestMatcher("/venues", "DELETE"),

			//restrict sensitive data api access via regex
			new RegexRequestMatcher("(\\/api\\/roles)(.*)", "GET"),
			new RegexRequestMatcher("(\\/api\\/users)(.*)", "GET"),
			new RegexRequestMatcher("(\\/api\\/permissions)(.*)", "GET")
	};

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// By default, all requests are authenticated except our specific lists.
		http.authorizeRequests()
				.requestMatchers(ORGANISER_AUTH).hasAnyRole(ORGANISER_ROLE, ADMIN_ROLE)
				.requestMatchers(NO_AUTH).permitAll().anyRequest().hasRole(ADMIN_ROLE);

		// Use form login/logout for the Web.
		http.formLogin().loginPage("/sign-in").permitAll();
		http.logout().logoutUrl("/sign-out").logoutSuccessUrl("/").permitAll();

		http.requestMatcher(new AntPathRequestMatcher("/api/**")).httpBasic();

		// Only use CSRF for Web requests.
		// Disable CSRF for the API and H2 console.
		http.antMatcher("/**").csrf().ignoringAntMatchers("/api/**", "/h2-console/**");

		// Disable X-Frame-Options for the H2 console.
		http.headers().frameOptions().disable();
	}
}
