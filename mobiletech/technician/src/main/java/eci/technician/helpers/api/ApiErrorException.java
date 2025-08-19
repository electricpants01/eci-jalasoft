package eci.technician.helpers.api;

public class ApiErrorException extends Exception {
    private int code;

    public ApiErrorException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
