package com.z.api.zapi;

import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.common.data.Freezable;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ZAPIClient zClient;
    DriveZAPI zDrive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        zClient = new ZAPIClient();
        zDrive = new DriveZAPI();

        zClient.AddAPI(zDrive);
        zClient.registerClient(this,this);
        //final Context c = this;

        //Run's code when the zClient connects
        zClient.addCallback(zClient.new onCreateCallback(){
            @Override
            public void onCreate() {

                //Create a new GoogleDocument
                DriveZAPI.GoogleDocument myDoc = zDrive.new GoogleDocument(zClient){

                    //Sets up code to be run when the document is successfully generated
                    @Override
                    public void onGenerateSuccess(String id) {
                        content = "Some content here";
                        this.Write();
                    }

                    //Sets up code to be run when the document is successfully written to
                    @Override
                    public void onWriteSuccess() {
                        System.out.println("Made a file with id: " + getID());
                    }
                };

                //Generates document
                myDoc.generateDocument("My File",true);
            }
        });
    }

    //Example of how to write to a known id
    public void writeToId(String id, String content){
        DriveZAPI.GoogleDocument someDocument = zDrive.new GoogleDocument(zClient); //create a new class to represent the document
        someDocument.content = content; //add content to the document
        someDocument.WriteToId(id);
    }

    //Example of how to load from a known id asynchronously
    public String loadFromId(String id){
        DriveZAPI.GoogleDocument someDocument = zDrive.new GoogleDocument(zClient);

        someDocument.LoadFromId(id);
        while (!someDocument.hasLoaded);

        return someDocument.content;
    }
}
