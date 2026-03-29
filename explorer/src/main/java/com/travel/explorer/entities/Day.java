package com.travel.explorer.entities;

import com.travel.explorer.entities.Activity;
import com.travel.explorer.entities.Place;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "days")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Day {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @ManyToOne
  @JoinColumn(name = "trip_id", nullable = false)
  private Trip trip;
  @OneToMany(mappedBy = "day")
  private List<Activity> activities = new ArrayList<>();

}
