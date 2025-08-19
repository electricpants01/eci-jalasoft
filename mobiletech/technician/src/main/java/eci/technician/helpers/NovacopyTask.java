package eci.technician.helpers;

public class NovacopyTask<T> {
    private final OnCompleteListener<T> onCompleteListener;

    public NovacopyTask(OnCompleteListener<T> onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    public interface OnCompleteListener<T> {
        void onComplete(boolean success, String errors, T result);
    }
}
