package com.travel.explorer.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "activity")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Activity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Order of this activity within its day (0-based). */
  @Column(name = "sort_order", nullable = false)
  @ColumnDefault("0")
  private Integer sortOrder = 0;

  @ManyToOne
  @JoinColumn(name = "day_id")
  private Day day;
  @ManyToMany()
  @JoinTable(joinColumns = @JoinColumn(name = "activity_id"),
      inverseJoinColumns = @JoinColumn(name = "place_id"),
      name = "activity_places")
  private List<Place> places =  new ArrayList<>();
  private LocalDateTime startTime;
  private LocalDateTime endTime;

}
