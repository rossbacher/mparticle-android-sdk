package com.mparticle.networking;

import android.content.Context;
import android.content.SharedPreferences;

import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

import java.io.IOException;
import java.net.HttpURLConnection;

public abstract class BaseNetworkConnection {
    private SharedPreferences mPreferences;

    public abstract HttpURLConnection makeUrlRequest(MParticleBaseClientImpl.Endpoint endpoint, HttpURLConnection connection, String payload, boolean identity) throws IOException;

    protected BaseNetworkConnection(Context context) {
        this.mPreferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
    }

    protected BaseNetworkConnection(SharedPreferences sharedPreferences) {
        this.mPreferences = sharedPreferences;
    }

    public void setNextAllowedRequestTime(HttpURLConnection connection) {
        long throttle = NetworkConnection.DEFAULT_THROTTLE_MILLIS;
        if (connection != null) {
            //most HttpUrlConnectionImpl's are case insensitive, but the interface
            //doesn't actually restrict it so let's be safe and check.
            String retryAfter = connection.getHeaderField("Retry-After");
            if (MPUtility.isEmpty(retryAfter)) {
                retryAfter = connection.getHeaderField("retry-after");
            }
            try {
                long parsedThrottle = Long.parseLong(retryAfter) * 1000;
                if (parsedThrottle > 0) {
                    throttle = Math.min(parsedThrottle, NetworkConnection.MAX_THROTTLE_MILLIS);
                }
            } catch (NumberFormatException nfe) {
                Logger.debug("Unable to parse retry-after header, using default.");
            }
        }

        long nextTime = System.currentTimeMillis() + throttle;
        setNextRequestTime(nextTime);
    }

    public void setNextRequestTime(long timeMillis) {
        mPreferences.edit().putLong(Constants.PrefKeys.NEXT_REQUEST_TIME, timeMillis).apply();
    }
}
