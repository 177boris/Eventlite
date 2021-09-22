package uk.ac.man.cs.eventlite.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uk.ac.man.cs.eventlite.entities.Venue;

public interface VenueRepository extends CrudRepository<Venue, Long>  {

	public Iterable<Venue> findAllByOrderByNameAsc();

	public Iterable<Venue> findByNameContainingIgnoreCaseOrderByNameAsc(String keyword);

	public Venue findByName(String name);

	@Query("select size(v.events) from Venue v where v.id=:venueID")
	public int findAllEventsCount(@Param("venueID") long venueId);


	@Query("SELECT v FROM Venue as v JOIN Event as e ON v.id = e.venue.id " +
	"GROUP BY v.id " +
	"ORDER BY count(e.venue.id)  DESC")
	public Iterable<Venue> findAllByOrderByEventsVenue_Id();

	@Query("SELECT v FROM Venue v WHERE v.name LIKE %?1%")
	public Iterable<Venue> search(String Keyword);

	public Iterable<Venue> findAllByNameContainingIgnoreCaseOrderByNameAsc(String string);
}
