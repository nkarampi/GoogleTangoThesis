package com.nkarampi.thesisproject;

import android.content.Intent;
import android.database.Cursor;
import android.hardware.display.DisplayManager;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.tango.support.TangoPointCloudManager;
import com.nkarampi.thesisproject.database.DatabaseContract;
import com.nkarampi.thesisproject.model.Book;
import com.nkarampi.thesisproject.render.TangoRenderer;
import com.nkarampi.thesisproject.utils.UserPreferences;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.nkarampi.thesisproject.BarcodeActivity.BARCODE_VALUE;
import static com.nkarampi.thesisproject.MainActivity.SCAN_MODE;

/*
    The ResultsActivity is the final activity of the Scan book feature.
    We get results depending on the measurements and the barcode scanning.
    For this activity we need the @BARCODE_VALUE, @BOOK_HEIGHT and @BOOK_WIDTH.
    We use this values to query our DB and get the results.

    Created by: Nikolaos Karampinas
    Date: 9/2018
    Email: nkarampi@csd.auth.gr
 */
public class ResultsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String BOOK_HEIGHT = "bookHeight";
    public static final String BOOK_WIDTH = "bookWidth";

    private static final int LOADER_ID = 146;
    private static final int LOADER2_ID = 936;
    private final double THRESHOLD = 0.21;

    @BindView(R.id.results_no) TextView tvNoResults;
    @BindView(R.id.results_book) TextView tvBook;
    @BindView(R.id.results_list) ListView listView;
    @BindView(R.id.card_view_results) CardView cardView;

    private String barcodeValue;
    private double height;
    private double width;
    private UserPreferences userPreferences;
    private ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(BARCODE_VALUE) && intent.hasExtra(BOOK_HEIGHT) && intent.hasExtra(BOOK_WIDTH)) {
                ButterKnife.bind(this);

                arrayAdapter = new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_list_item_1);

                //Here we retrieve the values.
                barcodeValue = intent.getStringExtra(BARCODE_VALUE);
                height = intent.getFloatExtra(BOOK_HEIGHT,-1);
                width = intent.getFloatExtra(BOOK_WIDTH,-1);

                //Here we create a UserPreferences reference to incriminate the total book count.
                userPreferences = new UserPreferences(this);
                int totalBooks = userPreferences.getTotalBooks() + 1;
                userPreferences.setTotalBooks(totalBooks);

                //Here we call checkDb() to check the db with the retrieved values.
                checkDB();
            }
            else
                finish();
        }
        else
            finish();
    }

    /**
     * The checkDB method starts an AsyncLoader to query our DB in another thread.
     * We do this only if we have valide values for our height/width values.
     */
    private void checkDB() {
        if (height != -1 && width != -1)
            getSupportLoaderManager().initLoader(LOADER_ID,null,ResultsActivity.this);
    }

    /**
     * This method creates the Async Loader that holds a Cursor.
     * We have to Loader Ids because we have different queries.
     * The @LOADER_ID gets called always and checks if the barcode value is in a record in our db.
     * If it's not then we restart our loader with the @LOADER2_ID and we check the book height
     * and width between some limits that are defined by the @THRESHOLD.
     * @param id the LOADER_ID
     * @param args the Bundle arguments
     * @return a Cursor tha holds the results
     */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID: {
                return new CursorLoader(
                        this,
                        DatabaseContract.CONTENT_URI,
                        null,
                        DatabaseContract.BookColumns.BARCODE + "=?",
                        new String[]{ barcodeValue },
                        null);
            }
            case LOADER2_ID: {
                String select = DatabaseContract.BookColumns.HEIGHT + " BETWEEN ? AND ? AND " + DatabaseContract.BookColumns.WIDTH + " BETWEEN ? AND ?";
                String[] selectArgs = new String[]{
                        String.valueOf(height - THRESHOLD),
                        String.valueOf(height + THRESHOLD),
                        String.valueOf(width - THRESHOLD),
                        String.valueOf(width + THRESHOLD)
                        };
                return new CursorLoader(
                        this,
                        DatabaseContract.CONTENT_URI,
                        null,
                        select,
                        selectArgs,
                        null);
            }
            default:
                throw new RuntimeException("Loader Not Implemented: " + id);
        }
    }

    /**
     * This method gets called when the async task is finished.
     * If we have data we use cases switching our @LOADER_ID.
     * If we have no results from @LOADER_ID
     * which checks the barcode value then we restart the loader with @LOADER2_ID to check the book
     * measurements. Else we take the measurements and the stored measurements of the book we scanned
     * and calculate the accuracy.
     * If we have no results from @LOADER2_ID
     * which checks the book's width and height between limits, we show that we have no results. Else
     * we show books with similar measurements to compare the tango's accuracy.
     * @param loader holds the AsyncLoader.
     * @param data is the the cursor's results.
     */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data != null) {
            switch (loader.getId()) {
                case LOADER_ID: {
                    if (data.getCount() == 0){
                        getSupportLoaderManager().initLoader(LOADER2_ID,null,ResultsActivity.this);
                    }
                    else {
                        if (data.moveToFirst()) {
                            Book book = new Book(data);
                            double accuracyHeight = (book.getHeight() - height) / book.getHeight() * 100;
                            double accuracyWidth = (book.getWidth() - width) / book.getWidth() * 100;

                            arrayAdapter.addAll(book.getName(),"Height Accuracy: " + accuracyHeight,"Width Accuracy: " + accuracyWidth);
                            setBooksUI();
                            userPreferences.setRecentBook(book);
                        }
                    }
                    break;
                }
                case LOADER2_ID: {
                    if (data.getCount() == 0){
                        Log.v(TAG,"No results"); //TODO
                        tvNoResults.setVisibility(View.VISIBLE);
                    }
                    else {
                        if (data.moveToFirst()){
                            ArrayList<String> books = new ArrayList<>();
                            while (!data.isAfterLast()) {
                                Book book = new Book(data);
                                books.add(book.getName());
                                data.moveToNext();
                            }
                            arrayAdapter.addAll(books);
                            tvBook.setText("Can't find a match for the book.\nPossible matches");
                            setBooksUI();
                        }
                    }
                    break;
                }
                default:
                    throw new RuntimeException("Loader Not Implemented: " + loader.getId());
            }
            data.close();
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {}

    /**
     * This method is only making UI changed depending on the results.
     */
    private void setBooksUI(){
        tvBook.setVisibility(View.VISIBLE);
        cardView.setVisibility(View.VISIBLE);
        listView.setAdapter(arrayAdapter);
    }
}
