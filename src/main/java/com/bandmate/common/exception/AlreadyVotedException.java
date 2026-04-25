package com.bandmate.common.exception;

public class AlreadyVotedException extends DuplicateException {
    public AlreadyVotedException(String message) {
        super(message);
    }
}
