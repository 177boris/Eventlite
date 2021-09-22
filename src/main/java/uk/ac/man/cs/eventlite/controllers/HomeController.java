package uk.ac.man.cs.eventlite.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

@Controller
public class HomeController {

    @Autowired
    private EventService eventService;

    @Autowired
    private VenueService venueService;

    @RequestMapping(method = RequestMethod.GET, value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home(Model model){
        final int DISPLAYED_EVENT_COUNT = 3;
        final int DISPLAYED_VENUE_COUNT = 3;
        ArrayList<Event> upcomingEvents = new ArrayList<>();
        ArrayList<Venue> popularVenues = new ArrayList<>();

        //add next three upcoming events to model
        Iterator<Event> events = eventService.findAll().iterator();
        while(upcomingEvents.size() != DISPLAYED_EVENT_COUNT && events.hasNext()){
            Event event = events.next();
            if(event.getDate().compareTo(LocalDate.now()) > 0){
                upcomingEvents.add(event);
            }
        }

        //add three most popular venues to model
        Iterator<Venue> venues = venueService.findAllByOrderByEventsVenue_Id().iterator();
        HashMap<String, Long> venueNameEventCountMap = new HashMap<>();

        while(popularVenues.size() != DISPLAYED_VENUE_COUNT && venues.hasNext()) {
            Venue venue = venues.next();
            popularVenues.add(venue);
            venueNameEventCountMap.put(venue.getName(), venueService.countEventsById(venue.getId()));
        }

        model.addAttribute("upcoming_events", upcomingEvents);
        model.addAttribute("popular_venues", popularVenues);
        model.addAttribute("venue_count_map", venueNameEventCountMap);

        return "/home/index";
    }
}
