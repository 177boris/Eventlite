package uk.ac.man.cs.eventlite.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.entities.Event;

@ExtendWith(SpringExtension.class)
@WebMvcTest(VenuesController.class)
@Import(Security.class)
@AutoConfigureMockMvc
public class VenuesControllerTest {

	@Autowired
	private MockMvc mvc;

	@Mock
	private Event event;

	@Mock
	private Venue venue;

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
	public void getIndexWhenNoVenues() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>emptyList());

		mvc.perform(get("/venues").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
			.andExpect(view().name("venues/index")).andExpect(handler().methodName("getAllVenues"));

		verify(venueService).findAll();
		verifyNoInteractions(event);
		verifyNoInteractions(venue);
	}

	@Test
	public void getIndexWithVenues() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));

		mvc.perform(get("/venues").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
			.andExpect(view().name("venues/index")).andExpect(handler().methodName("getAllVenues"));

		verify(venueService).findAll();
		verifyNoInteractions(event);
	}

	@Test
	public void getIndexWithQueriedVenues() throws Exception {
		String param = "Keyword";
		String value = "somevenue";

		mvc.perform(get("/venues?"+ param + "=" + value).accept(MediaType.TEXT_HTML))
			.andExpect(status().isOk())
			.andExpect(view().name("venues/index"))
			.andExpect(handler().methodName("getAllVenues"));

		//verify searches by input Keyword
		verify(venueService).search(value);

		//doesnt call findAll
		verifyNoMoreInteractions(venueService);
		verifyNoInteractions(event);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void accessAddVenuePageAuthenticated() throws Exception{
		mvc.perform(get("/venues/addVenue").accept(MediaType.TEXT_HTML)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken)
			.param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(status().isOk())
			.andExpect(view().name("venues/addVenue"))
			.andExpect(handler().methodName("newVenue"));
	}

	@Test
	public void accessAddVenuePageNotAuthenticated() throws Exception{
		mvc.perform(get("/venues/addVenue").accept(MediaType.TEXT_HTML)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken)
			.param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(status().isFound())
			.andExpect(redirectedUrlPattern("**/sign-in"));
	}

	@Test
	public void createNewVenueWithoutPermissions() throws Exception{
		mvc.perform(post("/venues").accept(MediaType.TEXT_HTML).sessionAttr(TOKEN_ATTR_NAME, csrfToken)
			.param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(status().isFound())
			.andExpect(redirectedUrlPattern("**/sign-in"));
	}


	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void createNewValidVenueWithPermission() throws Exception{
		when(venueService.geocodeVenue(any())).thenReturn(true);

		mvc.perform(post("/venues").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("_csrf", csrfToken.getToken())
			.param("name", "testVenue").param("postcode", "M13 9PL")
			.param("address", "Kilburn Building University of Manchester, Oxford Rd")
			.param("capacity", "200")
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(model().hasNoErrors())
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/venues"));

		//verify coordinates were attempted to fetch
		verify(venueService).geocodeVenue(any());
		//ensure venue was saved
		verify(venueService).save(any());
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void createNewVenueInvalidCapacity() throws Exception{
		mvc.perform(post("/venues").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("_csrf", csrfToken.getToken())
			.param("name", "testVenue").param("postcode", "M13 9PL")
			.param("address", "Kilburn Building University of Manchester, Oxford Rd")
			.param("capacity", "invalid string value for int")
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			//doesnt crash
			.andExpect(status().isOk())
			//verify capacity error
			.andExpect(model().attributeHasFieldErrors("venue", "capacity"))
			//view stays in the same form
			.andExpect(view().name("venues/addVenue"));

		//check persisting object wasn't attempted via service
		verifyNoInteractions(venueService);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void createNewVenueInvalidCoordinates() throws Exception{
		String modelErrorAttributeName = "errorMsg";
		String errorMessage = "Could not find venue by specified address and postcode.";
		//signal finding coordinates failed
		when(venueService.geocodeVenue(any())).thenReturn(false);

		mvc.perform(post("/venues").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("_csrf", csrfToken.getToken())
			.param("name", "testVenue").param("postcode", "M13 9PL")
			.param("address", "Kilburn Building University of Manchester, Oxford Rd")
			.param("capacity", "200")
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			//doesnt crash
			.andExpect(status().isOk())
			//verify coordinates not found error is reported
			.andExpect(model().attribute(modelErrorAttributeName, errorMessage))
			//verify view stays in the same form
			.andExpect(view().name("venues/addVenue"));

		//check that geocoding was attempted
		verify(venueService).geocodeVenue(any());
		//check persisting object wasn't attempted via service
		verifyNoMoreInteractions(venueService);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void createNewVenueNoData() throws Exception{
		mvc.perform(post("/venues").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("_csrf", csrfToken.getToken())
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			//doesnt crash
			.andExpect(status().isOk())
			//verify form errors are caught
			.andExpect(model().hasErrors())
			//verify redirect back to the same form
			.andExpect(view().name("venues/addVenue"));

		//check persisting object wasn't attempted via service
		verifyNoInteractions(venueService);
	}

	@Test
	public void getDetailedVenuePage() throws Exception {
		String testAttribute = "test name";
		Venue v = new Venue();
		v.setName(testAttribute);

		when(venueService.findOne(0)).thenReturn(v);

		mvc.perform(get("/venues/venuepage/0").accept(MediaType.TEXT_HTML))
			.andExpect(status().isOk())
			.andExpect(view().name("venues/venuepage")).andExpect(handler().methodName("getVenue"))
			.andExpect(model().attribute("name", testAttribute));

		verify(venueService).findOne(0);
	}

	@Test
	public void deleteExistingVenueWithoutAuthority() throws Exception {
		mvc.perform(delete("/venues/0").accept(MediaType.TEXT_HTML)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			//finds redirect and points user to sign in page
			.andExpect(status().isFound())
			.andExpect(redirectedUrlPattern("**/sign-in"));

		//confirm does not interact with venue service
		verifyNoInteractions(venueService);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void deleteExistingVenueWithAuthority() throws Exception {
		Venue v = mock(Venue.class);
		when(venueService.findById(0)).thenReturn(Optional.of(v));
		when(v.getEvents()).thenReturn(Collections.emptyList());

		mvc.perform(delete("/venues/0").accept(MediaType.TEXT_HTML)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(handler().methodName("deleteVenue"))
			//expect redirect to venues
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/venues"))
			//expect redirect to contain no error messages
			.andExpect(flash().attribute("deleteVenueErrorMsg", org.hamcrest.Matchers.nullValue()));

		//verify validation passed and delete was accessed
		verify(venueService).deleteById(0);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void shouldNotDeleteVenueThatHasEvents() throws Exception {
		Venue v = mock(Venue.class);
		//venue not empty
		when(venueService.findById(0)).thenReturn(Optional.of(v));
		//venue has an event associated with it
		when(v.getEvents()).thenReturn(Collections.singletonList(new Event()));

		mvc.perform(delete("/venues/0").accept(MediaType.TEXT_HTML)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(handler().methodName("deleteVenue"))
			//expect redirect to venues
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/venues/venuepage/0"))
			//expect redirect to contain no error messages
			.andExpect(flash().attribute("deleteVenueErrorMsg",
										"Cannot delete venue with one or more events."));

		//verify venue was searched for
		verify(venueService).findById(0);
		//verify no further interaction with service
		verifyNoMoreInteractions(venueService);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void redirectWhenNoVenueToDeleteFound() throws Exception {
		//venue is empty
		when(venueService.findById(0)).thenReturn(Optional.empty());

		mvc.perform(delete("/venues/0").accept(MediaType.TEXT_HTML)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(handler().methodName("deleteVenue"))
			//expect redirect to venues
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/venues"))
			//expect redirect to contain no error messages
			.andExpect(flash().attribute("deleteVenueErrorMsg",
				"Could not find venue by specified Id."));

		//verify venue was searched for
		verify(venueService).findById(0);
		//verify no further interaction with service
		verifyNoMoreInteractions(venueService);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void redirectWhenVenueHasNoLinkToEvents() throws Exception {
		Venue v = mock(Venue.class);
		//venue not empty
		when(venueService.findById(0)).thenReturn(Optional.of(v));
		//venue has an event associated with it
		when(v.getEvents()).thenReturn(null);

		mvc.perform(delete("/venues/0").accept(MediaType.TEXT_HTML)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken).param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(handler().methodName("deleteVenue"))
			//expect redirect to venues
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/venues"))
			//expect redirect to contain no error messages
			.andExpect(flash().attribute("deleteVenueErrorMsg",
				"Specified venue does not have an events attribute set."));

		//verify venue was searched for
		verify(venueService).findById(0);
		//verify no further interaction with service
		verifyNoMoreInteractions(venueService);
	}

	@Test
	public void updateExistingVenueWithoutAuthority() throws Exception {
		mvc.perform(post("/venues/update/0").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken)
			.param(csrfToken.getParameterName(), csrfToken.getToken()))
			//finds redirect and points user to sign in page
			.andExpect(status().isFound())
			.andExpect(redirectedUrlPattern("**/sign-in"));

		//confirm does not interact with venue service
		verifyNoInteractions(venueService);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void updateExistingVenueWithValidParams() throws Exception {
		Venue v = mock(Venue.class);
		when(venueService.findById(0)).thenReturn(Optional.of(v));

		//mock coordinates to save map api access
		when(venueService.geocodeVenue(any())).thenReturn(true);
		v.setLatitude(50);
		v.setLongitude(50);

		mvc.perform(post("/venues/update/0").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken)
			.param(csrfToken.getParameterName(), csrfToken.getToken())
			.param("_csrf", csrfToken.getToken())
			.param("name", "testVenue").param("postcode", "M13 9PL")
			.param("address", "Kilburn Building University of Manchester, Oxford Rd")
			.param("capacity", "200"))
			.andExpect(handler().methodName("updateVenue"))
			//expect redirect to venues
			.andExpect(status().isFound())
			.andExpect(redirectedUrl("/venues"))
			//expect redirect to contain no error messages
			.andExpect(flash().attribute("updateVenueErrorMsg", org.hamcrest.Matchers.nullValue()));

		//verify coordinates were attempted to find
		verify(venueService).geocodeVenue(any());
		//verify validation passed and venue update persisted
		verify(venueService).save(any());
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void updateExistingVenueWithInvalidGeocoding() throws Exception {
		Venue v = mock(Venue.class);
		when(venueService.findById(0)).thenReturn(Optional.of(v));

		//mock coordinates to save map api access
		when(venueService.geocodeVenue(any())).thenReturn(false);

		mvc.perform(post("/venues/update/0").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken)
			.param(csrfToken.getParameterName(), csrfToken.getToken())
			.param("_csrf", csrfToken.getToken())
			.param("name", "testVenue").param("postcode", "unrecognised postcode")
			.param("address", "unrecognised address")
			.param("capacity", "200"))
			.andExpect(handler().methodName("updateVenue"))
			//view stays in the same template, but reports the error
			.andExpect(status().isOk())
			.andExpect(view().name("venues/updateVenue"))
			//match expected error message
			.andExpect(model().attribute("updateVenueErrorMsg",
				"Could not find the coordinates of the new address"));

		//verify finding coordinates was attempted
		verify(venueService).geocodeVenue(any());
		verify(venueService).findById(0);
		//verify no further interaction
		verifyNoMoreInteractions(venueService);
	}

	@Test
	@WithMockUser(roles = "ADMINISTRATOR")
	public void updateExistingVenueWithNoData() throws Exception {
		mvc.perform(post("/venues/update/0").accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.sessionAttr(TOKEN_ATTR_NAME, csrfToken)
			.param(csrfToken.getParameterName(), csrfToken.getToken()))
			.andExpect(handler().methodName("updateVenue"))
			//view stays in the same template, but reports the error
			.andExpect(status().isOk())
			.andExpect(view().name("venues/updateVenue"))
			//expect error message field attribute to exist
			.andExpect(model().attributeExists("updateVenueErrorMsg"));

		//verify validation passed and venue update persisted
		verifyNoInteractions(venueService);
	}
}
