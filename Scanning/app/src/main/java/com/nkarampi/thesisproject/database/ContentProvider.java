package com.nkarampi.thesisproject.database;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static com.nkarampi.thesisproject.database.DatabaseContract.CONTENT_URI;
import static com.nkarampi.thesisproject.database.DatabaseContract.TABLE_NAME;

/*
    This class holds the provider for the connection between the database and the app.
    Content providers are one of the best practises for Android Development with SQLite DBs.

    Created by: Nikolaos Karampinas
    Date: 9/2018
    Email: nkarampi@csd.auth.gr
 */
public class ContentProvider extends android.content.ContentProvider{
    //We have this Integer to identify if we have a query with all books.
    private static final int BOOKS = 100;
    //We have this Integer to identify if we have a query for a specific book.
    private static final int BOOKS_WITH_ID = 101;

    private DbHelper mDbHelper;

    //We need this matcher to match the providers queries
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(DatabaseContract.CONTENT_AUTHORITY, TABLE_NAME, BOOKS);

        uriMatcher.addURI(DatabaseContract.CONTENT_AUTHORITY, TABLE_NAME + "/#", BOOKS_WITH_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new DbHelper(getContext());
        return true;
    }

    /**
     * This method is SELECT Query. We open the db with Read privileges.
     * We create a Cursor and we need to match the query for our implementations.
     * We have different cases if we SELECT * or if we SELECT a specific book (w/ ID).
     * @param uri the CONTENT_URI
     * @param projection the Columns to return
     * @param selection the WHERE clause
     * @param selectionArgs WHERE clause values
     * @param sortOrder ORDER_BY clause
     * @return a Cursor with the results
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder)  {
        Cursor cursor;
        switch (uriMatcher.match(uri)){
            case BOOKS:{
                cursor = mDbHelper.getReadableDatabase().query(
                        TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case BOOKS_WITH_ID:{
                String id = uri.getLastPathSegment();
                String[] selectionArguments = new String[]{id};
                cursor = mDbHelper.getReadableDatabase().query(
                        TABLE_NAME,
                        projection,
                        "_id=?",
                        selectionArguments,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (getContext() != null)
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /**
     * This method is the INSERT Query. We open the db with Write privileges.
     * We have only a case for * the books and we insert it with Content Values.
     * @param uri the CONTENT_URI
     * @param contentValues are the values of the Query.
     * @return a Uri that holds the number of row of the new insertion
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        Uri returnUri;
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        switch (uriMatcher.match(uri)){
            case BOOKS:{
                long id = db.insert(
                        TABLE_NAME,
                        null,
                        contentValues
                );
                if (id>0)
                    returnUri = ContentUris.withAppendedId(CONTENT_URI,id);
                else
                    throw new SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        //Always close the Cursor.
        db.close();
        //If the insertion is successful we notify the database
        if (getContext() != null)
            getContext().getContentResolver().notifyChange(uri,null);
        return returnUri;
    }

    /**
     * This method is the DELETE Query. We open the db with Write privileges.
     * We have different cases for * the books and for a book with an ID.
     * @param uri the CONTENT_URI
     * @param selection  the WHERE clause
     * @param selectionArgs WHERE clause values
     * @return an Integer that holds the count of deletions
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        switch (uriMatcher.match(uri)){
            case BOOKS:{
                if (selection == null)
                    selection = "1";
                break;
            }
            case BOOKS_WITH_ID:{
                long id = ContentUris.parseId(uri);
                selection = String.format("%s = ?", DatabaseContract.BookColumns._ID);
                selectionArgs = new String[]{String.valueOf(id)};
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        int count = db.delete(TABLE_NAME,selection,selectionArgs);
        //If we have a deletion we notify the database about the change.
        if (count > 0 && getContext() != null)
            getContext().getContentResolver().notifyChange(uri,null);
        //Always close the Cursor.
        db.close();
        return count;
    }

    /**
     * We don't use this method in this project.
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        throw new RuntimeException("We are not implementing getType in this Project.");
    }

    /**
     * We don't use this method in this project.
     */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        throw new RuntimeException("We are not implementing getType in this Project.");
    }
}