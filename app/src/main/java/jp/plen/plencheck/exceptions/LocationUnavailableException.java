package jp.plen.plencheck.exceptions;

public class LocationUnavailableException extends ScenographyException {
    public LocationUnavailableException() {
    }

    public LocationUnavailableException(String detailMessage) {
        super(detailMessage);
    }

    public LocationUnavailableException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public LocationUnavailableException(Throwable throwable) {
        super(throwable);
    }
}
