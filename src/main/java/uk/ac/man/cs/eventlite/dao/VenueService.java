package uk.ac.man.cs.eventlite.dao;

import java.util.Optional;

import uk.ac.man.cs.eventlite.entities.Venue;

public interface VenueService {

	public long count();

	public long countEventsById(long id);

	public Venue save(Venue venue);

	public Venue findOne(long id);

	public Venue findByName(String name);

	public boolean geocodeVenue(final Venue v) throws InterruptedException;

	public Iterable<Venue> findAll();

	public Iterable<Venue> findAllByOrderByEventsVenue_Id();

	public Optional<Venue> findById(long id);

//	public Iterable<Venue> findOrderedVenues();

	public Iterable<Venue> findAllMatching(String keyword);

	public Iterable<Venue> search(String Keyword);
	
	public void deleteById(long id);

}
