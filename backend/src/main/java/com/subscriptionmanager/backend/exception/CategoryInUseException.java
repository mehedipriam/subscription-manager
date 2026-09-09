package com.subscriptionmanager.backend.exception;

public class CategoryInUseException extends RuntimeException {
    public CategoryInUseException() {
        super("This category is still assigned to one or more subscriptions");
    }
}
