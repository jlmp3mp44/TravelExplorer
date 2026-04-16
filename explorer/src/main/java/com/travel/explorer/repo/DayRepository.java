package com.travel.explorer.repo;

import com.travel.explorer.entities.Day;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DayRepository extends JpaRepository<Day, Integer> {

  Optional<Day> findByIdAndTrip_Id(Integer dayId, Long tripId);
}
