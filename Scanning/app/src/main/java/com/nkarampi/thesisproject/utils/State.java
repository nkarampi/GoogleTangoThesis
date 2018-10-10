package com.nkarampi.thesisproject.utils;

/*
    This is class that holds the enumerators.
    We have enums for the library and the book state.

    Created by: Nikolaos Karampinas
    Date: 9/2018
    Email: nkarampi@csd.auth.gr
 */
public class State {
    public enum LibraryState {
        //This State means that we set the library's width
        LIBRARY_SET_X,
        //This State means that we set the library's height
        LIBRARY_SET_Y,
        //This State means that we done scanning the library
        DONE
    }

    public enum BookState {
        //This State means that we set the books's width
        BOOK_SET_X,
        //This State means that we set the books's height
        BOOK_SET_Y,
        //This State means that we done scanning the book
        DONE
    }
}