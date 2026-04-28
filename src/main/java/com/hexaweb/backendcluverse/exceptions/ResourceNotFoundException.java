package com.hexaweb.backendcluverse.exceptions;

/**
 * Exception levée lorsqu'une ressource n'est pas trouvée
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
