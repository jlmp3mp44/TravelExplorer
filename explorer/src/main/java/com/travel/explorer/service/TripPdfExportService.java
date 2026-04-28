package com.travel.explorer.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.travel.explorer.entities.Activity;
import com.travel.explorer.entities.Day;
import com.travel.explorer.entities.Place;
import com.travel.explorer.entities.Trip;
import java.io.ByteArrayOutputStream;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/** Builds a simple itinerary PDF for a trip (no template engine). */
@Service
public class TripPdfExportService {

  public byte[] buildTripPdf(Trip trip) {
    Document document = new Document();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      PdfWriter.getInstance(document, out);
      document.open();

      document.add(
          new Paragraph(
              trip.getTitle() != null ? trip.getTitle() : "Trip",
              FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
      document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 11)));

      if (trip.getDesc() != null && !trip.getDesc().isBlank()) {
        document.add(new Paragraph(trip.getDesc(), FontFactory.getFont(FontFactory.HELVETICA, 11)));
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 9)));
      }

      document.add(
          new Paragraph(
              "Dates: "
                  + trip.getStartDate()
                  + " – "
                  + trip.getEndDate()
                  + "  |  Budget: "
                  + trip.getBudget(),
              FontFactory.getFont(FontFactory.HELVETICA, 11)));
      if (trip.getIntensity() != null) {
        document.add(
            new Paragraph(
                "Intensity: " + trip.getIntensity(),
                FontFactory.getFont(FontFactory.HELVETICA, 11)));
      }
      document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 11)));

      if (trip.getDays() != null) {
        List<Day> sortedDays =
            trip.getDays().stream().sorted(Comparator.comparing(Day::getDate)).toList();
        for (Day day : sortedDays) {
          document.add(
              new Paragraph(
                  "Day: " + day.getDate(),
                  FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
          if (day.getActivities() != null) {
            List<Activity> sortedActs =
                day.getActivities().stream()
                    .sorted(Comparator.comparing(Activity::getSortOrder))
                    .toList();
            for (Activity a : sortedActs) {
              StringBuilder line = new StringBuilder("  • ");
              if (a.getPlaces() != null && !a.getPlaces().isEmpty()) {
                Place p = a.getPlaces().get(0);
                line.append(p.getTitle() != null ? p.getTitle() : "Place");
                if (p.getAddress() != null && !p.getAddress().isBlank()) {
                  line.append(" — ").append(p.getAddress());
                }
              } else {
                line.append("(No place)");
              }
              if (Boolean.TRUE.equals(a.getUserAdded())) {
                line.append(" [added by you]");
              }
              document.add(new Paragraph(line.toString(), FontFactory.getFont(FontFactory.HELVETICA, 11)));
            }
          }
          document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 9)));
        }
      }

      document.close();
    } catch (DocumentException e) {
      throw new IllegalStateException("Failed to build trip PDF", e);
    }
    return out.toByteArray();
  }
}
