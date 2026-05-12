package com.travel.explorer.repo;

import com.travel.explorer.entities.InterestingPlace;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterestingPlaceRepository extends JpaRepository<InterestingPlace, Long> {

  List<InterestingPlace> findAllByUser_UserIdOrderByCreatedAtDesc(Long userId);

  List<InterestingPlace> findAllByUser_UserIdAndCity_IdIn(
      Long userId, Collection<Long> cityIds);

  List<InterestingPlace> findAllByUser_UserIdAndCountry_Id(Long userId, Long countryId);

  Optional<InterestingPlace> findByUser_UserIdAndPlace_Id(Long userId, Long placeId);

  void deleteByIdAndUser_UserId(Long id, Long userId);
}
