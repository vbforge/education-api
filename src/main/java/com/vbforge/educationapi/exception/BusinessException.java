package com.vbforge.educationapi.exception;

// for business rule violations (e.g. student already enrolled)
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}