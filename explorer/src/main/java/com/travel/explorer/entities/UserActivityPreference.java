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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Per-user override for an activity on a trip (e.g. public trip customized by each traveler).
 * Does not change the canonical {@link Activity} row used as the default itinerary.
 */
@Entity
@Table(
    name = "user_activity_preferences",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"user_id", "activity_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityPreference {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "activity_id", nullable = false)
  private Activity activity;

  @Enumerated(EnumType.STRING)
  @Column(name = "change_reason", nullable = false, length = 32)
  private ActivityChangeReason changeReason;

  /** Mock substitute for now; later can point to a recommended alternative. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "replacement_place_id", nullable = false)
  private Place replacementPlace;
}
