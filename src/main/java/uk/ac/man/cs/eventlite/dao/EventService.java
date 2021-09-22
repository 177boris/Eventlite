package uk.ac.man.cs.eventlite.dao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import uk.ac.man.cs.eventlite.entities.Event;
public interface EventService {

	public long count();

	public Iterable<Event> findAll();

	public Iterable<Event> search(String Keyword);

	public void save(Event event);

	public void update(Event event);

	public Event findOne(long id);

	Optional<Event> findById(long id);

	public void deleteById(Long eventId);

	public Iterable<Event> findNext3(long venueId, LocalDate currentDate, LocalTime currentTime);

}
