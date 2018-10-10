package com.nkarampi.thesisproject.model;

/*
    This class represent a library.

    Created by: Nikolaos Karampinas
    Date: 9/2018
    Email: nkarampi@csd.auth.gr
 */
public class Library {
    private int id;
    //This is the total width of the library
    private double width;
    //This is the first(start) width point of the library
    private float widthPoint1;
    //This is the second(end) width point of the library
    private float widthPoint2;
    //This is the total height of the library
    private double height;
    //This is the first(start) height point of the library
    private float heightPoint1;
    //This is the seconde(end) height point of the library
    private float heightPoint2;
    private boolean isSet;

    public Library(int id, double width, float widthPoint1, float widthPoint2, double height, float heightPoint1, float heightPoint2, boolean isSet) {
        this.id = id;
        this.width = width;
        this.widthPoint1 = widthPoint1;
        this.widthPoint2 = widthPoint2;
        this.height = height;
        this.heightPoint1 = heightPoint1;
        this.heightPoint2 = heightPoint2;
        this.isSet = isSet;
    }


    public int getId(){ return id; }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public boolean isSet() {
        return isSet;
    }

    public float getWidthPoint1() {
        return widthPoint1;
    }

    public float getWidthPoint2() {
        return widthPoint2;
    }

    public float getHeightPoint1() {
        return heightPoint1;
    }

    public float getHeightPoint2() {
        return heightPoint2;
    }
}
