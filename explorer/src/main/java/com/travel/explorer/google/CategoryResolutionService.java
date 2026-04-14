package com.travel.explorer.google;

import com.travel.explorer.entities.Category;
import com.travel.explorer.place.PlaceInterestType;
import com.travel.explorer.repo.CategoryRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryResolutionService {

  private final CategoryRepository categoryRepository;

  public CategoryResolutionService(CategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
  }

  @Transactional
  public List<Category> resolveFromGoogleTypes(List<String> types) {
    if (types == null || types.isEmpty()) {
      return new ArrayList<>();
    }
    Set<String> unique = new LinkedHashSet<>();
    for (String t : types) {
      if (t != null) {
        String trimmed = t.trim();
        if (!trimmed.isEmpty() && PlaceInterestType.isAllowedCode(trimmed)) {
          unique.add(trimmed);
        }
      }
    }
    List<Category> out = new ArrayList<>();
    for (String name : unique) {
      out.add(findOrCreate(name));
    }
    return out;
  }

  private Category findOrCreate(String name) {
    return categoryRepository
        .findByName(name)
        .orElseGet(
            () -> {
              try {
                return categoryRepository.save(new Category(null, name));
              } catch (DataIntegrityViolationException e) {
                return categoryRepository
                    .findByName(name)
                    .orElseThrow(() -> e);
              }
            });
  }
}
