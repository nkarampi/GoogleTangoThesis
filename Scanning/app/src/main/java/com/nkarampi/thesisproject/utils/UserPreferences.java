package com.nkarampi.thesisproject.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.nkarampi.thesisproject.model.Book;
import com.nkarampi.thesisproject.model.Library;

/*
    UserPreferences is a class that holds all the User saves.
    For the libraries it stores a @Library object with measurements.
    For books we save a history of scans.
    To save an object in the Preferences we use Gson library.

    Created by: Nikolaos Karampinas
    Date: 9/2018
    Email: nkarampi@csd.auth.gr
 */
public class UserPreferences {
    private static final String PREFS = "prefs"; //Global preferences name
    private static final String PREFS_LIB_SAVED = "prefs_lib_saved";
    private static final String PREF_LIB = "pref_lib";
    private static final String PREF_TOTAL_BOOKS = "pref_books_tot";
    private static final String PREF_REC_BOOK = "pref_books_rec";
    private static final String REC_BOOK_NONE = "-1";

    private SharedPreferences sharedPreferences;

    /**
     * This is the constructor. We get the preferences in the context it gets created.
     * @param context This is the activity/context we need to read/write UserPreferences
     */
    public UserPreferences(Context context){
        sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /**
     *  This method gets a library object and saves it in the preferences to analyze it later.
     *  We use Gson to transform the object to a json string.
     * @param library is the @Library we are saving to the preferences.
     */
    public void setLibrary(Library library){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(library);
        editor.putString(PREF_LIB, json);
        setLibrarySaved();
        editor.apply();
    }

    /**
     * This method returns the saved library. We need Gson to transform the json string to an object.
     * @return a @Library object
     */
    public Library getLibrary(){
        Gson gson = new Gson();
        String json = sharedPreferences.getString(PREF_LIB, "");
        return gson.fromJson(json, Library.class);
    }

    /**
     * This methods sets if we have saved a library.
     */
    private void setLibrarySaved(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREFS_LIB_SAVED,true);
        editor.apply();
    }

    /**
     * This method returns if we have a saved library or not.
     * @return a boolean
     */
    public boolean isLibrarySaved(){
        return sharedPreferences.getBoolean(PREFS_LIB_SAVED,false);
    }

    /**
     * This method sets the number of scans in the preferences.
     * @param totalBooks represents the number of scans
     */
    public void setTotalBooks(int totalBooks){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PREF_TOTAL_BOOKS,totalBooks);
        editor.apply();
    }

    /**
     * This method returns the total scanned books number. Default value is 0.
     * @return an Integer with the number of books.
     */
    public int getTotalBooks(){
        return sharedPreferences.getInt(PREF_TOTAL_BOOKS,0);
    }

    /**
     *  This method gets the scanned book and saves it in the preferences to save history.
     *  We use Gson to transform the object to a json string.
     * @param book the @Book that gets saved
     */
    public void setRecentBook(Book book){
        SharedPreferences.Editor editor= sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(book);
        editor.putString(PREF_REC_BOOK, json);
        editor.apply();
    }

    /**
     * This method returns the latest scanned book. We need Gson to transform the json string to an object.
     * @return the latest scanned @Book
     */
    public Book getRecentBook(){
        Gson gson = new Gson();
        String json = sharedPreferences.getString(PREF_REC_BOOK, REC_BOOK_NONE);
        if (json.equals(REC_BOOK_NONE)){
            return new Book("None");
        }
        return gson.fromJson(json, Book.class);
    }

    /**
     * This methods clears all the preferences
     */
    public void clear(){
        sharedPreferences.edit().clear().apply();
    }

    /**
     * This method clears only the prefrences that holds library's infos.
     * PREF_LIB to remove the @Library
     * PREFS_LIB_SAVED to remove the boolean
     */
    public void clearLibrary(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PREF_LIB);
        editor.remove(PREFS_LIB_SAVED);
        editor.apply();
    }
}
