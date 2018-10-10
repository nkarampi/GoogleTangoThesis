package com.nkarampi.thesisproject.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import static android.provider.BaseColumns._ID;
import static com.nkarampi.thesisproject.database.DatabaseContract.BookColumns.AUTHOR;
import static com.nkarampi.thesisproject.database.DatabaseContract.BookColumns.BARCODE;
import static com.nkarampi.thesisproject.database.DatabaseContract.BookColumns.BOOK_ID;
import static com.nkarampi.thesisproject.database.DatabaseContract.BookColumns.DATE;
import static com.nkarampi.thesisproject.database.DatabaseContract.BookColumns.HEIGHT;
import static com.nkarampi.thesisproject.database.DatabaseContract.BookColumns.NAME;
import static com.nkarampi.thesisproject.database.DatabaseContract.BookColumns.WIDTH;
import static com.nkarampi.thesisproject.database.DatabaseContract.TABLE_NAME;

/*
    This is the SQLiteHelper. We have this class to create,initialize,update the db.

    Created by: Nikolaos Karampinas
    Date: 9/2018
    Email: nkarampi@csd.auth.gr
 */
public class DbHelper extends SQLiteOpenHelper{
    public static final String DATABASE_NAME = "books.db";

    //If we make a change to db structure we need to change the version.
    public static final int DATABASE_VERSION = 1;

    private Context context;

    //This is the CREATE Query with all the databases fields.
    private static final String SQL_CREATE_TABLE = String.format(
            "CREATE TABLE %s" +
                    "(%s INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "%s INTEGER NOT NULL," +
                    "%s TEXT NOT NULL," +
                    "%s REAL NOT NULL," +
                    "%s REAL NOT NULL," +
                    "%s TEXT NOT NULL," +
                    "%s TEXT NOT NULL," +
                    "%s TEXT NOT NULL)" ,
            TABLE_NAME,
            _ID,
            BOOK_ID,
            NAME,
            HEIGHT,
            WIDTH,
            BARCODE,
            AUTHOR,
            DATE
    );

    public DbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    /**
     * onCreate method creates the database and calls the loadDemoTask to fill it for the demo
     * @param sqLiteDatabase is a reference to the database we create
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_TABLE);
        //We fill the database for the demo's purposes
        loadDemoTask(sqLiteDatabase);
    }

    /**
     * onUpgrade is called when we make a change to database version.
     * @param sqLiteDatabase points to the database we Update
     * @param i The old database version.
     * @param i1 The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    /**
     * This is a testing method to fill the database with some test books.
     * @param db a reference to the database where we create the demo values.
     */
    private void loadDemoTask(SQLiteDatabase db) {
        for (int i = 0; i <= 20; i++) {
            ContentValues values = new ContentValues();
            values.put(BOOK_ID, 100 + i);
            values.put(NAME, "test_" + i);
            values.put(HEIGHT, 1.0 + i*(0.2));
            values.put(WIDTH, 0.89 + i*(0.1));
            values.put(BARCODE, "AAA" + i);
            values.put(AUTHOR, "author_" + i);
            values.put(DATE, "21/05/1995");
            db.insertOrThrow(TABLE_NAME, null, values);
        }
    }
}
