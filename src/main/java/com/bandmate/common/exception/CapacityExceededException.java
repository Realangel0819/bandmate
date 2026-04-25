package com.bandmate.common.exception;

public class CapacityExceededException extends InvalidRequestException {
    public CapacityExceededException(String message) {
        super(message);
    }
}
