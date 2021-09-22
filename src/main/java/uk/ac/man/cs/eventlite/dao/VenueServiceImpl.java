package uk.ac.man.cs.eventlite.dao;

import java.util.List;

import java.util.Optional;

import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.ac.man.cs.eventlite.entities.Venue;

@Service
public class VenueServiceImpl implements VenueService {


	@Autowired
	private VenueRepository venueRepository;

	@Autowired
	private Environment env;

	@Override
	public long count() {
		return venueRepository.count();
	}

	@Override
	public Venue findOne(long id) {
		return venueRepository.findById(id).orElse(null);
	}

	@Override
	public Venue findByName(String name) {
		return venueRepository.findByName(name);
	}

	@Override
	public boolean geocodeVenue(final Venue v) throws InterruptedException{
		String address = v.getAddress();
		String postcode = v.getPostcode();

		MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder()
			.accessToken(env.getProperty("mapbox.accessToken"))
			.query(String.format("%s, %s", address, postcode))
			.build();

		mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
			@Override
			public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
				List<CarmenFeature> results = response.body().features();
				Point venueMapPoint;
				if(results.size() > 0){
					venueMapPoint = results.get(0).center();
					v.setLatitude(venueMapPoint.latitude());
					v.setLongitude(venueMapPoint.longitude());
				}
				else{
					//coordinates not found, set latitude and longitude to invalid values to signal failure
					v.setLatitude(Double.NaN);
					v.setLongitude(Double.NaN);
				}
			}

			@Override
			public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
				throwable.printStackTrace();
			}
		});
		Thread.sleep(1000L);

		return !Double.isNaN(v.getLatitude()) && !Double.isNaN(v.getLongitude());
	}

	@Override
	public long countEventsById(long id) {
		return venueRepository.findAllEventsCount(id);
	}

	@Override
	public Iterable<Venue> findAll() {
		return venueRepository.findAllByOrderByNameAsc();
	}

	@Override
	public Iterable<Venue> findAllByOrderByEventsVenue_Id() {
		return venueRepository.findAllByOrderByEventsVenue_Id();
	}

	public Optional<Venue> findById(Long id) {
		return(venueRepository.findById(id));
	}

	public Venue save(Venue venue) {
		return venueRepository.save(venue);
	}

	public Optional<Venue> findById(long id) {
		return venueRepository.findById(id);
	}

	@Override
	public Iterable<Venue> findAllMatching(String keyword) { return venueRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword); }

	@Override
	public Iterable<Venue> search(String Keyword){
		if(Keyword != null) {
			return venueRepository.findAllByNameContainingIgnoreCaseOrderByNameAsc(Keyword);
		}
		return venueRepository.findAllByOrderByNameAsc();
	}

	@Override
	public void deleteById(long id) {
		venueRepository.deleteById(id);
	}

}
