package com.nium.cardplatform.exception;

public class InvalidTransactionAmountException extends RuntimeException {
    public InvalidTransactionAmountException(String message) {
      super(message);
    }
}
