package com.travel.explorer.excpetions;

import com.travel.explorer.security.responce.ValidationErrorsResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MyGlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ValidationErrorsResponse> myMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    List<String> errors =
        e.getBindingResult().getAllErrors().stream()
            .map(
                err ->
                    err instanceof FieldError fieldError
                        ? fieldError.getDefaultMessage()
                        : err.getDefaultMessage())
            .filter(msg -> msg != null && !msg.isBlank())
            .collect(Collectors.toList());
    return new ResponseEntity<>(new ValidationErrorsResponse(errors), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<String> myResourceNotFoundException(ResourceNotFoundException e){
    String responce = e.getMessage();
    return new ResponseEntity<String>(responce, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(APIException.class)
  public ResponseEntity<String> myAPIException(APIException e) {
    String message = e.getMessage();
    return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
  }


}
