package uk.ac.man.cs.eventlite.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.minidev.json.JSONObject;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/venues", produces = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE })
public class VenuesControllerApi {

	@Autowired
	private VenueService venueService;

	@Autowired
	private EventService eventService;

	@GetMapping("/index")
	public CollectionModel<Venue> getAllVenues() {

		return venueCollection(venueService.findAll());
	}

	@GetMapping("/{id}")
	private ResponseEntity<JSONObject> singleVenue(@PathVariable("id") long id) {
		Venue venue = venueService.findOne(id);
		JSONObject res = new JSONObject();
		res.put("name", venue.getName());
		res.put("capacity", venue.getCapacity());
		JSONObject links = new JSONObject();
		JSONObject self = new JSONObject();
		self.put("href", "http://localhost:8080/api/venues/" + id);
		links.put("self", self);
		links.put("venue", self);
		JSONObject events = new JSONObject();
		events.put("href", "http://localhost:8080/api/venues/" + id + "/events");
		links.put("events", events);
		JSONObject next = new JSONObject();
		next.put("href", "http://localhost:8080/api/venues/" + id + "/next3events");
		links.put("next3events", next);
		res.put("_links", links);

		return ResponseEntity.ok(res);
	}

	@GetMapping("/{id}/next3Events")
	public EntityModel<Model> getVenueEvents(@PathVariable(value = "id") long id, Model model) {
		Optional<Venue> venue = venueService.findById(id);

		if(venue.isEmpty())
			return null;

		Link selfLink = linkTo(VenuesControllerApi.class).slash(venue.get().getId()).slash("next3events").withSelfRel();
		model.addAttribute("venue", venue.get());
		model.addAttribute("next3events", eventService.findNext3(venue.get().getId(), LocalDate.now(), LocalTime.now()));

		return EntityModel.of(model, selfLink);
	}

	private CollectionModel<Venue> venueCollection(Iterable<Venue> venues) {
		Link selfLink = linkTo(methodOn(VenuesControllerApi.class).getAllVenues()).withSelfRel();

		return CollectionModel.of(venues, selfLink);
	}
}
