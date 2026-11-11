package de.leonhard.storage.logger;

public class PrinterLogger implements LoggerModel {

    @Override
    public void printMessage(Object object) {
        System.err.println(object);
    }

    @Override
    public void printMessage(String message) {
        System.err.println(message);
    }

    @Override
    public void sendWarning(String message) {
        System.err.println(message);
    }

    @Override
    public void sendError(String message) {
        System.err.println(message);
    }

    @Override
    public void printStackTrace(Exception exception) {
        exception.printStackTrace();
    }
}
