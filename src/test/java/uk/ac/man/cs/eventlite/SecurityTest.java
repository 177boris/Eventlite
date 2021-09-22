package uk.ac.man.cs.eventlite;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import uk.ac.man.cs.eventlite.config.Security;

@ExtendWith(SpringExtension.class)
@WebMvcTest(EventLite.class)
@Import(Security.class)
public class SecurityTest {

	// This class isn't strictly necessary. We wouldn't normally make a habit of
	// testing functionality provided by a library, but these tests serve as good
	// examples of how to (unit) test with and around security.

	// the test methods in this class have been removed as they are pointless after overriding UserDetailsService,
	// users, their roles and privileges are now persisted in a database and dont use the Non-persistent implementation of
	// UserDetailsManager. This prepares the application for further scaling and adding registration and not loosing user data
	// everytime the server is restarted.
}
