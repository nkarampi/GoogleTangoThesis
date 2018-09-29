/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified by: Nikolaos Karampinas
 * Date: 9/2018
 * Email: nkarampi@csd.auth.gr
 */
package com.nkarampi.project2.renderer.rajawali;

import android.graphics.Color;
import android.util.Log;

import org.rajawali3d.materials.Material;

import java.nio.FloatBuffer;

import static com.nkarampi.project2.MainActivity.height1;
import static com.nkarampi.project2.MainActivity.height2;
import static com.nkarampi.project2.MainActivity.minDepth;
import static com.nkarampi.project2.MainActivity.width1;
import static com.nkarampi.project2.MainActivity.width2;

/**
 * Renders a point cloud using colors to indicate distance to the depth sensor.
 * Coloring is based on the light spectrum: closest points are in red, farthest in violet.
 */
public class PointCloud extends Points {
    private final float DEPTH_THRESHOLD = 0.2f;
    // Maximum depth range used to calculate coloring (min = 0).
    public static final float CLOUD_MAX_Z = 5;

    private float[] mColorArray;
    private final int[] mPalette;
    public static final int PALETTE_SIZE = 360;
    public static final float HUE_BEGIN = 0;
    public static final float HUE_END = 320;

    public PointCloud(int maxPoints, int floatsPerPoint) {
        //We use this to render without colors
//        super(maxPoints, floatsPerPoint, false);
        super(maxPoints, floatsPerPoint, true);
        mPalette = createPalette();
        mColorArray = new float[maxPoints * 4];
        Material m = new Material();
        m.useVertexColors(true);
        setMaterial(m);
    }

    /**
     * Update the points and colors in the point cloud.
     */
    public void updateCloud(int pointCount, FloatBuffer pointBuffer) {
        calculateColors(pointCount, pointBuffer);
        updatePoints(pointCount, pointBuffer, mColorArray);
        //We use this to render without colors
//        updatePoints(pointCount, pointBuffer);
    }

    /**
     * Pre-calculate a palette to be used to translate between point distance and RGB color.
     */
    private int[] createPalette() {
        int[] palette = new int[PALETTE_SIZE];
        float[] hsv = new float[3];
        hsv[1] = hsv[2] = 1;
        for (int i = 0; i < PALETTE_SIZE; i++) {
            hsv[0] = (HUE_END - HUE_BEGIN) * i / PALETTE_SIZE + HUE_BEGIN;
            palette[i] = Color.HSVToColor(hsv);
        }
        return palette;
    }

    /**
     * Calculate the right color for each point in the point cloud.
     * We only render the points that belong to the library.
     * To achieve this we check if the x,y and z coordinates are within the boundaries of the library.
     */
    private void calculateColors(int pointCount, FloatBuffer pointCloudBuffer) {
        float[] points = new float[pointCount * 4];
        pointCloudBuffer.rewind();
        pointCloudBuffer.get(points);
        pointCloudBuffer.rewind();

        int color;
        int colorIndex;
        float x,y,z;
        for (int i = 0; i < pointCount; i++) {
            x = points[i * mFloatsPerPoint];
            y = points[i * mFloatsPerPoint + 1];
            z = points[i * mFloatsPerPoint + 2];
          //  if (z <= minDepth + DEPTH_THRESHOLD) {
            if ((z <= minDepth + DEPTH_THRESHOLD)
                    && (x >= width1) && (x <= width2)
                    && (y >= height1) && (y <= height2)) {
                colorIndex = (int) Math.min(z / CLOUD_MAX_Z * mPalette.length, mPalette.length - 1);
                colorIndex = Math.max(colorIndex, 0);
                color = mPalette[colorIndex];
                mColorArray[i * 4] = Color.red(color) / 255f;
                mColorArray[i * 4 + 1] = Color.green(color) / 255f;
                mColorArray[i * 4 + 2] = Color.blue(color) / 255f;
                mColorArray[i * 4 + 3] = Color.alpha(color) / 255f;
            }
        }
    }
}
