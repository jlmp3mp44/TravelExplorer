package com.travel.explorer.repo;

import com.travel.explorer.entities.AppRole;
import com.travel.explorer.entities.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findByRoleName(AppRole appRole);
}
