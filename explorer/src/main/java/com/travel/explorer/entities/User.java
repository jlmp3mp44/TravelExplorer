package com.travel.explorer.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints =
    {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long userId;

  @Email
  @NotBlank
  @Column(name = "email")
  private String email;

  @NotNull
  @Size(min = 3, max = 22, message = "username should be between 3 and 22 characters")
  @Column(name = "username")
  private String username;

  @NotNull
  @Size(min = 8, max = 126, message = "username should be between 8 and 126 characters")
  @Column(name = "password")
  private String password;

  @ManyToMany(cascade = {CascadeType.MERGE},
              fetch = FetchType.EAGER)
  @JoinTable(name = "user_roles",
  joinColumns = @JoinColumn(name = "user_id"),
  inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles =  new HashSet<>();

  public User(String username, String email, String password, Set<Role> roles) {
    this.email = email;
    this.username = username;
    this.password = password;
    this.roles = roles;
  }

}
