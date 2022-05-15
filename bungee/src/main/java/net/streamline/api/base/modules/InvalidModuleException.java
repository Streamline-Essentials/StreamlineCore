package net.streamline.api.base.modules;

public class InvalidModuleException extends Exception {
    private static final long serialVersionUID = -8242141640709409544L;

    public InvalidModuleException( final Throwable cause){super(cause);}
    public InvalidModuleException() {}
    public InvalidModuleException( final String message, final Throwable cause){super(message, cause);}
    public InvalidModuleException( final String message){super(message);}
}