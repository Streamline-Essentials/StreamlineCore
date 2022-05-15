package net.streamline.api.base.modules;

public class UnknownDependencyException extends RuntimeException {
    private static final long serialVersionUID = 5721389371901775895L;

    public UnknownDependencyException(final Throwable throwable) {super(throwable);}
    public UnknownDependencyException(final String message) {super(message);}
    public UnknownDependencyException(final Throwable throwable, final String message) {super(message, throwable);}
    public UnknownDependencyException() {}
}
