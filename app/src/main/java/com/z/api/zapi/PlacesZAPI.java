package com.z.api.zapi;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.Places.*;
import com.google.android.gms.location.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

public class PlacesZAPI implements ZAPIClient.ZAPI{

    public class placesRequest{
        placesRequest(final ZAPIClient client, final Context context){
            new Thread(){
                @Override
                public void run() {
                    //if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
                        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi.getCurrentPlace(client.getClient(), null);
                        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                            @Override
                            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                                ArrayList<PlaceLikelihood> arrayPlace = new ArrayList<PlaceLikelihood>();
                                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                                    arrayPlace.add(placeLikelihood);
                                }
                                onReturn(arrayPlace);
                                likelyPlaces.release();
                            }
                        });
                    //}
                }
            }.start();
        }

        public void onReturn(ArrayList<PlaceLikelihood> places){
        }
    }

    //Build instructions
    @Override
    public GoogleApiClient.Builder buildInstructions(GoogleApiClient.Builder originalBuilder, Context context, FragmentActivity fragmentActivity) {
        originalBuilder.addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API);
        return originalBuilder;
    }

    //Cleans up debugging and error handling
    private void Toaster(String result) {
        System.out.println("PlacesZAPI Toaster: " + result);
    }
}
