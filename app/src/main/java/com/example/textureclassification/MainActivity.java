package com.example.textureclassification;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    boolean doubleBackToExitPressedOnce = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnSelectSource = findViewById(R.id.btn_start);
        btnSelectSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Select Source");
                builder.setItems(R.array.select_source, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 0){
                            Intent i = new Intent(MainActivity.this, GalleryActivity.class);
                            startActivity(i);
                            //Toast.makeText(getApplicationContext(), "Gallery selected", Toast.LENGTH_LONG).show();

                        }else if(which == 1){
                            Intent i = new Intent(MainActivity.this, CameraActivity.class);
                            startActivity(i);
                            //Toast.makeText(getApplicationContext(), "Camera selected", Toast.LENGTH_LONG).show();

                        }else{
                            //theres an error in what was selected
                            Toast.makeText(getApplicationContext(), "Hmmm... : " + which + "?", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                builder.show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }


}