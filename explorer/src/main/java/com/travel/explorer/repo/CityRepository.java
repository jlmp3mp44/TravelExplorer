package com.travel.explorer.repo;

import com.travel.explorer.entities.City;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {

  List<City> findByCountry_IdOrderByNameAsc(Long countryId);

  @EntityGraph(attributePaths = "country")
  @Query("select c from City c where c.id in :ids")
  List<City> findAllByIdInWithCountry(@Param("ids") Collection<Long> ids);
}
