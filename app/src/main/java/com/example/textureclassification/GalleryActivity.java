package com.example.textureclassification;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.textureclassification.Retrofit.IUploadAPI;
import com.example.textureclassification.Retrofit.RetrofitClient;
import com.example.textureclassification.Utils.Common;
import com.example.textureclassification.Utils.IUploadCallBacks;
import com.example.textureclassification.Utils.ProgressRequestBody;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.net.URISyntaxException;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GalleryActivity extends AppCompatActivity implements IUploadCallBacks {

    private static final int PICK_FILE_REQUEST = 1;

    IUploadAPI mService;
    Uri selectedFileUri;
    Button btnUploadGal;
    ImageView imgViewGal;

    ProgressDialog dialog;

    private IUploadAPI getAPIUpload(){
        return RetrofitClient.getClient().create(IUploadAPI.class);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        Toast.makeText(GalleryActivity.this, "Tap icon to select picture", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(GalleryActivity.this, "You should accept permission", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();

        //Create mService
        mService = getAPIUpload();

        //InitView
        btnUploadGal =  findViewById(R.id.btn_upload_gal);
        imgViewGal = findViewById(R.id.image_view_gal);

        //event
        imgViewGal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFile();
            }
        });

        btnUploadGal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadFile();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK);
        {
            if (requestCode == PICK_FILE_REQUEST)
            {
                if (data != null)
                {
                    selectedFileUri = data.getData();
                    if(selectedFileUri != null && !selectedFileUri.getPath().isEmpty())
                        imgViewGal.setImageURI(selectedFileUri);
                    else
                        Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    private void uploadFile() {
        if (selectedFileUri != null) {
            dialog = new ProgressDialog(GalleryActivity.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMessage("Uploading...");
            dialog.setIndeterminate(false);
            dialog.setMax(100);
            dialog.setCancelable(false);
            dialog.show();

            File file = null;
            try {
                file = new File(Common.getFilePath(this, selectedFileUri));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            if (file != null) {
                final ProgressRequestBody requestBody = new ProgressRequestBody(file, this);

                final MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestBody);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mService.uploadFile(body)
                                .enqueue(new Callback<String>() {
                                    @Override
                                    public void onResponse(Call<String> call, Response<String> response) {
                                        dialog.dismiss();
                                        // >RetrofitClient.java
                                        String image_processed_link = new StringBuilder("http://192.168.1.3:5000/" +
                                                response.body().replace("\"", "")).toString();

                                        Picasso.get().load(image_processed_link)
                                                .into(imgViewGal);

                                        Toast.makeText(GalleryActivity.this, "Identified!!!", Toast.LENGTH_SHORT).show();

                                    }

                                    @Override
                                    public void onFailure(Call<String> call, Throwable t) {
                                        dialog.dismiss();
                                        Toast.makeText(GalleryActivity.this, "" + t.getMessage(), Toast.LENGTH_SHORT).show();

                                    }
                                });

                    }
                }).start();
            }
        }else
        {
            Toast.makeText(this, "Cannot upload this file!!!", Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //Uri uri = Uri.parse(String.format("/testImage/", Environment.getExternalStorageDirectory().getPath()));
        //intent.setDataAndType(uri, "image/*");
        intent.setType("image/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }


    @Override
    public void onProgressUpdate(int percent) {
        dialog.setProgress(percent);

    }

}



