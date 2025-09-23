package com.travel.explorer.repo;

import com.travel.explorer.entities.Place;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepo extends JpaRepository<Place, Long> {

  Optional<Place> findByTitle(String title);
}
