package uk.ac.man.cs.eventlite.dao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@Service
@Transactional
public class EventServiceImpl implements EventService {

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private VenueService venueService;

	@Override
	public long count() {
		return eventRepository.count();
	}

	@Override
	public Iterable<Event> findAll() {
		return eventRepository.findAllByOrderByDateAscNameAsc();
	}

	@Override
	public void save(Event event) {
		eventRepository.save(event);
	}

	public Event findOne(long id) {
		return findById(id).orElse(null);
	}

	@Override
	public void update(Event event) {
		eventRepository.save(event);
	}

	@Override
	public Optional<Event> findById(long id) {
		return eventRepository.findById(id);
	}

	@Override
	public Iterable<Event> search(String Keyword){
		if(Keyword != null) {
			return eventRepository.findAllByNameContainingIgnoreCaseOrderByDateAscNameAsc(Keyword);
		}
		return this.findAll();
	}

	@Override
	public void deleteById(Long eventid) {
		eventRepository.deleteById(eventid);
	}

	@Override
	public Iterable<Event> findNext3(long venueId, LocalDate currentDate, LocalTime currentTime) {
		Optional<Venue> venue = venueService.findById(venueId);
		if(venue.isEmpty())
			return null;

		return eventRepository.findTop3ByVenue_IdAndDateAfterOrDateEqualsAndTimeGreaterThanOrderByDateAscTimeAsc(
			venueId, currentDate, currentDate, currentTime);
	}

}

