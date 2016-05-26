package com.z.api.zapi;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class DriveZAPI implements ZAPIClient.ZAPI {
    class GoogleDocument {
            GoogleDocument(ZAPIClient client) {
                this.client = client;
            }

            public boolean hasLoaded = false;
            private ZAPIClient client;
            private DriveId driveId = null;
            private DriveFile file;
            String content;

            public void setID(String id){
                driveId = DriveId.decodeFromString(id);
            }
            public String getID(){
                return driveId.encodeToString();
            }

            public void LoadFromId(String id) {
                setID(id);
                Load();
            }
            public void Load() {
                checkConnection();
                hasLoaded = false;
                new Thread() {
                    @Override
                    public void run() {
                        file = driveId.asDriveFile();
                        file.open(client.getClient(), DriveFile.MODE_READ_ONLY, null)
                                .setResultCallback(new ResultCallback<DriveContentsResult>() {
                                                       @Override
                                                       public void onResult(@NonNull DriveContentsResult result) {
                                                           if (!result.getStatus().isSuccess()) {
                                                               onLoadFailure();
                                                           } else {
                                                               DriveContents contents = result.getDriveContents();
                                                               BufferedReader reader = new BufferedReader(new InputStreamReader(contents.getInputStream()));
                                                               StringBuilder builder = new StringBuilder();
                                                               String line;
                                                               try {
                                                                   while ((line = reader.readLine()) != null) {
                                                                       builder.append(line);
                                                                   }
                                                               } catch (Exception e) {
                                                                   onLoadFailure();
                                                               }
                                                               content = builder.toString();
                                                               contents.discard(client.getClient());
                                                               onLoadSuccess(content);
                                                               hasLoaded = true;
                                                           }
                                                       }
                                                   }
                                );
                    }
                }.start();
            }

            public void Write(String content){
                this.content = content;
                Write();
            }
            public void WriteToId(String id, String content){
                this.content = content;
                WriteToId(id);
            }

            public void Write(){
                checkConnection();
                if (driveId == null){
                    Toaster("This file has no id! You must either write to an id, or call generateDocument()");
                    return;
                }
                new Thread() {
                    @Override
                    public void run() {
                        file = driveId.asDriveFile();
                        file.open(client.getClient(), DriveFile.MODE_WRITE_ONLY, null).setResultCallback(new ResultCallback<DriveContentsResult>() {
                            @Override
                            public void onResult(DriveContentsResult result) {
                                if (!result.getStatus().isSuccess()) {
                                    onWriteFailure();
                                    return;
                                }
                                DriveContents driveContents = result.getDriveContents();

                                try {
                                    ParcelFileDescriptor parcelFileDescriptor = driveContents.getParcelFileDescriptor();
                                    FileOutputStream fileOutputStream = new FileOutputStream(parcelFileDescriptor
                                            .getFileDescriptor());
                                    Writer writer = new OutputStreamWriter(fileOutputStream);
                                    System.out.println(content);
                                    writer.write(content);
                                    writer.close();
                                }catch(Exception e){
                                    onWriteFailure();
                                }
                                driveContents.commit(client.getClient(), null).setResultCallback(new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(Status result) {
                                        if (!result.getStatus().isSuccess()){
                                            onWriteFailure();
                                        }
                                        onWriteSuccess();
                                    }
                                });
                            }
                        });
                    }
                }.start();
            }

            public void WriteToId(String id){
                setID(id);
                Write();
            }

            public void generateDocument(final String fileTitle, final boolean isStarred) {
                new Thread() {
                    @Override
                    public void run() {
                        Drive.DriveApi.newDriveContents(client.getClient())
                                .setResultCallback(new ResultCallback<DriveContentsResult>() {
                                    @Override
                                    public void onResult(@NonNull DriveContentsResult driveContentsResult) {
                                        Drive.DriveApi.getRootFolder(client.getClient())
                                                .createFile(client.getClient(), new MetadataChangeSet.Builder()
                                                        .setTitle(fileTitle)
                                                        .setMimeType("text/plain")
                                                        .setStarred(isStarred).build(), driveContentsResult.getDriveContents())
                                                .setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
                                                    @Override
                                                    public void onResult(@NonNull DriveFolder.DriveFileResult driveFileResult) {
                                                        setID(driveFileResult.getDriveFile().getDriveId().encodeToString());
                                                        onGenerateSuccess(getID());
                                                    }
                                                });

                                    }
                                });
                    }
                }.start();
            }

            public void onGenerateSuccess(String id) {}

            public void onGenerateFailure() {}

            public void onLoadSuccess(String content) {}

            public void onLoadFailure() {}

            public void onWriteSuccess() {}

            public void onWriteFailure() {}

            private void checkConnection(){
                if (!client.getClient().isConnected()){
                    Toaster("You are not connected to the google client, attempting a connection now...");
                    client.getClient().connect();
                }
            }
    }

    //Build instructions
    @Override
    public GoogleApiClient.Builder buildInstructions(GoogleApiClient.Builder originalBuilder, Context context, FragmentActivity fragmentActivity) {
        originalBuilder.addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE);
        return originalBuilder;
    }

    //Cleans up debugging and error handling
    private void Toaster(String result) {
        System.out.println("DriveZAPI Toaster: " + result);
    }
}
