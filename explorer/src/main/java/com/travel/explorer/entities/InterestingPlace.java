package com.travel.explorer.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Place a user has flagged as personally interesting. Used to surface saved places in the
 * trip-creation flow regardless of selected categories.
 */
@Entity
@Table(
    name = "interesting_places",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_interesting_places_user_place",
            columnNames = {"user_id", "place_id"})
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterestingPlace {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(optional = false)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  /** Optional saved-context city (used for cityId-priority matching). */
  @ManyToOne
  @JoinColumn(name = "city_id")
  private City city;

  /** Optional saved-context country (used as fallback when no city was selected). */
  @ManyToOne
  @JoinColumn(name = "country_id")
  private Country country;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
