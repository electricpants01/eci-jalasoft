package com.jumptech.networking;

import androidx.fragment.app.FragmentActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class ProgressCallback<T> implements Callback<T> {

    private FragmentActivity context;

    public ProgressCallback(FragmentActivity context) {
        this.context = context;
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        onResponse(response);
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        if (context != null && context.isFinishing()) {
            return;
        }
        onFailure(t);
    }

    public abstract void onResponse(Response<T> response);

    public abstract void onFailure(Throwable t);
}
