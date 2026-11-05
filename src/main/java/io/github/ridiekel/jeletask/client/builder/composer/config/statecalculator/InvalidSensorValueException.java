package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

/**
 * The central sent a value that is an error code rather than a measurement. The message is dropped, so the last
 * good state stays published.
 */
public class InvalidSensorValueException extends RuntimeException {
    public InvalidSensorValueException(String message) {
        super(message);
    }
}
