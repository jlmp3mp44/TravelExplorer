package com.travel.explorer.security.responce;

import lombok.Data;

@Data
public class MessageResponce {

  private String message;
  public MessageResponce(String message) {
    this.message =  message;
  }
}
