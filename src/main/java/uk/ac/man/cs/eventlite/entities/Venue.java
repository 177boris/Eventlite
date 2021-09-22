package uk.ac.man.cs.eventlite.entities;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Id;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;
import javax.validation.constraints.Min;
import javax.persistence.OneToMany;
import javax.persistence.GeneratedValue;
import javax.validation.constraints.NotNull;
import javax.persistence.GenerationType;

@Entity
@Table(name = "venues")
public class Venue {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@NotNull(message = "Venue must have a name")
	@Size(max = 255, message = "Venue name must be less than 256 characters")
	private String name;

	@NotNull(message = "Venue must have an address")
	@Size(max = 299, message = "Venue address must have less than 300 characters")
	private String address;

	@NotNull(message = "Venue must have a postcode")
	private String postcode;

	@NotNull(message = "Venue must have a positive capacity")
	@Min(0)
	private int capacity;

	@NotNull(message = "Address not found")
	@Max(value = 180)
	@Min(value = -180)
	private double longitude;

	@NotNull(message = "Address not found")
	@Max(value = 90)
	@Min(value = -90)
	private double latitude;

	@JsonIgnore
	@OneToMany(mappedBy="venue")
	private List<Event> events;

	public Venue() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAddress() {
		return address;
	}

	public String getPostcode() {
		return postcode;
	}

	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public int getCapacity() {
		return capacity;
	}

	public double getLatitude(){
		return latitude;
	}

	public void setLatitude(double latitude){
		this.latitude = latitude;
	}

	public double getLongitude(){
		return longitude;
	}

	public void setLongitude(double longitude){
		this.longitude = longitude;
	}

	public List<Event> getEvents() {
		return events;
	}
}
