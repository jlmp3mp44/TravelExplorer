package com.travel.explorer.repo;

import com.travel.explorer.entities.UserActivityPreference;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivityPreferenceRepository
    extends JpaRepository<UserActivityPreference, Long> {

  Optional<UserActivityPreference> findByUser_UserIdAndActivity_Id(Long userId, Long activityId);

  @Query(
      "SELECT DISTINCT p FROM UserActivityPreference p "
          + "JOIN FETCH p.replacementPlace "
          + "JOIN FETCH p.activity "
          + "WHERE p.user.userId = :userId AND p.activity.id IN :activityIds")
  List<UserActivityPreference> findForUserAndActivities(
      @Param("userId") Long userId, @Param("activityIds") Collection<Long> activityIds);
}
