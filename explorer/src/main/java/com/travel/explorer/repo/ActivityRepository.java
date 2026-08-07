package com.travel.explorer.repo;

import com.travel.explorer.entities.Activity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

  @Query("SELECT DISTINCT a FROM Activity a JOIN FETCH a.places WHERE a.id = :id")
  Optional<Activity> findByIdWithPlaces(@Param("id") Long id);
}
