package com.travel.explorer.entities.embeddable;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeopleAlsoSearch {
  private String category;
  private String title;
  private int reviewsCount;
  private double totalScore;
}
