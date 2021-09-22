package uk.ac.man.cs.eventlite.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.DeleteMapping;

import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.dao.VenueService;

import javax.validation.Valid;

@Controller
@RequestMapping(value = "/venues", produces = { MediaType.TEXT_HTML_VALUE })
public class VenuesController {

	@Autowired
	private VenueService venueService;

	@GetMapping
	public String getAllVenues(Model model, @Param("Keyword") String Keyword) {
		if(Keyword != null)
			model.addAttribute("venues", venueService.search(Keyword));
		else
			model.addAttribute("venues", venueService.findAll());

		return "venues/index";
	}

	@GetMapping("/addVenue")
	public String newVenue(Model model) {
		return "venues/addVenue";
	}


	@RequestMapping(method = RequestMethod.POST)
	public String createNewVenue(@Valid Venue v, BindingResult bindingResult, Model model) throws InterruptedException{
		//validate form elements
		StringBuilder errString = new StringBuilder();

		if(bindingResult.hasErrors()){
			for(FieldError error : bindingResult.getFieldErrors())
				errString.append("Invalid ").append(error.getField())
					.append(" input: ").append(error.getRejectedValue()).append("\n");

			model.addAttribute("errorMsg", errString);
			model.addAttribute("venue", v);
			return "venues/addVenue";
		}

		//attempt to geocode and persist the venue, storing the result
		boolean coordinatesFound = venueService.geocodeVenue(v);

		//if geocoding failed report error
		if(!coordinatesFound){
			model.addAttribute("errorMsg", "Could not find venue by specified address and postcode.");
			model.addAttribute("venue", v);
			return "venues/addVenue";
		}

		venueService.save(v);
		return "redirect:/venues";
	}

    @RequestMapping(value = "/venuepage/{id}", method = RequestMethod.GET)
	public String getVenue(@PathVariable("id") long id, Model model) {

		Venue venue = venueService.findOne(id);
		model.addAttribute("id", id);
		model.addAttribute("name", venue.getName());
		model.addAttribute("address", venue.getAddress());
		model.addAttribute("postcode", venue.getPostcode());
		model.addAttribute("capacity", venue.getCapacity());

		List<Event> events = venue.getEvents();
		List<Event> venueUpcomingEvents = new ArrayList<Event>();

		if(events!=null)
			for (Event event : events) {
				if (event.getDate().compareTo(LocalDate.now()) >= 0)
					venueUpcomingEvents.add(event);
			}

		model.addAttribute("venue_upcoming_events", venueUpcomingEvents);

		return "venues/venuepage";
	}

    @DeleteMapping("/{id}")
    public String deleteVenue(Model model, @PathVariable("id") long id, RedirectAttributes redirect) {

    	Optional<Venue> optionalVenue = venueService.findById(id);

    	//validate venue found
    	if(optionalVenue.isEmpty()) {
			redirect.addFlashAttribute("deleteVenueErrorMsg",
									"Could not find venue by specified Id.");
			return "redirect:/venues";
		}

    	Venue venue = optionalVenue.get();

    	//validate venue events not null
		if(venue.getEvents() == null) {
			redirect.addFlashAttribute("deleteVenueErrorMsg",
									"Specified venue does not have an events attribute set.");
			return "redirect:/venues";
		}

		//Dont allow cascade delete. If venue is has associated events, report error
    	if (!venue.getEvents().isEmpty()) {
    		redirect.addFlashAttribute("deleteVenueErrorMsg",
									"Cannot delete venue with one or more events.");
    		return "redirect:/venues/venuepage/" + id;
    	}

    	//else everything is fine, free to delete
    	venueService.deleteById(id);
    	return "redirect:/venues";
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String updateV(@PathVariable("id") long id, Model model, RedirectAttributes redirect) {

		Optional<Venue> optionalVenue = venueService.findById(id);

		//validate venue found
		if(optionalVenue.isEmpty()) {
			redirect.addFlashAttribute("deleteVenueErrorMsg",
				"Could not find venue by specified Id.");
			return "redirect:/venues";
		}

		Venue venue = optionalVenue.get();

    	model.addAttribute("venue", venue);
    	return "venues/updateVenue";
    }

	@RequestMapping(value="update/{id}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String updateVenue(@PathVariable("id") long id, @Valid Venue v, BindingResult bindingResult, Model model) throws InterruptedException {
		StringBuilder errString = new StringBuilder();
		//check for any binding errors
		if(bindingResult.hasErrors()){
			for(FieldError error : bindingResult.getFieldErrors())
				errString.append("Invalid ").append(error.getField())
					.append(" input: ").append(error.getRejectedValue()).append("\n");

			model.addAttribute("updateVenueErrorMsg", errString);
			model.addAttribute("venue", v);
			return "venues/updateVenue";
		}

		//check if the id still matches an existing entry in the repository
		Optional<Venue> existingVenue = venueService.findById(id);
		if(existingVenue.isEmpty()){
			model.addAttribute("updateVenueErrorMsg", "Could not find venue by specified Id.");
			return "/venues";
		}

		//attempt to find coordinates by address and postcode
		boolean newCoordinatesFound = venueService.geocodeVenue(v);

		//if coordinates could not be resolved
		if(!newCoordinatesFound){
			model.addAttribute("updateVenueErrorMsg", "Could not find the coordinates of the new address");
			model.addAttribute("venue", v);
			return "venues/updateVenue";
		}

		venueService.save(v);
		return "redirect:/venues";
	}


}
