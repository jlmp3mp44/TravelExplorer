package com.travel.explorer.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "cities")
@Data
// Keep relation fields out of generated equals/hashCode: Hibernate uses hashCode while loading
// PersistentSet, and traversing bidirectional relations here can cause re-entrant collection loads.
@ToString(exclude = {"country", "trips"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class City {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  private Long id;

  private String name;

  @ManyToOne
  @JoinColumn(name = "country_id")
  private Country country;

  @ManyToMany(mappedBy = "cities")
  private Set<Trip> trips =  new HashSet<>();

}
