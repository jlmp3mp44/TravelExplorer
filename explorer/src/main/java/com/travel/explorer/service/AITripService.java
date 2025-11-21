package com.travel.explorer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.explorer.payload.trip.AITripRequest;
import com.travel.explorer.payload.trip.AITripResponce;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AITripService {

  @Autowired
  private ModelMapper modelMapper;

  @Autowired
  private final String openRouterApiKey;

  public String buildPrompt(AITripRequest request) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("Generate a trip with those parameters: ");
    prompt.append(request.getNumberOfDays()).append(" Days in trip");

    if (request.getNumberOfDays() != null ) {
      prompt.append(request.getNumberOfDays()).append(" Days in trip");
    }

    if (request.getStartDate() != null && request.getEndDate() != null) {
      prompt.append(" from ").append(request.getStartDate()).append(" to ").append(request.getEndDate());
    }

    if (request.getDesiredCountries() != null && !request.getDesiredCountries().isEmpty()) {
      prompt.append(". Countries: ").append(String.join(", ", request.getDesiredCountries()));
    }

    if (request.getInterests() != null && !request.getInterests().isEmpty()) {
      prompt.append(". User interests: ").append(String.join(", ", request.getInterests()));
    }

    if (request.getBudget() != null) {
      prompt.append(". Budget: ").append(request.getBudget()).append(" USD");
    }

    prompt.append(". Provide a detailed itinerary in JSON format with fields: tripTitle, days (with day number, location, list of activities), estimatedBudget.");
    System.out.println(prompt);
    return prompt.toString();
  }

  public String generateTripPlan(String prompt) {
    OkHttpClient client = new OkHttpClient();
    MediaType JSON = MediaType.get("application/json; charset=utf-8");

    try {
      JSONObject json = new JSONObject();
      json.put("model", "gpt-4o-mini");
      JSONArray messages = new JSONArray();
      messages.put(new JSONObject().put("role", "system")
          .put("content", "You are a travel expert. Plan a detailed itinerary in JSON format."));
      messages.put(new JSONObject().put("role", "user").put("content", prompt));
      json.put("messages", messages);

      RequestBody body = RequestBody.create(json.toString(), JSON);
      Request requestObj = new Request.Builder()
          .url("https://openrouter.ai/api/v1/chat/completions")
          .header("Authorization", "Bearer " + openRouterApiKey)
          .header("HTTP-Referer", "http://localhost:8080")
          .header("X-Title", "Travel Explorer")
          .post(body)
          .build();

      try (Response response = client.newCall(requestObj).execute()) {
        if (!response.isSuccessful()) {
          throw new RuntimeException("OpenRouter API error: " + response.code() + " " + response.message());
        }

        String responseBody = response.body().string();
        JSONObject responseJson = new JSONObject(responseBody);
        return responseJson
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content");
      }

    } catch (Exception e) {
      throw new RuntimeException("Error generating trip plan with AI", e);
    }
  }

  public AITripResponce parseTripPlan(String rawResponse) {
    try {
      int startIndex = rawResponse.indexOf("{");
      int endIndex = rawResponse.lastIndexOf("}");
      if (startIndex < 0 || endIndex <= startIndex) {
        throw new JSONException("No valid JSON object found in response");
      }

      String cleanedJson = rawResponse.substring(startIndex, endIndex + 1);

      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> parsedMap = mapper.readValue(cleanedJson, new TypeReference<>() {});

      return modelMapper.map(parsedMap, AITripResponce.class);

    } catch (Exception e) {
      throw new RuntimeException("Error parsing AI trip plan", e);
    }
  }

}
