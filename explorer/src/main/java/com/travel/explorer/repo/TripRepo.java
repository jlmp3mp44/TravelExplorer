package com.travel.explorer.repo;

import com.travel.explorer.entities.Trip;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepo
    extends JpaRepository<Trip, Long>, JpaSpecificationExecutor<Trip> {

  /** Trips whose {@code owner.userId} matches; empty page if none (e.g. legacy rows without owner). */
  Page<Trip> findByOwner_UserId(Long ownerUserId, Pageable pageable);

  /** Subset with {@code isPublic == true} (for listing another user's trips). */
  Page<Trip> findByOwner_UserIdAndIsPublicTrue(Long ownerUserId, Pageable pageable);

  @Query(
      value =
          """
          SELECT t.trip_id FROM trips t
          LEFT JOIN (
            SELECT trip_id, AVG(stars) AS avg_stars, COUNT(*) AS rating_cnt
            FROM trip_ratings
            GROUP BY trip_id
          ) s ON s.trip_id = t.trip_id
          ORDER BY COALESCE(s.avg_stars, 0) DESC, t.trip_id ASC
          """,
      countQuery = "SELECT COUNT(*) FROM trips",
      nativeQuery = true)
  Page<Long> findAllTripIdsOrderByAverageRatingDesc(Pageable pageable);

  @Query(
      value =
          """
          SELECT t.trip_id FROM trips t
          LEFT JOIN (
            SELECT trip_id, AVG(stars) AS avg_stars, COUNT(*) AS rating_cnt
            FROM trip_ratings
            GROUP BY trip_id
          ) s ON s.trip_id = t.trip_id
          ORDER BY COALESCE(s.avg_stars, 0) ASC, t.trip_id ASC
          """,
      countQuery = "SELECT COUNT(*) FROM trips",
      nativeQuery = true)
  Page<Long> findAllTripIdsOrderByAverageRatingAsc(Pageable pageable);

  @Query(
      value =
          """
          SELECT t.trip_id FROM trips t
          LEFT JOIN (
            SELECT trip_id, COUNT(*) AS rating_cnt
            FROM trip_ratings
            GROUP BY trip_id
          ) s ON s.trip_id = t.trip_id
          ORDER BY COALESCE(s.rating_cnt, 0) DESC, t.trip_id ASC
          """,
      countQuery = "SELECT COUNT(*) FROM trips",
      nativeQuery = true)
  Page<Long> findAllTripIdsOrderByRatingCountDesc(Pageable pageable);

  @Query(
      value =
          """
          SELECT t.trip_id FROM trips t
          LEFT JOIN (
            SELECT trip_id, COUNT(*) AS rating_cnt
            FROM trip_ratings
            GROUP BY trip_id
          ) s ON s.trip_id = t.trip_id
          ORDER BY COALESCE(s.rating_cnt, 0) ASC, t.trip_id ASC
          """,
      countQuery = "SELECT COUNT(*) FROM trips",
      nativeQuery = true)
  Page<Long> findAllTripIdsOrderByRatingCountAsc(Pageable pageable);

  @Query("SELECT t FROM Trip t LEFT JOIN FETCH t.owner WHERE t.id IN :ids")
  List<Trip> findAllWithOwnerByIdIn(@Param("ids") Collection<Long> ids);

  @Query(
      value =
          """
          SELECT trip_id, photo_url, place_id
          FROM (
            SELECT d.trip_id,
                   p.photo_url,
                   p.place_id,
                   ROW_NUMBER() OVER (
                     PARTITION BY d.trip_id
                     ORDER BY d.date NULLS LAST, a.sort_order, ap.place_id
                   ) AS rn
            FROM days d
            JOIN activity a ON a.day_id = d.id
            JOIN activity_places ap ON ap.activity_id = a.id
            JOIN places p ON p.place_id = ap.place_id
            WHERE d.trip_id IN (:tripIds)
              AND p.google_place_id IS NOT NULL
              AND TRIM(p.google_place_id) <> ''
          ) ranked
          WHERE rn = 1
          """,
      nativeQuery = true)
  List<Object[]> findCoverPhotosByTripIds(@Param("tripIds") Collection<Long> tripIds);
}
