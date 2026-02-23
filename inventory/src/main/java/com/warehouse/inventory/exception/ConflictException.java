package com.warehouse.inventory.exception;
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}