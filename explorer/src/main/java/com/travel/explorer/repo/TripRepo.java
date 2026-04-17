package com.travel.explorer.repo;

import com.travel.explorer.entities.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepo  extends JpaRepository<Trip, Long> {

  /** Trips whose {@code owner.userId} matches; empty page if none (e.g. legacy rows without owner). */
  Page<Trip> findByOwner_UserId(Long ownerUserId, Pageable pageable);

  /** Subset with {@code isPublic == true} (for listing another user's trips). */
  Page<Trip> findByOwner_UserIdAndIsPublicTrue(Long ownerUserId, Pageable pageable);
}
