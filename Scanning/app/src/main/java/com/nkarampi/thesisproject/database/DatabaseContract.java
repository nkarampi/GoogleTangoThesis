package com.nkarampi.thesisproject.database;

import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*
    This class holds all the static Strings fot the Database.
    We store all the database fields, the database name, the providers authorities etc.
    We also have methods to get from the db.

    Created by: Nikolaos Karampinas
    Date: 9/2018
    Email: nkarampi@csd.auth.gr
 */
public class DatabaseContract {
    public static final String TABLE_NAME = "books";
    //This is the format we use for the Dates in our db. We have Italian locale for Greece
    private static SimpleDateFormat simpleDate = new SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN);

    public static final class BookColumns implements BaseColumns{
        public static final String BOOK_ID = "bookid";
        public static final String NAME = "name";
        public static final String HEIGHT = "height";
        public static final String WIDTH = "width";
        public static final String BARCODE = "barcodeCode";
        public static final String AUTHOR = "author";
        public static final String DATE = "date";
    }

    //We need to give the package to providers Authority.
    public static final String CONTENT_AUTHORITY = "com.nkampi.thesisproject";

    public static final Uri CONTENT_URI = new Uri.Builder().scheme("content")
            .authority(CONTENT_AUTHORITY)
            .appendPath(TABLE_NAME)
            .build();

    /**
     * This is method returns a column with an Integer value.
     * @param cursor a reference to the cursor we get the column from.
     * @param columnName is the name of the column we need the Integer.
     * @return the Integer.
     */
    public static int getColumnInt(Cursor cursor, String columnName){
        return cursor.getInt(cursor.getColumnIndex(columnName));
    }

    /**
     * This is method returns a column with a Double value.
     * @param cursor a reference to the cursor we get the column from.
     * @param columnName is the name of the column we need the Double.
     * @return the Double.
     */
    public static double getColumnDouble(Cursor cursor, String columnName){
        return cursor.getDouble(cursor.getColumnIndex(columnName));
    }

    /**
     * This is method returns a column with a String value.
     * @param cursor a reference to the cursor we get the column from.
     * @param columnName is the name of the column we need the String.
     * @return the String.
     */
    public static String getColumnString(Cursor cursor, String columnName){
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    /**
     * This method converts a String with a specific format(@simpleDate) to a Date for the DB.
     * @param stringDate the date String in a @simpleDate format.
     * @return the Date for the DB.
     */
    public static Date stringToDate(String stringDate){
        try {
            return simpleDate.parse(stringDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}