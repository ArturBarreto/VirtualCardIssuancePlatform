package com.nium.cardplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Standard API error response")
public class ApiErrorResponse {

  @Schema(
          description = "Timestamp of the error",
          example = "2025-07-24T17:45:31.123"
  )
  private LocalDateTime timestamp;

  @Schema(
          description = "HTTP status code",
          example = "400"
  )
  private int status;

  @Schema(
          description = "HTTP status reason",
          example = "Bad Request"
  )
  private String error;

  @Schema(
          description = "Detailed error message",
          example = "Spend amount must be greater than zero."
  )
  private String message;

  // Getters and setters

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
