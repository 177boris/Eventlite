package uk.ac.man.cs.eventlite.config.data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
import uk.ac.man.cs.eventlite.dao.*;
import uk.ac.man.cs.eventlite.entities.*;

@Component
@Profile({ "default", "test" })
public class InitialDataLoader implements ApplicationListener<ContextRefreshedEvent> {

	private final static Logger log = LoggerFactory.getLogger(InitialDataLoader.class);

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PermissionRepository permissionRepository;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

		//fill database with sample venue and event data if not populated already
		if (eventService.count() == 0) {
			// Build and save initial models here.
			populateDatabaseWithTestData();
		} else log.info("Database already populated. Skipping data initialization.");

		//initialise permissions, roles and sample users if not initialised already
		if(permissionRepository.count() == 0){
			Permission adminPermission = getOrCreatePermission("ADMINISTRATOR_PERMISSION");
			Permission viewEventsPermission = getOrCreatePermission("VIEW_EVENTS_PERMISSION");
			Permission manageEventsPermission = getOrCreatePermission("MANAGE_EVENTS_PERMISSION");

			List<Permission> adminPermissions = Arrays.asList(adminPermission);
			List<Permission> organiserPermissions = Arrays.asList(viewEventsPermission, manageEventsPermission);
			List<Permission> seekerPermissions = Arrays.asList(viewEventsPermission);

			getOrCreateRole("ROLE_ADMINISTRATOR", adminPermissions);
			getOrCreateRole("ROLE_EVENT_ORGANISER", organiserPermissions);
			getOrCreateRole("ROLE_EVENT_SEEKER", seekerPermissions);
		}	else log.info("Permissions and Roles already Initialised. Skipping.");

		if(userRepository.count() == 0){
			Role adminRole = roleRepository.findByName("ROLE_ADMINISTRATOR");
			Role organiserRole = roleRepository.findByName("ROLE_EVENT_ORGANISER");

			persistUser("organiser", encoder.encode("organiser"), Arrays.asList(organiserRole));
			persistUser("Rob", encoder.encode("Haines"), Arrays.asList(adminRole));
			persistUser("Caroline", encoder.encode("Jay"), Arrays.asList(adminRole));
			persistUser("Markel", encoder.encode("Vigo"), Arrays.asList(adminRole));
			persistUser("Mustafa", encoder.encode("Mustafa"), Arrays.asList(adminRole));
		}	else log.info("Users already Initialised. Skipping.");
	}

	@Transactional
	void persistUser(String username, String encodedPassword, List<Role> roles){
		User user = new User();
		user.setUsername(username);
		user.setPassword(encodedPassword);
		user.setRoles(roles);
		user.setEnabled(true);
		userRepository.save(user);
	}

	@Transactional
	Permission getOrCreatePermission(String name){
		Permission permission = permissionRepository.findByName(name);
		if(permission == null){
			permission = new Permission(name);
			permissionRepository.save(permission);
		}
		return permission;
	}

	@Transactional
	Role getOrCreateRole(String name, Collection<Permission> permissions){
		Role role = roleRepository.findByName(name);
		if(role == null){
			role = new Role(name);
			role.setPermissions(permissions);
			roleRepository.save(role);
		}
		return role;
	}

	private void populateDatabaseWithTestData(){
		//initialise some default real-life venues from Manchester
		//load static co-ordinates to not use up api calls
		Venue kilburnVenue = makeNewVenue("Kilburn LF31", "Kilburn Building, Oxford Rd, Manchester",
			"M13 9PL", 53.467524, -2.233915, 200);
		Venue stopfordVenue = makeNewVenue("Stopford Theatre 1", "Stopford Building, 99 Oxford Rd, Manchester",
			"M13 9PG", 53.464101, -2.230575, 313);
		Venue roscoeVenue = makeNewVenue("Roscoe Theatre 1", "Roscoe Building, Oxford Rd, Manchester",
			"M13 9PL", 53.466888, -2.231565, 470);

		//store all venues
		List<Venue> venues = Arrays.asList(kilburnVenue, stopfordVenue, roscoeVenue);
		//persist venues
		venues.forEach(v -> venueService.save(v));

		//adjust how many venues and events to generate
		final int NUMBER_OF_EVENTS = 20;
		Event[] sampleEvent = new Event[NUMBER_OF_EVENTS];

		//now generate and persist events, referencing above venues
		for(int e=0; e<NUMBER_OF_EVENTS; e++){
			String eventName = String.format("Event %d", e);
			//get random index from venues
			int venueIndex = getRandomNumber(0, venues.size());
			//between two months ago and two months ahead
			int dateOffset = getRandomNumber(-60, 61);
			sampleEvent[e] = newSimpleEvent(eventName, venues.get(venueIndex), dateOffset);
			//persist
			eventService.save(sampleEvent[e]);
		}
	}

	//returns a new Event with an offset of specified days (may be negative, offsets from current local time)
	//   sets the argument specified Venue for this new event
	private Event newSimpleEvent(String name, Venue venue, int initialDayOffset){
		Event event = new Event();
		LocalDate date = LocalDate.now();
		LocalTime time = LocalTime.now()
			.plusHours(getRandomNumber(0,24))
			.plusMinutes(getRandomNumber(0,59))
			.withSecond(0);
		event.setName(name);
		event.setDate(date.plusDays(initialDayOffset));
		event.setTime(time);
		event.setDescription("Event for " + venue.getName());
		event.setVenue(venue);
		return event;
	}

	//returns a new venue with specified name, address, postcode, map co-ordinates and capacity
	private Venue makeNewVenue(String name, String address, String postCode,
							   double latitude, double longitude, int capacity){
		Venue venue = new Venue();
		venue.setName(name);
		venue.setAddress(address);
		venue.setPostcode(postCode);
		venue.setCapacity(capacity);

		//validate coordinates, if coordinates invalid, randomly generate them
		if((latitude >= -180 && latitude <= 180) && (longitude >= -180 && longitude <=180)){
			venue.setLatitude(latitude);
			venue.setLongitude(longitude);
		}
		else{
			venue.setLatitude(getRandomNumber(-180, 181));
			venue.setLongitude(getRandomNumber(-180, 181));
		}
		return venue;
	}

	//return random uppercase letter
	private char getRandomChar(){
		String abc = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		return abc.charAt(getRandomNumber(0,abc.length()));
	}

	//random int number from specified range, min inclusive, max exclusive
	private int getRandomNumber(int min, int max){
		//dont need to worry about thread safety for intial data
		return (int) (Math.random() * (max - min) + min);
	}
}
