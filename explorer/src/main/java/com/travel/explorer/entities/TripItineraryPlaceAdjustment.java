package com.travel.explorer.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Audit of manual itinerary edits (add/remove activity). Separate from {@link UserActivityPreference}
 * (replace flow).
 */
@Entity
@Table(name = "trip_itinerary_place_adjustments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripItineraryPlaceAdjustment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "trip_id", nullable = false)
  private Trip trip;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false, length = 16)
  private ItineraryAdjustmentKind kind;

  @Enumerated(EnumType.STRING)
  @Column(name = "reason", nullable = false, length = 32)
  private ActivityChangeReason reason;

  /** For REMOVE: id of deleted activity (stored before delete). */
  @Column(name = "removed_activity_id")
  private Long removedActivityId;

  /** For ADD: id of created activity. */
  @Column(name = "created_activity_id")
  private Long createdActivityId;
}
