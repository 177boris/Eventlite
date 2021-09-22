package uk.ac.man.cs.eventlite.controllers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.repository.query.Param;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import twitter4j.TwitterException;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.TwitterService;
import uk.ac.man.cs.eventlite.dao.VenueService;

import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

import javax.validation.Valid;

@Controller
@RequestMapping(value = "/events", produces = { MediaType.TEXT_HTML_VALUE })

public class EventsController {

	private TwitterService twitterService = new TwitterService();
	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	@Autowired
	private Environment env;

	@GetMapping
	public String getAllEvents(Model model, @Param("Keyword") String Keyword ) throws TwitterException {
		List<Event> upcomingEventsList = new ArrayList<Event>();
		List<Event> previousEventsList = new ArrayList<Event>();
		Iterable<Event> allEvents;

		if(Keyword != null)
			allEvents = eventService.search(Keyword);
		else
			allEvents = eventService.findAll();

		model.addAttribute("events", allEvents);

		for (Event currentEvent : allEvents) {
			if (currentEvent.getDate().compareTo(LocalDate.now()) < 0)
				previousEventsList.add(currentEvent);
			else
				upcomingEventsList.add(currentEvent);
		}

		model.addAttribute("upcoming_events", upcomingEventsList);
		model.addAttribute("previous_events", previousEventsList);
		model.addAttribute("allTweets",twitterService.getTimeLine());
		model.addAttribute("mapbox_access_token", env.getProperty("mapbox.accessToken"));

		return "events/index";
	}

	@RequestMapping(value="/eventpage/{id}", method=RequestMethod.GET)
	public String getEventDetails(@PathVariable("id") long id, Model model) {

		Event event = eventService.findOne(id);

		model.addAttribute("event", event);
		model.addAttribute("mapbox_access_token", env.getProperty("mapbox.accessToken"));

		return "events/eventpage";
	}


	@RequestMapping(value = "/{id}", method = RequestMethod.POST)
	public String setTweet(@PathVariable("id") long id,@RequestParam("tweet") String tweet,RedirectAttributes redirect) throws TwitterException{
		  boolean istweetset = twitterService.setTwitterInstance(tweet);
		  if(istweetset) {
			  redirect.addFlashAttribute("pass_message","Tweet was sent: "+ tweet);
		  }else {
			  redirect.addFlashAttribute("fail_message","Tweet sent failed");
		  }
		  String str_id = Long.toString(id);
		  return "redirect:/events/eventpage/" + str_id;
	  }

	  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
	  public String updateE(@PathVariable("id") long id, Model model)
	  {
		Optional<Event> event = eventService.findById(id);

		if (event.isPresent()) {
			Event e = event.get();
			model.addAttribute("event", e);
			model.addAttribute("venues", venueService.findAll());

	      return "events/update";
		}

		return "redirect:/events";
	  }

	@RequestMapping(value="update/{id}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String updateEvent(@PathVariable("id") long id, Model model,
							  @Valid @ModelAttribute("event") Event e, BindingResult bindingResult,
							  RedirectAttributes redirect){

	  if(bindingResult.hasErrors()){
        StringBuilder errString = new StringBuilder();
        for(FieldError error : bindingResult.getFieldErrors())
          errString.append("Invalid ").append(error.getField())
            .append(" input: ").append(error.getRejectedValue()).append("\n");

        redirect.addFlashAttribute("errorMsg", errString);
        return "redirect:/events/" + id + "?";
      }

      eventService.save(e);
      return "redirect:/events";
	}

	@GetMapping("/addEvent")
	public String newEvent(Model model) {
		model.addAttribute("venues", venueService.findAll());
		model.addAttribute("newEvent", new Event());
		return "events/addEvent";
	}

	@DeleteMapping("/{id}")
	public String deleteEvent(Model model, @PathVariable("id") long id, RedirectAttributes redirect)
	{
		Optional<Event> optionalEvent = eventService.findById(id);

		//validate venue found
		if(optionalEvent.isEmpty()) {
			redirect.addFlashAttribute("deleteEventErrorMsg",
				"Could not find event by specified Id.");
			return "redirect:/events";
		}

		eventService.deleteById(id);
		return "redirect:/events";
	}

	@PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String createNewEvent(Model model, @Valid @ModelAttribute("newEvent") Event e, BindingResult bindingResult) {
		if(bindingResult.hasErrors()){
			StringBuilder errString = new StringBuilder();
			for(FieldError error : bindingResult.getFieldErrors())
				errString.append("Invalid ").append(error.getField())
					.append(" input: ").append(error.getRejectedValue()).append("\n");

			model.addAttribute("errorMsg", errString);
			model.addAttribute("event", e);
			model.addAttribute("venues", venueService.findAll());
			return "events/addEvent";
		}

		eventService.save(e);

		return "redirect:/events";
	}
}
