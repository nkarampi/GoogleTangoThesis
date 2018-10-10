package com.nkarampi.thesisproject;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nkarampi.thesisproject.model.Book;
import com.nkarampi.thesisproject.model.Library;
import com.nkarampi.thesisproject.utils.UserPreferences;

import butterknife.BindView;
import butterknife.ButterKnife;

/*
    This class is the MainActivity. It gets started when we have a successful library barcode scanned.
    This activity is responsible to retrieve the user saved preferences and show the data.
    We also start the Tango activities.

    Created by: Nikolaos Karampinas
    Date: 9/2018
    Email: nkarampi@csd.auth.gr
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int LIBRARY_REQUEST = 1;
    public static final int LIBRARY_SAVED = 2;
    public static final String IS_LIBRARY_SAVED = "isLibrarySaved";
    public static final String SCAN_MODE = "scanMode";
    //This String holds the first(start) point of the library width
    public static final String LIB_W1 = "libW1";
    //This String holds the second(last) point of the library width
    public static final String LIB_W2 = "libW2";
    //This String holds the first(start) point of the library height
    public static final String LIB_H1 = "libH1";
    //This String holds the second(end) point of the library height
    public static final String LIB_H2 = "libH2";

    private UserPreferences userPreferences;

    //We use ButterKnife to bind the UI elements.
    @BindView(R.id.card_view) CardView cardView;
    @BindView(R.id.library_height_value) TextView tvHeightValue;
    @BindView(R.id.library_width_value) TextView tvWidthValue;
    @BindView(R.id.library_empty) RelativeLayout layoutLibraryEmpty;
    @BindView(R.id.card_view_analyze_library) CardView cardViewAnalyze;
    @BindView(R.id.books_title) TextView tvBooksTitle;
    @BindView(R.id.card_view_books) CardView cardViewBooks;
    @BindView(R.id.books_total_value) TextView tvTotalValue;
    @BindView(R.id.books_recent_value) TextView tvRecentValue;
    @BindView(R.id.library_analyze_button) Button buttonAnalyze;
    @BindView(R.id.library_analyze_description) TextView tvAnalyzeDescription;

    //Here we have the two different Tango scan modes.
    public enum ScanMode {
        BOOK_SCAN,
        LIBRARY_SCAN
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        userPreferences = new UserPreferences(this);
        getLibraryState();
    }

    /**
     * This method gets called when we return to this activity. We need this to check if we have saved
     * a library and do some UI changes.
     * @param requestCode the code that we started the request for a result.
     * @param resultCode the code that the result holds.
     * @param data the data that returned from the activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LIBRARY_REQUEST) {
            if (resultCode == LIBRARY_SAVED) {
                boolean isSaved = data.getBooleanExtra(IS_LIBRARY_SAVED,false);
                if (isSaved){
                    //If we scanned a library successfully we change our UI.
                    getLibraryState();
                }
            }
        }
    }

    /**
     * This method checks if we have a saved library and if yes we set the UI.
     * We also set the UI for the books.
     */
    private void getLibraryState() {
        if (userPreferences.isLibrarySaved()){
            setLibraryUI();
        }
        setBooksUI();
    }

    /**
     * This sets the UI for the library values. We only call this if we have a saved library.
     */
    private void setLibraryUI() {
        Library library = userPreferences.getLibrary();
        tvHeightValue.setText(String.valueOf(library.getHeight()));
        tvWidthValue.setText(String.valueOf(library.getWidth()));
        layoutLibraryEmpty.setVisibility(View.INVISIBLE);
        cardView.setVisibility(View.VISIBLE);
        cardViewAnalyze.setVisibility(View.VISIBLE);
        buttonAnalyze.setEnabled(true);
        tvAnalyzeDescription.setText(getResources().getString(R.string.library_saved));
    }

    /**
     * This method set the UI for the books.
     * Set the recently scanned book title and the total number of book scans.
     */
    private void setBooksUI(){
        int totalBooks = userPreferences.getTotalBooks();
        tvTotalValue.setText(String.valueOf(totalBooks));
        Book book = userPreferences.getRecentBook();
        tvRecentValue.setText(String.valueOf(book.getName()));
    }

    /**
     * This is an onClick method from Scan library button. It starts the @TangoScanActivity via intent.
     * It's important to start the activity with the @SCAN_MODE set to @LIBRARY_SCAN.
     * @param view is the Button.
     */
    public void libraryScan(View view) {
        userPreferences.clearLibrary();
        Intent intent = new Intent(MainActivity.this,TangoScanActivity.class);
        intent.putExtra(SCAN_MODE,ScanMode.LIBRARY_SCAN);
        startActivityForResult(intent,LIBRARY_REQUEST);
    }

    /**
     * This is an onClick method from Analyze library button.
     * This method starts an intent for a package. The package represents the second app of this thesis
     * that contains the point clouds analyzer. We need to have the second app installed to start the intent.
     * It's important to start the app with the needed values.
     * @param view is the button.
     */
    public void libraryAnalyze(View view) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.nkarampi.project2");
        //Null pointer check in case package name was not found
        if (launchIntent != null) {
            Library library = userPreferences.getLibrary();
            launchIntent.putExtra(LIB_W1, library.getWidthPoint1());
            launchIntent.putExtra(LIB_W2, library.getWidthPoint2());
            launchIntent.putExtra(LIB_H1, library.getHeightPoint1());
            launchIntent.putExtra(LIB_H2, library.getHeightPoint2());
            startActivity(launchIntent);
        }
        else {
            Toast.makeText(this,getResources().getString(R.string.app_missing),Toast.LENGTH_LONG).show();
        }
    }

    /**
     * This is an onClick method from Scan book button. It starts the @BarcodeActivity via intent.
     * It's important to start the activity with the @SCAN_MODE set to @BOOK_SCAN.
     * @param view is the Button.
     */
    public void bookScan(View view) {
        Intent intent = new Intent(MainActivity.this,BarcodeActivity.class);
        intent.putExtra(BarcodeActivity.BARCODE_MODE,BarcodeActivity.BarcodeMode.BOOK_BARCODE);
        startActivity(intent);
    }

}