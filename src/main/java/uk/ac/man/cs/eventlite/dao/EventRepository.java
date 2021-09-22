package uk.ac.man.cs.eventlite.dao;

import org.springframework.data.repository.CrudRepository;

import uk.ac.man.cs.eventlite.entities.Event;
import java.time.LocalDate;
import java.time.LocalTime;

public interface EventRepository extends CrudRepository<Event, Long>{

	public Iterable<Event> findAllByOrderByDateAscNameAsc();

	public Iterable<Event> findAllByNameContainingIgnoreCaseOrderByDateAscNameAsc(String Keyword);


	public Iterable<Event> findTop3ByVenue_IdAndDateAfterOrDateEqualsAndTimeGreaterThanOrderByDateAscTimeAsc(
		long venue_id, LocalDate date, LocalDate date2, LocalTime time);
}

