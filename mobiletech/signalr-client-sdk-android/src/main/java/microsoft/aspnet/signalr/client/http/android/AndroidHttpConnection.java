/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package microsoft.aspnet.signalr.client.http.android;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import microsoft.aspnet.signalr.client.*;
import microsoft.aspnet.signalr.client.http.*;
import microsoft.aspnet.signalr.client.http.HttpConnectionFuture.ResponseCallback;

/**
 * Android HttpConnection implementation, based on AndroidHttpClient and
 * AsyncTask for async operations
 */
public class AndroidHttpConnection implements HttpConnection {
    private Logger mLogger;

    /**
     * Initializes the AndroidHttpConnection
     *
     * @param logger logger to log activity
     */
    public AndroidHttpConnection(Logger logger) {
        if (logger == null) {
            throw new IllegalArgumentException("logger");
        }

        mLogger = logger;
    }

    @Override
    public HttpConnectionFuture execute(final Request request, final ResponseCallback responseCallback) {

        mLogger.log("Create new AsyncTask for HTTP Connection", LogLevel.Verbose);

        final HttpConnectionFuture future = new HttpConnectionFuture();

        final RequestTask requestTask = new RequestTask() {
            InputStream mResponseStream;
            HttpURLConnection connection;

            @Override
            protected Void doInBackground(Void... voids) {
                if (request == null) {
                    future.triggerError(new IllegalArgumentException("request"));
                }

                try {
                    URL url = new URL(request.getUrl());
                    connection = (HttpURLConnection) url.openConnection();

                    connection.setRequestProperty("http.agent", Platform.getUserAgent());

                    mResponseStream = null;
                    URI uri;

                    mLogger.log("Create an Android-specific request", LogLevel.Verbose);
                    request.log(mLogger);

                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(30000);
                    connection.setRequestMethod(request.getVerb());

                    if (request.getContent() != null) {
                        connection.setDoOutput(true);

                        DataOutputStream stream = new DataOutputStream(connection.getOutputStream());
                        stream.writeBytes(request.getContent());
                    }

                    Map<String, String> headers = request.getHeaders();

                    for (String key : headers.keySet()) {
                        connection.addRequestProperty(key, headers.get(key));
                    }

                    mLogger.log("Execute the HTTP Request", LogLevel.Verbose);

                    try {
                        mResponseStream = connection.getInputStream();
                    } catch (SocketTimeoutException timeoutException) {
                        closeStreamAndClient();
                        mLogger.log("Timeout executing request: " + timeoutException.getMessage(), LogLevel.Information);

                        future.triggerTimeout(timeoutException);

                        return null;
                    }

                    mLogger.log("Request executed", LogLevel.Verbose);


                    responseCallback.onResponse(new StreamResponse(mResponseStream, connection.getResponseCode(), connection.getHeaderFields()));
                    future.setResult(null);
                    closeStreamAndClient();
                } catch (Exception e) {
                    closeStreamAndClient();
                    mLogger.log("Error executing request: " + e.getMessage(), LogLevel.Critical);

                    future.triggerError(e);
                }

                return null;
            }

            protected void closeStreamAndClient() {
                if (mResponseStream != null) {
                    try {
                        mResponseStream.close();
                    } catch (IOException ignored) {
                    }
                }

                if (connection != null) {
                    connection.disconnect();
                }
            }
        };

        future.onCancelled(new Runnable() {

            @Override
            public void run() {
                AsyncTask<Void, Void, Void> cancelTask = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        requestTask.closeStreamAndClient();
                        return null;
                    }
                };

                executeTask(cancelTask);
            }
        });

        executeTask(requestTask);

        return future;
    }

    @SuppressLint("NewApi")
    private void executeTask(AsyncTask<Void, Void, Void> task) {
        // If it's running with Honeycomb or greater, it must execute each
        // request in a different thread
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            task.execute();
        }
    }

    /**
     * Internal class to represent an async operation that can close a stream
     */
    private abstract class RequestTask extends AsyncTask<Void, Void, Void> {

        /**
         * Closes the internal stream and http client, if they exist
         */
        abstract protected void closeStreamAndClient();
    }
}
