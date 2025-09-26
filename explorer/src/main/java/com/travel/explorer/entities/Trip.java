package com.travel.explorer.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(name = "trips")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trip {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "can not be blank")
  @Size(min = 3, max = 50, message = "Title must contains between 3 and 50 characters")
  private String title;

  private String desc;
  @NotNull
  private Date startDate;
  @NotNull
  private Date endDate;

  @ManyToMany(cascade = CascadeType.REMOVE)
  @JoinTable(joinColumns = @JoinColumn(name = "trip_id"),
  inverseJoinColumns = @JoinColumn(name = "place_id"),
  name = "trips_places")
  private List<Place> places =  new ArrayList<>();


}
