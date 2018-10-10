// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/*
   Modified by: Nikolaos Karampinas
   Date: 9/2018
   Email: nkarampi@csd.auth.gr
 */
package com.nkarampi.thesisproject.barcode.mlkit.barcodescanning;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.nkarampi.thesisproject.BarcodeActivity;
import com.nkarampi.thesisproject.MainActivity;
import com.nkarampi.thesisproject.StartActivity;
import com.nkarampi.thesisproject.TangoScanActivity;
import com.nkarampi.thesisproject.barcode.mlkit.FrameMetadata;
import com.nkarampi.thesisproject.barcode.mlkit.GraphicOverlay;
import com.nkarampi.thesisproject.barcode.mlkit.VisionProcessorBase;

import java.io.IOException;
import java.util.List;

import static com.nkarampi.thesisproject.BarcodeActivity.BARCODE_VALUE;
import static com.nkarampi.thesisproject.MainActivity.SCAN_MODE;

public class BarcodeScanningProcessor extends VisionProcessorBase<List<FirebaseVisionBarcode>> {
  private static final String TAG = "BarcodeScanProc";
  private Context context;
  private BarcodeActivity.BarcodeMode barcodeMode;

  private final FirebaseVisionBarcodeDetector detector;

  public BarcodeScanningProcessor(Context context, BarcodeActivity.BarcodeMode barcodeMode) {
    this.context = context;
    this.barcodeMode = barcodeMode;

    detector = FirebaseVision.getInstance().getVisionBarcodeDetector();
  }

  @Override
  public void stop() {
    try {
      detector.close();
    } catch (IOException e) {
      Log.e(TAG, "Exception thrown while trying to close Barcode Detector: " + e);
    }
  }

  @Override
  protected Task<List<FirebaseVisionBarcode>> detectInImage(FirebaseVisionImage image) {
    return detector.detectInImage(image);
  }

    /**
     * onSuccess is a callback method that gets called when we have a result.
     * We render the graphics for the barcode with graphicOverlay.
     * If the value is valid we call returnResult().
     * @param barcodes contains the values of the successfully scanned barcodes
     * @param frameMetadata
     * @param graphicOverlay handles the barcode graphics
     */
  @Override
  protected void onSuccess(
      @NonNull List<FirebaseVisionBarcode> barcodes,
      @NonNull FrameMetadata frameMetadata,
      @NonNull GraphicOverlay graphicOverlay) {
    graphicOverlay.clear();
    for (int i = 0; i < barcodes.size(); ++i) {
      FirebaseVisionBarcode barcode = barcodes.get(i);
      if (barcode.getRawValue() != null)
        returnResult((Activity) context, barcode.getRawValue());

      BarcodeGraphic barcodeGraphic = new BarcodeGraphic(graphicOverlay, barcode);
      graphicOverlay.add(barcodeGraphic);
    }

  }

    /**
     * We have two modes where we scan barcodes.
     * The first is when we scan the library (@LIBRARY_BARCODE) and we check if we are in the valid
     * livrary for this app.
     * The second is when we scan a book then we just return the barcode value to the next activity.
     * @param activity points to the Context
     * @param barcode is a String that holds the barcode value
     */
  private void returnResult(Activity activity, String barcode) {
    if (barcodeMode.equals(BarcodeActivity.BarcodeMode.LIBRARY_BARCODE)) {
        if (barcode.equals("ABC-abc-1234")) {
            Intent intent = activity.getIntent();
            activity.setResult(StartActivity.RESULT_SAVED, intent);
            intent.putExtra(StartActivity.IS_LIBRARY, true);
            activity.finish();
        }
    }
    else {
        Intent intent = new Intent(activity, TangoScanActivity.class);
        intent.putExtra(BARCODE_VALUE, barcode);
        intent.putExtra(SCAN_MODE, MainActivity.ScanMode.BOOK_SCAN);
        activity.finish();
        activity.startActivity(intent);
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Barcode detection failed " + e);
  }
}
