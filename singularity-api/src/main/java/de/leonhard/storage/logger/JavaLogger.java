package de.leonhard.storage.logger;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaLogger implements LoggerModel {
    private final Logger logger;

    public JavaLogger(Logger logger) {
        this.logger = logger;
    }
    @Override
    public void printMessage(Object object) {
        logger.info(String.valueOf(object));
    }

    @Override
    public void printMessage(String message) {
        logger.info(message);
    }

    @Override
    public void sendWarning(String message) {
        logger.warning(message);
    }

    @Override
    public void sendError(String message) {
        logger.severe(message);
    }

    @Override
    public void printStackTrace(Exception exception) {
        logger.log(Level.WARNING, "An error occurred.", exception);
    }
}
