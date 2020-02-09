package com.example.textureclassification;


import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CameraActivity extends AppCompatActivity implements IUploadCallBacks {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;

    IUploadAPI mService;
    Uri selectedFileUri;

    Button btnUpload;
    ImageView imageView;

    String currentPhotoPath;
    Context context = this;

    ProgressDialog dialog;

    private IUploadAPI getAPIUpload(){
        return RetrofitClient.getClient().create(IUploadAPI.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        Toast.makeText(CameraActivity.this, "Tap icon to take picture", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(CameraActivity.this, "You should accept permission", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();

        //Create mService
        mService = getAPIUpload();

        //InitView
        imageView = findViewById(R.id.image_view_cam);
        btnUpload = findViewById(R.id.btn_upload_cam);

        //event
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadFile();
            }
        });

    }

    private void uploadFile() {
        if (selectedFileUri != null) {
            dialog = new ProgressDialog(CameraActivity.this);
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
                                                .into(imageView);

                                        Toast.makeText(CameraActivity.this, "Identified!!!", Toast.LENGTH_SHORT).show();

                                    }

                                    @Override
                                    public void onFailure(Call<String> call, Throwable t) {
                                        dialog.dismiss();
                                        Toast.makeText(CameraActivity.this, "" + t.getMessage(), Toast.LENGTH_SHORT).show();

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

    //1
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.textureclassification.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile( imageFileName, ".jpg", storageDir );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        Log.e("createImageFile", "" + image);

        return image;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {

            galleryAddPic();
            setPic();

            BitmapDrawable draw = (BitmapDrawable) imageView.getDrawable();
            Bitmap bitmap = draw.getBitmap();

            FileOutputStream outStream = null;

            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            dir.mkdirs();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = String.format("%s","IMG_"+ timeStamp + ".jpg");
            File outFile = new File(dir, fileName);

            try{
                outStream = new FileOutputStream(outFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                outStream.flush();
                outStream.close();

            }catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            File f = outFile;//File f = new File(currentPhotoPath);
            selectedFileUri = Uri.fromFile(f);//selectedFileUri = Uri.fromFile(f);


        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
        bitmap = Bitmap.createScaledBitmap(bitmap, 800, 800, false);

        //rotatebitmap
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap bitmapRotate = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        imageView.setImageBitmap(bitmapRotate);

        File fdelete = new File(currentPhotoPath);
        if (fdelete.exists()) {
            fdelete.delete();
        }

    }

    @Override
    public void onProgressUpdate(int percent) {
        dialog.setProgress(percent);

    }
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
