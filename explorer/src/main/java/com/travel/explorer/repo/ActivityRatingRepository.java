package com.travel.explorer.repo;

import com.travel.explorer.entities.ActivityRating;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRatingRepository extends JpaRepository<ActivityRating, Long> {

  Optional<ActivityRating> findByUser_UserIdAndActivity_Id(Long userId, Long activityId);

  @Query("SELECT AVG(r.stars) FROM ActivityRating r WHERE r.activity.id = :activityId")
  Optional<Double> averageStarsByActivityId(@Param("activityId") Long activityId);

  long countByActivity_Id(Long activityId);
}
