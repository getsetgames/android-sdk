package com.playhaven.test_app_android_mopub.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;
import com.playhaven.android.Placement;
import com.playhaven.android.PlacementListener;
import com.playhaven.android.PlayHaven;
import com.playhaven.android.PlayHavenException;
import com.playhaven.android.req.OpenRequest;
import com.playhaven.android.req.RequestListener;
import com.playhaven.android.view.FullScreen;
import com.playhaven.android.view.PlayHavenView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Reference: https://github.com/mopub/mopub-android-sdk/wiki/Custom-Events
 * Created by jeremyberman on 10/22/14.
 */
public class PlayhavenCustomEventInterstitial extends CustomEventInterstitial implements RequestListener, PlacementListener {

    private static final String LOG_TAG = "PlayhavenCustomEventInterstitial";

    private Context mContext;
    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private Placement mPlacement;
    private boolean mPlacementIsLoaded = false;

    /**
     * Override the main loadIntersitial method. Calls required PlayHaven OpenRequest
     *
     * @param context of the request
     * @param customEventInterstitialListener the main event listener
     * @param localExtras
     * @param serverExtras data returned from the Custom Event Class Data in Custom Native Network
     *                     section of the Mopub Dashboard
     */
    @Override
    protected void loadInterstitial(Context context, CustomEventInterstitialListener customEventInterstitialListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        mContext = context;
        mCustomEventInterstitialListener = customEventInterstitialListener;

        if (!(context instanceof Activity)) {
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        String secret = serverExtras.get("secret");
        String token = serverExtras.get("token");
        String placementTag = serverExtras.get("placement");
        Log.i(LOG_TAG, "Loading instertitial for Playhaven placement " + placementTag + ". (secret: " + secret + " token: " + token + ")");

        if (secret != null && token != null && placementTag != null) {
            try {
                Log.i(LOG_TAG, "Making open request...");
                PlayHaven.configure(context, token, secret);
                mPlacement = new Placement(placementTag);
                OpenRequest openRequest = new OpenRequest();
                openRequest.setResponseHandler(this);
                openRequest.send(context);
            } catch (PlayHavenException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(LOG_TAG, "Secret, token, or placement not properly set. Please make sure that these values are set in the Mopub Dashboard.");
        }
    }

    /**
     * will show the interstitial after the placement is loaded
     */
    @Override
    protected void showInterstitial() {
        if (mPlacementIsLoaded) {
            Log.i(LOG_TAG, "Showing interstitial...");
            mContext.startActivity(FullScreen.createIntent(mContext, mPlacement));
            mCustomEventInterstitialListener.onInterstitialShown();
        }
    }

    @Override
    protected void onInvalidate() {
        //
    }

    /**
     * preloads the placement if there's no error in the request
     *
     * @param context of the request
     * @param s the response
     */
    @Override
    public void handleResponse(Context context, String s) {
        Log.i(LOG_TAG, "Open response: " + s);
        try {
            JSONObject response = new JSONObject(s);
            if (response.isNull("errobj")) {
                mPlacement.setListener(this);
                mPlacement.preload(context);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param context of the request
     * @param e the exception
     */
    @Override
    public void handleResponse(Context context, PlayHavenException e) {
        Log.i(LOG_TAG, e.getMessage());
    }

    /**
     * called when content has successfully preloaded
     *
     * @param placement
     */
    @Override
    public void contentLoaded(Placement placement) {
        mPlacementIsLoaded = true;
        mCustomEventInterstitialListener.onInterstitialLoaded();
    }

    /**
     * called when content has failed to load
     *
     * @param placement
     * @param e
     */
    @Override
    public void contentFailed(Placement placement, PlayHavenException e) {
        mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.UNSPECIFIED);
    }

    /**
     * called when interstitial has been dismissed
     *
     * @param placement
     * @param dismissType
     * @param bundle
     */
    @Override
    public void contentDismissed(Placement placement, PlayHavenView.DismissType dismissType, Bundle bundle) {
        mCustomEventInterstitialListener.onInterstitialDismissed();
    }
}
