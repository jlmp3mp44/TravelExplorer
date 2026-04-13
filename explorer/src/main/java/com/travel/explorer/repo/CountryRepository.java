package com.travel.explorer.repo;

import com.travel.explorer.entities.Country;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {

  List<Country> findAllByOrderByNameAsc();
}
