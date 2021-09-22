package uk.ac.man.cs.eventlite.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@WebMvcTest(EventsController.class)
@Import(Security.class)
public class EventsControllerTest {

	@Autowired
	private MockMvc mvc;

	@Mock
	private Event event;

	@Mock
	private Venue venue;

	@MockBean
	private EventService eventService;

	@MockBean
	private VenueService venueService;

	private CsrfToken csrfToken;
	private final String TOKEN_ATTR_NAME =
		"org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN";

	@BeforeEach
	public void initCsrfToken(){
		HttpSessionCsrfTokenRepository httpSessionCsrfTokenRepository = new HttpSessionCsrfTokenRepository();
		csrfToken = httpSessionCsrfTokenRepository.generateToken(new MockHttpServletRequest());
	}

	@Test
	public void getIndexWhenNoEvents() throws Exception {
		when(eventService.findAll()).thenReturn(Collections.<Event>emptyList());

		mvc.perform(get("/events").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("events/index")).andExpect(handler().methodName("getAllEvents"));

		verifyNoInteractions(event);
		verifyNoInteractions(venue);
	}

	@Test
	public void getIndexWithEvents() throws Exception {
		when(venue.getName()).thenReturn("Kilburn Building");

		when(event.getVenue()).thenReturn(venue);
		when(event.getDate()).thenReturn(LocalDate.MAX);
		when(eventService.findAll()).thenReturn(Collections.<Event>singletonList(event));

		mvc.perform(get("/events").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("events/index")).andExpect(handler().methodName("getAllEvents"));

		verify(eventService).findAll();
	}

	@Test
	public void getIndexWithQueriedEvents() throws Exception {
		String param = "Keyword";
		String value = "somevenue";

		mvc.perform(get("/events?"+ param + "=" + value).accept(MediaType.TEXT_HTML))
			.andExpect(status().isOk())
			.andExpect(view().name("events/index"))
			.andExpect(handler().methodName("getAllEvents"));

		//verify searches by input Keyword
		verify(eventService).search(value);

		//doesnt call findAll
		verifyNoMoreInteractions(eventService);
		verifyNoInteractions(venue);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void accessAddEventPageAuthenticated() throws Exception{
		when(venueService.findAll()).thenReturn(Collections.singletonList(new Venue()));

		mvc.perform(get("/events/addEvent").accept(MediaType.TEXT_HTML)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken)
			.param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(status().isOk())
			.andExpect(view().name("events/addEvent"))
			.andExpect(handler().methodName("newEvent"));

		verify(venueService).findAll();
	}

	@Test
	public void accessAddEventPageNotAuthenticated() throws Exception{
		when(venueService.findAll()).thenReturn(Collections.singletonList(new Venue()));

		mvc.perform(get("/events/addEvent").accept(MediaType.TEXT_HTML)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken)
			.param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(status().isFound())
			.andExpect(redirectedUrlPattern("**/sign-in"));

		verifyNoInteractions(venueService);
	}

	@Test
	public void createNewEventWithoutPermissions() throws Exception{
		mvc.perform(post("/events").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("_csrf", csrfToken.getToken())
			.param("name", "testEvent")
			.param("venue", "0")
			.param("date", "2021-05-17")
			.param("time", "22:17")
			.param("description", "desc")
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(status().isFound())
			.andExpect(redirectedUrlPattern("**/sign-in"));

		//check no venue was looked up
		verifyNoInteractions(venueService);
		//check persisting object wasn't attempted
		verifyNoInteractions(eventService);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void createNewValidEventWithPermission() throws Exception{
		mvc.perform(post("/events").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("_csrf", csrfToken.getToken())
			.param("name", "testEvent")
			.param("venue.id", "1")
			.param("date", "2021-05-17")
			.param("time", "22:17")
			.param("description", "desc")
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/events"));

		//check validation successful by making sure event persisted after binding
		verify(eventService).save(any());
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void createNewEventInvalidTimeFormat() throws Exception{
		when(venueService.findById(0)).thenReturn(java.util.Optional.of(new Venue()));

		mvc.perform(post("/events").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("_csrf", csrfToken.getToken())
			.param("name", "testEvent")
			.param("venue", "0")
			.param("date", "2020-04-25")
			.param("time", "25:80:79")
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			//status okay, since the controller deals with binding errors
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("errorMsg"))
			//check correctly redirected back to addEvent
			.andExpect(view().name("events/addEvent"));

		//verify refreshed venue list before redirect
		verify(venueService).findAll();
		//check persisting object wasn't attempted
		verifyNoInteractions(eventService);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void createNewEventNoData() throws Exception{
		mvc.perform(post("/events").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("_csrf", csrfToken.getToken())
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			//verify invalid inputs handled without issues in the same view
			.andExpect(status().isOk())
			.andExpect(view().name("events/addEvent"));

		//check persisting object wasn't attempted via service
		verifyNoInteractions(eventService);
	}

	@Test
	public void getDetailedEventPage() throws Exception {
		when(eventService.findOne(0)).thenReturn(event);
		when(event.getVenue()).thenReturn(new Venue());

		mvc.perform(get("/events/eventpage/0").accept(MediaType.TEXT_HTML))
			.andExpect(status().isOk())
			.andExpect(view().name("events/eventpage")).andExpect(handler().methodName("getEventDetails"))
			.andExpect(model().attribute("event", event));

		verify(eventService).findOne(0);
	}

	@Test
	public void accessUpdateEventViewWithoutAuthority() throws Exception{
		mvc.perform(post("/events/0").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken)
			.param(csrfToken.getParameterName(), csrfToken.getToken()))
			//finds redirect and points user to sign in page
			.andExpect(status().isFound())
			.andExpect(redirectedUrlPattern("**/sign-in"));

		verifyNoInteractions(eventService);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void updateEventWithValidData() throws Exception{
		mvc.perform(post("/events/update/0").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken)
			.param(csrfToken.getParameterName(), csrfToken.getToken())
			.param("_csrf", csrfToken.getToken())
			.param("name", "testEvent")
			.param("venue.id", "0")
			.param("date", "2021-05-17")
			.param("time", "22:17")
			.param("description", "desc"))
			.andExpect(handler().methodName("updateEvent"))
			//expect redirect to events
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/events"));

		//verify update called on our event
		verify(eventService).save(any());
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void updateEventWithNoData() throws Exception{

		mvc.perform(post("/events/update/0").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken)
			.param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(handler().methodName("updateEvent"))
			//doesnt crash and stays in the same view
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/events/0?"))
			//reports missing fields in an error box
			.andExpect(flash().attributeExists("errorMsg"));

		//verify update called on our event
		verifyNoInteractions(eventService);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void updateEventWithInvalidData() throws Exception{
		mvc.perform(post("/events/update/0").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken)
			.param(csrfToken.getParameterName(), csrfToken.getToken())
			.param("_csrf", csrfToken.getToken())
			.param("name", "testEvent")
			.param("venue.id", "43")
			.param("date", "2021-43-12")
			.param("time", "22:171")
			.param("description", "desc"))
			.andExpect(handler().methodName("updateEvent"))
			//redirects to the same form
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/events/0?"))
			//reports missing fields in an error box
			.andExpect(flash().attributeExists("errorMsg"));

		//verify update called on our event
		verifyNoInteractions(eventService);
	}

	@Test
	public void deleteExistingEventWithoutAuthority() throws Exception {
		mvc.perform(delete("/events/0").accept(MediaType.TEXT_HTML)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			//finds redirect and points user to sign in page
			.andExpect(status().isFound())
			.andExpect(redirectedUrlPattern("**/sign-in"));

		//confirm does not interact with event service
		verifyNoInteractions(eventService);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void deleteExistingEventWithAuthority() throws Exception {
		when(eventService.findById(0)).thenReturn(Optional.of(new Event()));

		mvc.perform(delete("/events/0").accept(MediaType.TEXT_HTML)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(handler().methodName("deleteEvent"))
			//expect redirect to venues
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/events"))
			//expect redirect to contain no error messages
			.andExpect(flash().attribute("deleteEventErrorMsg", org.hamcrest.Matchers.nullValue()));

		//verify event id was checked to be valid
		verify(eventService).findById(0L);
		//verify delete was called
		verify(eventService).deleteById(0L);
		//confirm does not interact with event service
		verifyNoMoreInteractions(eventService);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void deleteNonExistentEventWithAuthority() throws Exception {
		when(eventService.findById(0)).thenReturn(Optional.empty());

		mvc.perform(delete("/events/0").accept(MediaType.TEXT_HTML)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(handler().methodName("deleteEvent"))
			//expect redirect to venues
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/events"))
			//expect redirect to contain an error message
			.andExpect(flash().attribute("deleteEventErrorMsg", "Could not find event by specified Id."));

		//verify event id was checked to be valid
		verify(eventService).findById(0L);
		//confirm service didnt attempt delete and does not further interact with event service
		verifyNoMoreInteractions(eventService);
	}
}
