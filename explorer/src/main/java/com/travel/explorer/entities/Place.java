package com.travel.explorer.entities;

import com.travel.explorer.entities.embeddable.Location;
import com.travel.explorer.entities.embeddable.OpenHours;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "places")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Place {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "place_id")
  private Long id;

  @NotBlank(message = "can not be blank")
  @Size(min = 3, max = 50, message = "Title should contains between 3 and 50 characters")
  @Column(name = "title")
  private String title;
  @Column(name = "description")
  private String desc;
  @ManyToMany
  @JoinTable(
      name = "place_category",
      joinColumns = @JoinColumn(name = "place_id"),
      inverseJoinColumns = @JoinColumn(name = "category_id")
  )
  private List<Category> categories;
  @Column(name = "address")
  private String address;
  @Column(name = "neighborhood")
  private String neighborhood;
  @Column(name = "city")
  private String city;
  @Column(name = "street")
  private String street;
  @Column(name = "state")
  private String state;
  @Column(name = "phone")
  private String phone;
  @Embedded
  private Location location;
  @Column (name = "totalScore")
  private Integer totalScore;
  @Column (name = "permanentlyClosed")
  private Boolean permanentlyClosed;
  @Column(name = "temporarilyClosed")
  private Boolean temporarilyClosed;
  @ElementCollection
  @CollectionTable(name = "place_open_hours", joinColumns = @JoinColumn(name = "place_id"))
  private List<OpenHours> openHours;


  @ManyToMany
  @JoinTable(
      name = "place_placesPeopleSearch",
      joinColumns = @JoinColumn(name = "place_id"),
      inverseJoinColumns = @JoinColumn(name = "related_place_id")
  )
  private List<Place> placesPeopleSearches;
  @Column(name = "photoUrl")
  private String photoUrl;
  @ManyToMany(mappedBy = "places")
  private List<Activity> activities =  new ArrayList<>();
}
