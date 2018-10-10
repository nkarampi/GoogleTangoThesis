package com.nkarampi.thesisproject.model;

import android.database.Cursor;

import java.util.Date;

import static com.nkarampi.thesisproject.database.DatabaseContract.*;
import static com.nkarampi.thesisproject.database.DatabaseContract.BookColumns.*;

/*
   This is a class that represents a book.

   Created by: Nikolaos Karampinas
   Date: 9/2018
   Email: nkarampi@csd.auth.gr
 */
public class Book {
    private int bookID;
    private double height;
    private double width;
    private String barcodeCode;
    private String name;
    private String author;
    private Date date;

    public Book(String name){
        this(-1,-1,-1,"-1",name,"-1",stringToDate("23/07/2018"));
    }

    public Book(int bookID,double height, double width, String barcodeCode, String name, String author, Date date) {
        this.bookID = bookID;
        this.height = height;
        this.width = width;
        this.barcodeCode = barcodeCode;
        this.name = name;
        this.author = author;
        this.date = date;
    }

    public Book(Cursor cursor){
        this.bookID = getColumnInt(cursor, BOOK_ID);
        this.height = getColumnDouble(cursor, HEIGHT);
        this.width = getColumnDouble(cursor, WIDTH);
        this.barcodeCode = getColumnString(cursor, BARCODE);
        this.name = getColumnString(cursor, NAME);
        this.author = getColumnString(cursor, AUTHOR);
        this.date = stringToDate(getColumnString(cursor, DATE));
    }

    public int getBookID(){
        return bookID;
    }

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }

    public String getBarcodeCode() {
        return barcodeCode;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public Date getDate() {
        return date;
    }
}