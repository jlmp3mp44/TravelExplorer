package com.travel.explorer.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "trips")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trip {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "trip_id")
  private Long id;

  @NotBlank(message = "can not be blank")
  @Size(min = 3, max = 120, message = "Title must contain between 3 and 120 characters")
  @Column(name = "title", length = 120)
  private String title;

  @Column(name = "description")
  private String desc;
  @NotNull
  @Column(name = "start_date")
  private LocalDate startDate;
  @NotNull
  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name= "budget")
  @NotNull
  private Integer budget;

  /** Pace / density chosen when creating the trip (optional until clients send it). */
  @Enumerated(EnumType.STRING)
  @Column(name = "trip_intensity", length = 16)
  private TripIntensity intensity;

  /** When true, the itinerary is visible to others; each user can still keep personal activity overrides. */
  @Column(name = "is_public", nullable = false)
  @ColumnDefault("true")
  private Boolean isPublic = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_user_id")
  private User owner;

  @ManyToMany
  @JoinTable(
      name = "trip_cities",
      joinColumns = @JoinColumn(name = "trip_id"),
      inverseJoinColumns = @JoinColumn(name = "city_id")
  )
  private Set<City> cities = new HashSet<>();

  /** Selected Google Places type codes (whitelist); drives nearby search and is stored for the trip. */
  @ElementCollection
  @CollectionTable(name = "trip_place_categories", joinColumns = @JoinColumn(name = "trip_id"))
  @Column(name = "category_code", length = 80)
  @OrderColumn(name = "sort_idx")
  private List<String> categories = new ArrayList<>();

  @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Day> days = new ArrayList<>();

  public Set<Country> getCountries() {
    return cities.stream()
        .map(City::getCountry)
        .collect(Collectors.toSet());
  }

  @OneToMany(mappedBy = "trip",
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private List<TripRating> ratings = new ArrayList<>();

  @OneToMany(mappedBy = "trip",
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private List<TripItineraryPlaceAdjustment> tripItineraryPlaceAdjustments = new ArrayList<>();
}
