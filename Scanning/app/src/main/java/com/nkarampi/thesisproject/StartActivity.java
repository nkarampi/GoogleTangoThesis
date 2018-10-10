package com.nkarampi.thesisproject;

import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.nkarampi.thesisproject.database.DatabaseContract;
import com.nkarampi.thesisproject.model.Book;

import static com.nkarampi.thesisproject.MainActivity.LIBRARY_REQUEST;
import static com.nkarampi.thesisproject.MainActivity.SCAN_MODE;

/*
    This class is the first Activity that gets called. It shows some information about the app
    and it's purpose and has a button to start the BarcodeActivity.

    Created by: Nikolaos Karampinas
    Date: 9/2018
    Email: nkarampi@csd.auth.gr
 */
public class StartActivity extends AppCompatActivity{
    private static final String TAG = StartActivity.class.getSimpleName();
    public static final int BARCODE_REQUEST = 1;
    public static final int RESULT_SAVED = 2;
    public static final String IS_LIBRARY = "isLibrary";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activty_start);

        //Test
        //startActivity(new Intent(StartActivity.this,MainActivity.class));
    }

    /**
     * This method is the onClick method for the scan library's barcode button.
     * It starts the @BarcodeActivity with the @BARCODE_MODE set to LIBRARY_BARCODE via intent.
     * We start the intent with a Result that gets returned from the @BarcodeActivity.
     * @param view is the view of the button.
     */
    public void openBarcode(View view) {
        Intent intent = new Intent(StartActivity.this,BarcodeActivity.class);
        intent.putExtra(BarcodeActivity.BARCODE_MODE,BarcodeActivity.BarcodeMode.LIBRARY_BARCODE);
        startActivityForResult(intent,BARCODE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BARCODE_REQUEST) {
            if (resultCode == RESULT_SAVED) {
                boolean isLibrary = data.getBooleanExtra(IS_LIBRARY,false);
                //if we scanned the correct library we continue.
                if (isLibrary){
                    Intent intent = new Intent(StartActivity.this,MainActivity.class);
                    startActivity(intent);
                }
            }
        }
    }
}