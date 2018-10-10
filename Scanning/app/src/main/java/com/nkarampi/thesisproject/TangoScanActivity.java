package com.nkarampi.thesisproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoException;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.google.atap.tangoservice.experimental.TangoImageBuffer;
import com.google.tango.depthinterpolation.TangoDepthInterpolation;
import com.google.tango.support.TangoPointCloudManager;
import com.google.tango.support.TangoSupport;
import com.google.tango.transformhelpers.TangoTransformHelper;
import com.nkarampi.thesisproject.model.Library;
import com.nkarampi.thesisproject.render.TangoRenderer;
import com.nkarampi.thesisproject.utils.UserPreferences;

import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.scene.ASceneFrameCallback;
import org.rajawali3d.view.SurfaceView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.nkarampi.thesisproject.BarcodeActivity.BARCODE_VALUE;
import static com.nkarampi.thesisproject.MainActivity.IS_LIBRARY_SAVED;
import static com.nkarampi.thesisproject.MainActivity.LIBRARY_SAVED;
import static com.nkarampi.thesisproject.MainActivity.SCAN_MODE;
import static com.nkarampi.thesisproject.MainActivity.ScanMode;
import static com.nkarampi.thesisproject.ResultsActivity.BOOK_HEIGHT;
import static com.nkarampi.thesisproject.ResultsActivity.BOOK_WIDTH;
import static com.nkarampi.thesisproject.utils.State.BookState;
import static com.nkarampi.thesisproject.utils.State.LibraryState;


@SuppressLint("ClickableViewAccessibility")
public class TangoScanActivity extends AppCompatActivity implements View.OnTouchListener {
    private static final String TAG = TangoScanActivity.class.getSimpleName();

    @BindView(R.id.library_surface_view) SurfaceView surfaceView;
    @BindView(R.id.library_scan_x) TextView tvScanX;
    @BindView(R.id.library_scan_y) TextView tvScanY;
    @BindView(R.id.library_scan_x_value) EditText etScanX;
    @BindView(R.id.library_scan_y_value) EditText etScanY;

//    TANGO
//    ------------------------------------------------------------------------------------------------
    private class MeasuredPoint {
        public double mTimestamp;
        public float[] mDepthTPoint;

        public MeasuredPoint(double timestamp, float[] depthTPoint) {
            mTimestamp = timestamp;
            mDepthTPoint = depthTPoint;
        }
    }

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int CAMERA_PERMISSION_CODE = 0;

    private static final int INVALID_TEXTURE_ID = 0;

    private TangoRenderer mTangoRenderer;
    private TangoPointCloudManager mPointCloudManager;
    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsConnected = false;
    private double mCameraPoseTimestamp = 0;

    private volatile TangoImageBuffer mCurrentImageBuffer;

    // Texture rendering related fields.
    // NOTE: Naming indicates which thread is in charge of updating this variable.
    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);
    private double mRgbTimestampGlThread;

    private boolean mPointSwitch = true;

    // Two measured points in Depth Camera space.
    private MeasuredPoint[] mMeasuredPoints = new MeasuredPoint[2];

    // Two measured points in OpenGL space, we used a stack to hold the data is because rajawalli
    // LineRenderer expects a stack of points to be passed in. This is render ready data format from
    // Rajawalli's perspective.
    private Stack<Vector3> mMeasurePoitnsInOpenGLSpace = new Stack<Vector3>();
    private float mMeasuredDistance = 0.0f;

    private int mDisplayRotation = 0;
//    ------------------------------------------------------------------------------------------------

    UserPreferences userPreferences;
    ScanMode scanMode;
    private String barcodeValue;

    //This is the value of the total width
    private float measuredDistanceX = 0.0f;
    //This is the value of X1(width)
    float minX = Float.MAX_VALUE;
    //This is the value of X2(width)
    float maxX = Float.MIN_VALUE;
    //This is the value of the total height
    float measuredDistanceY = 0.0f;
    //This is the value of Y1(height)
    float minY = Float.MAX_VALUE;
    //This is the value of Y2(height)
    float maxY = Float.MIN_VALUE;

    //We count the scans we made
    int counter = 0;
    //We need this boolean to check if we are ready to exit
    boolean exit = true;
    //We need this boolean to check if we set the width
    boolean xSetted = false;
    //We need this boolean to check if we set the height
    boolean ySetted = false;

    //These are the scanning states for the library and the book
    LibraryState libraryState = LibraryState.LIBRARY_SET_X;
    BookState bookState = BookState.BOOK_SET_X;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_library);

        //UI Binding
        ButterKnife.bind(this);

        Intent intent = getIntent();
        if (intent != null) {
            //We need to have a SCAN_MODE which is either LIBRARY or BOOK
            if (intent.hasExtra(SCAN_MODE)) {
                scanMode = (ScanMode) intent.getSerializableExtra(SCAN_MODE);
                //In BOOK_MODE we also have a BARCODE_VALUE
                if (intent.hasExtra(BARCODE_VALUE))
                    barcodeValue = intent.getStringExtra(BARCODE_VALUE);
                userPreferences = new UserPreferences(this);

                mTangoRenderer = new TangoRenderer(this);
                surfaceView.setSurfaceRenderer(mTangoRenderer);
                surfaceView.setOnTouchListener(this);
                mPointCloudManager = new TangoPointCloudManager();
                surfaceView.setOnTouchListener(this);

                //This represents the first(starting) point
                mMeasuredPoints[0] = null;
                //This represents the second(ending) point
                mMeasuredPoints[1] = null;

                DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
                if (displayManager != null) {
                    displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                        @Override
                        public void onDisplayAdded(int displayId) {
                        }

                        @Override
                        public void onDisplayChanged(int displayId) {
                            synchronized (this) {
                                setDisplayRotation();
                            }
                        }

                        @Override
                        public void onDisplayRemoved(int displayId) {
                        }
                    }, null);
                }
            }
            else
                finish();
        }
        else
            finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (checkAndRequestPermissions()) {
            bindTangoService();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        clearLine();
        synchronized (this) {
            try {
                mTangoRenderer.getCurrentScene().clearFrameCallbacks();
                if (mTango != null) {
                    mTango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                    mTango.disconnect();
                }
                mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
                mIsConnected = false;
            } catch (TangoErrorException e) {
                Log.e(TAG, getString(R.string.exception_tango_error), e);
            }
        }
    }


    /**
     * Initialize Tango Service as a normal Android Service.
     */
    private void bindTangoService() {
        // Initialize Tango Service as a normal Android Service. Since we call mTango.disconnect()
        // in onPause, this will unbind Tango Service, so every time onResume gets called we
        // should create a new Tango object.
        mTango = new Tango(TangoScanActivity.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there are no
            // UI thread changes involved.
            @Override
            public void run() {
                synchronized (TangoScanActivity.this) {
                    try {
                        mConfig = setupTangoConfig(mTango);
                        mTango.connect(mConfig);
                        startupTango();
                        TangoSupport.initialize(mTango);
                        connectRenderer();
                        mIsConnected = true;
                        setDisplayRotation();
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, getString(R.string.exception_out_of_date), e);
                        showsToastAndFinishOnUiThread(R.string.exception_out_of_date,true);
                    } catch (TangoErrorException e) {
                        Log.e(TAG, getString(R.string.exception_tango_error), e);
                        showsToastAndFinishOnUiThread(R.string.exception_tango_error,true);
                    } catch (TangoInvalidException e) {
                        Log.e(TAG, getString(R.string.exception_tango_invalid), e);
                        showsToastAndFinishOnUiThread(R.string.exception_tango_invalid,true);
                    }
                }
            }
        });
    }

    /**
     * Sets up the Tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setupTangoConfig(Tango tango) {
        // Use default configuration for Tango Service (motion tracking), plus low latency
        // IMU integration, color camera, depth and drift correction.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        // NOTE: Low latency integration is necessary to achieve a
        // precise alignment of virtual objects with the RBG image and
        // produce a good AR effect.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);
        // Drift correction allows motion tracking to recover after it loses tracking.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true);

        return config;
    }

    /**
     * Set up the callback listeners for the Tango Service and obtain other parameters required
     * after Tango connection.
     * Listen to updates from the RGB camera and point cloud.
     */
    private void startupTango() {
        // No need to add any coordinate frame pairs since we are not
        // using pose data. So just initialize.
        ArrayList<TangoCoordinateFramePair> framePairs =
                new ArrayList<TangoCoordinateFramePair>();
        mTango.connectListener(framePairs, new Tango.OnTangoUpdateListener() {
            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                // We are not using OnPoseAvailable for this app.
            }

            @Override
            public void onFrameAvailable(int cameraId) {
                // Check if the frame available is for the camera we want and update its frame
                // on the view.
                if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                    // Mark a camera frame as available for rendering in the OpenGL thread.
                    mIsFrameAvailableTangoThread.set(true);
                    surfaceView.requestRender();
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData pointCloud) {
                // Save the cloud and point data for later use.
                mPointCloudManager.updatePointCloud(pointCloud);
            }

            @Override
            public void onTangoEvent(TangoEvent event) {
                // We are not using OnPoseAvailable for this app.
            }
        });
        mTango.experimentalConnectOnFrameListener(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                new Tango.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(TangoImageBuffer tangoImageBuffer, int i) {
                        mCurrentImageBuffer = copyImageBuffer(tangoImageBuffer);
                    }

                    TangoImageBuffer copyImageBuffer(TangoImageBuffer imageBuffer) {
                        ByteBuffer clone = ByteBuffer.allocateDirect(imageBuffer.data.capacity());
                        imageBuffer.data.rewind();
                        clone.put(imageBuffer.data);
                        imageBuffer.data.rewind();
                        clone.flip();
                        return new TangoImageBuffer(imageBuffer.width, imageBuffer.height,
                                imageBuffer.stride, imageBuffer.frameNumber,
                                imageBuffer.timestamp, imageBuffer.format, clone,
                                imageBuffer.exposureDurationNs);
                    }
                });
    }

    /**
     * Connects the view and renderer to the color camara and callbacks.
     */
    private void connectRenderer() {
        // Register a Rajawali Scene Frame Callback to update the scene camera pose whenever a new
        // RGB frame is rendered.
        // (@see https://github.com/Rajawali/Rajawali/wiki/Scene-Frame-Callbacks)
        mTangoRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                // NOTE: This is called from the OpenGL render thread, after all the renderer
                // onRender callbacks have a chance to run and before scene objects are rendered
                // into the scene.

                try {
                    // Prevent concurrent access to {@code mIsFrameAvailableTangoThread} from the
                    // Tango callback thread and service disconnection from an onPause event.
                    synchronized (TangoScanActivity.this) {
                        // Don't execute any tango API actions if we're not connected to the
                        // service.
                        if (!mIsConnected) {
                            return;
                        }

                        // Set up scene camera projection to match RGB camera intrinsics.
                        if (!mTangoRenderer.isSceneCameraConfigured()) {
                            TangoCameraIntrinsics intrinsics =
                                    TangoSupport.getCameraIntrinsicsBasedOnDisplayRotation(
                                            TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                            mDisplayRotation);
                            mTangoRenderer.setProjectionMatrix(
                                    projectionMatrixFromCameraIntrinsics(intrinsics));
                        }

                        // Connect the camera texture to the OpenGL Texture if necessary
                        // NOTE: When the OpenGL context is recycled, Rajawali may regenerate the
                        // texture with a different ID.
                        if (mConnectedTextureIdGlThread != mTangoRenderer.getTextureId()) {
                            mTango.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                    mTangoRenderer.getTextureId());
                            mConnectedTextureIdGlThread = mTangoRenderer.getTextureId();
                            Log.d(TAG, "connected to texture id: " + mTangoRenderer.getTextureId());
                        }

                        // If there is a new RGB camera frame available, update the texture with
                        // it.
                        if (mIsFrameAvailableTangoThread.compareAndSet(true, false)) {
                            mRgbTimestampGlThread =
                                    mTango.updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                        }

                        // If a new RGB frame has been rendered, update the camera pose to match.
                        if (mRgbTimestampGlThread > mCameraPoseTimestamp) {
                            // Calculate the camera color pose at the camera frame update time in
                            // OpenGL engine.
                            //
                            // When drift correction mode is enabled in config file, we need
                            // to query the device with respect to Area Description pose in
                            // order to use the drift-corrected pose.
                            //
                            // Note that if you don't want to use the drift corrected pose, the
                            // normal device with respect to start of service pose is still
                            // available.
                            //
                            // Also, we used mColorCameraToDipslayRotation to rotate the
                            // transformation to align with the display frame. The reason we use
                            // color camera instead depth camera frame is because the
                            // getDepthAtPointNearestNeighbor transformed depth point to camera
                            // frame.
                            TangoPoseData lastFramePose = TangoSupport.getPoseAtTime(
                                    mRgbTimestampGlThread,
                                    TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                    TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                                    TangoSupport.ENGINE_OPENGL,
                                    TangoSupport.ENGINE_OPENGL,
                                    mDisplayRotation);
                            if (lastFramePose.statusCode == TangoPoseData.POSE_VALID) {
                                // Update the camera pose from the renderer.
                                mTangoRenderer.updateRenderCameraPose(lastFramePose);
                                mCameraPoseTimestamp = lastFramePose.timestamp;
                            } else {
                                // When the pose status is not valid, it indicates the tracking has
                                // been lost. In this case, we simply stop rendering.
                                //
                                // This is also the place to display UI to suggest the user walk
                                // to recover tracking.
                                Log.w(TAG, "Can't get device pose at time: " +
                                        mRgbTimestampGlThread);
                            }

                            // If both points have been measured, we transform the points to OpenGL
                            // space, and send it to mTangoRenderer to render.
                            if (mMeasuredPoints[0] != null && mMeasuredPoints[1] != null) {
                                // To make sure drift correct pose is also applied to virtual
                                // object (measured points).
                                // We need to re-query the Start of Service to Depth camera
                                // pose every frame. Note that you will need to use the timestamp
                                // at the time when the points were measured to query the pose.
                                TangoSupport.MatrixTransformData openglTDepthArr0 =
                                        TangoSupport.getMatrixTransformAtTime(
                                                mMeasuredPoints[0].mTimestamp,
                                                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                                TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                                                TangoSupport.ENGINE_OPENGL,
                                                TangoSupport.ENGINE_TANGO,
                                                TangoSupport.ROTATION_IGNORED);

                                TangoSupport.MatrixTransformData openglTDepthArr1 =
                                        TangoSupport.getMatrixTransformAtTime(
                                                mMeasuredPoints[1].mTimestamp,
                                                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                                TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                                                TangoSupport.ENGINE_OPENGL,
                                                TangoSupport.ENGINE_TANGO,
                                                TangoSupport.ROTATION_IGNORED);

                                if (openglTDepthArr0.statusCode == TangoPoseData.POSE_VALID &&
                                        openglTDepthArr1.statusCode == TangoPoseData.POSE_VALID) {
                                    mMeasurePoitnsInOpenGLSpace.clear();
                                    float[] p0 = TangoTransformHelper.transformPoint(
                                            openglTDepthArr0.matrix,
                                            mMeasuredPoints[0].mDepthTPoint);
                                    float[] p1 = TangoTransformHelper.transformPoint(
                                            openglTDepthArr1.matrix,
                                            mMeasuredPoints[1].mDepthTPoint);

                                    mMeasurePoitnsInOpenGLSpace.push(
                                            new Vector3(p0[0], p0[1], p0[2]));
                                    mMeasurePoitnsInOpenGLSpace.push(
                                            new Vector3(p1[0], p1[1], p1[2]));

                                    //We measure the distance
                                    mMeasuredDistance = (float) Math.sqrt(
                                            Math.pow(p0[0] - p1[0], 2) +
                                                    Math.pow(p0[1] - p1[1], 2) +
                                                    Math.pow(p0[2] - p1[2], 2));

                                    //We use this to set the distance values
                                    setValues(mMeasuredPoints,mMeasuredDistance);
                                }
                            }

                            mTangoRenderer.setLine(mMeasurePoitnsInOpenGLSpace);
                        }
                    }
                    // Avoid crashing the application due to unhandled exceptions.
                } catch (TangoErrorException e) {
                    Log.e(TAG, "Tango API call error within the OpenGL render thread", e);
                } catch (Throwable t) {
                    Log.e(TAG, "Exception on the OpenGL thread", t);
                }
            }

            @Override
            public void onPreDraw(long sceneTime, double deltaTime) {

            }

            @Override
            public void onPostFrame(long sceneTime, double deltaTime) {

            }

            @Override
            public boolean callPreFrame() {
                return true;
            }
        });
    }

    /**
     * This method get the points and the distances and store them.
     * @param mMeasuredPoints the width points
     * @param mMeasuredDistance the total width
     * @param setWidth this boolean checks if we need to set Width or Height
     */
    private void setMeasurements(MeasuredPoint[] mMeasuredPoints, float mMeasuredDistance, boolean setWidth){
        if (setWidth){
            if (mMeasuredPoints[0].mDepthTPoint[0] <= mMeasuredPoints[1].mDepthTPoint[0]) {
                minX = mMeasuredPoints[0].mDepthTPoint[0];
                maxX = mMeasuredPoints[1].mDepthTPoint[0];
            } else {
                maxX = mMeasuredPoints[0].mDepthTPoint[0];
                minX = mMeasuredPoints[1].mDepthTPoint[0];
            }
            measuredDistanceX = mMeasuredDistance;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setUIValues(tvScanX, etScanX, String.valueOf(measuredDistanceX));
                }
            });
            xSetted = true;
            mMeasuredPoints[0] = null;
            mMeasuredPoints[1] = null;
        }
        else {
            if (mMeasuredPoints[0].mDepthTPoint[1] <= mMeasuredPoints[1].mDepthTPoint[1]) {
                minY = mMeasuredPoints[0].mDepthTPoint[1];
                maxY = mMeasuredPoints[1].mDepthTPoint[1];
            } else {
                maxY = mMeasuredPoints[0].mDepthTPoint[1];
                minY = mMeasuredPoints[1].mDepthTPoint[1];
            }
            measuredDistanceY = mMeasuredDistance;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setUIValues(tvScanY, etScanY, String.valueOf(measuredDistanceY));
                }
            });
            ySetted = true;
        }
    }


    /**
     * After we measure the distance of the two points the user chose we need to store them.
     * The runtime depends on the SCAN_MODE we have so we store the correct values each time.
     * @param mMeasuredPoints are the set of Points
     * @param mMeasuredDistance is the distance of these two Points
     */
    //TODO: Change to only one state
    private void setValues(MeasuredPoint[] mMeasuredPoints, float mMeasuredDistance) {
        if (scanMode.equals(ScanMode.LIBRARY_SCAN)){
            switch (libraryState){
                case LIBRARY_SET_X:{
                    //If we haven't set Width we set it here
                    if (!xSetted) {
                    setMeasurements(mMeasuredPoints,mMeasuredDistance,true);
                    }
                    break;
                }
                case LIBRARY_SET_Y:{
                    //If we haven't set Height we set it here
                    if (!ySetted) {
                      setMeasurements(mMeasuredPoints,mMeasuredDistance,false);
                    }
                    break;
                }
                case DONE:{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            saveAndQuit();
                        }
                    });
                    break;
                }
                default:{
                    throw new UnsupportedOperationException("Unknown LibraryState");
                }
            }
        }
        else if (scanMode.equals(ScanMode.BOOK_SCAN)){
            switch (bookState){
                case BOOK_SET_X:{
                    if (!xSetted) {
                        setMeasurements(mMeasuredPoints,mMeasuredDistance,true);
                    }
                    break;
                }
                case BOOK_SET_Y:{
                    if (!ySetted) {
                        setMeasurements(mMeasuredPoints,mMeasuredDistance,false);
                    }
                    break;
                }
                case DONE:{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            saveAndQuit();
                        }
                    });
                    break;
                }
                default:{
                    throw new UnsupportedOperationException("Unknown LibraryState");
                }
            }
        }
        else
            throw new UnsupportedOperationException("Unknown ScanMode");
    }

    /**
     * Use Tango camera intrinsics to calculate the projection Matrix for the Rajawali scene.
     */
    private static float[] projectionMatrixFromCameraIntrinsics(TangoCameraIntrinsics intrinsics) {
        // Uses frustumM to create a projection matrix taking into account calibrated camera
        // intrinsic parameter.
        // Reference: http://ksimek.github.io/2013/06/03/calibrated_cameras_in_opengl/
        float near = 0.1f;
        float far = 100;

        double cx = intrinsics.cx;
        double cy = intrinsics.cy;
        double width = intrinsics.width;
        double height = intrinsics.height;
        double fx = intrinsics.fx;
        double fy = intrinsics.fy;

        double xscale = near / fx;
        double yscale = near / fy;

        double xoffset = (cx - (width / 2.0)) * xscale;
        // Color camera's coordinates has y pointing downwards so we negate this term.
        double yoffset = -(cy - (height / 2.0)) * yscale;

        float m[] = new float[16];
        Matrix.frustumM(m, 0,
                (float) (xscale * -width / 2.0 - xoffset),
                (float) (xscale * width / 2.0 - xoffset),
                (float) (yscale * -height / 2.0 - yoffset),
                (float) (yscale * height / 2.0 - yoffset), near, far);
        return m;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

            float u = motionEvent.getX() / view.getWidth();
            float v = motionEvent.getY() / view.getHeight();

            try {
                // Place point near the clicked point using the latest point cloud data.
                // Synchronize against concurrent access to the RGB timestamp in the OpenGL thread
                // and a possible service disconnection due to an onPause event.
                MeasuredPoint newMeasuredPoint;
                synchronized (this) {
                    newMeasuredPoint = getDepthAtTouchPosition(u, v);
                }
                if (newMeasuredPoint != null) {
                    //We increase the counter only if we got the Point
                    counter++;
                    // Update a line endpoint to the touch location.
                    // This update is made thread-safe by the renderer.
                    updateLine(newMeasuredPoint);
                } else {
                    Log.w(TAG, "Point was null.");
                }

            } catch (TangoException t) {
                Toast.makeText(getApplicationContext(),
                        R.string.failed_measurement,
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, getString(R.string.failed_measurement), t);
            } catch (SecurityException t) {
                Toast.makeText(getApplicationContext(),
                        R.string.failed_permissions,
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, getString(R.string.failed_permissions), t);
            }
        }
        return true;
    }

    /**
     * Use the Tango Support Library with point cloud data to calculate the depth
     * of the point closest to where the user touches the screen. It returns a
     * Vector3 in OpenGL world space.
     *
     * Also, after we have the point we need to know the Scanning mode and set the states.
     */
    private MeasuredPoint getDepthAtTouchPosition(float u, float v) {
        TangoPointCloudData pointCloud = mPointCloudManager.getLatestPointCloud();
        if (pointCloud == null) {
            return null;
        }

        double rgbTimestamp;
        TangoImageBuffer imageBuffer = mCurrentImageBuffer;
//        if (mBilateralBox.isChecked()) {
//            rgbTimestamp = imageBuffer.timestamp; // CPU.
//        } else {
            rgbTimestamp = mRgbTimestampGlThread; // GPU.
//        }

        TangoPoseData depthlTcolorPose = TangoSupport.getPoseAtTime(
                rgbTimestamp,
                TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                TangoSupport.ENGINE_TANGO,
                TangoSupport.ENGINE_TANGO,
                TangoSupport.ROTATION_IGNORED);
        if (depthlTcolorPose.statusCode != TangoPoseData.POSE_VALID) {
            Log.w(TAG, "Could not get color camera transform at time "
                    + rgbTimestamp);
            return null;
        }

        float[] depthPoint;
        //CPU
        /*if (mBilateralBox.isChecked()) {
            depthPoint = TangoDepthInterpolation.getDepthAtPointBilateral(
                    pointCloud,
                    new double[] {0.0, 0.0, 0.0},
                    new double[] {0.0, 0.0, 0.0, 1.0},
                    imageBuffer,
                    u, v,
                    mDisplayRotation,
                    depthlTcolorPose.translation,
                    depthlTcolorPose.rotation);
        } else {*/
        depthPoint = TangoDepthInterpolation.getDepthAtPointNearestNeighbor(
                pointCloud,
                new double[] {0.0, 0.0, 0.0},
                new double[] {0.0, 0.0, 0.0, 1.0},
                u, v,
                mDisplayRotation,
                depthlTcolorPose.translation,
                depthlTcolorPose.rotation);
//        }
        if (depthPoint == null) {
            return null;
        }
        /*
            Firstly the state is set to SET_X.
            After we got two points and the counter is 2, we change the state to SET_Y.
            When the counter is 4 we need to set the state to DONE to finish.
         */
        switch (scanMode){
            case LIBRARY_SCAN:{
                if (counter == 2){
                    if (libraryState.equals(LibraryState.LIBRARY_SET_X)) {
                        libraryState = LibraryState.LIBRARY_SET_Y;
                    }
                    clearLine();
                }
                else if (counter == 4){
                    if (libraryState.equals(LibraryState.LIBRARY_SET_Y)) {
                        libraryState = LibraryState.DONE;
                    }
                }
                break;
            }
            case BOOK_SCAN:{
                if (counter == 2){
                    if (bookState.equals(BookState.BOOK_SET_X)) {
                        bookState = BookState.BOOK_SET_Y;
                    }
                    clearLine();
                }
                else if (counter == 4){
                    if (bookState.equals(BookState.BOOK_SET_Y)) {
                        bookState = BookState.DONE;
                    }
                }
                break;
            }
            default:{
                throw new UnsupportedOperationException("Unknown ScanMode");
            }
        }
        return new MeasuredPoint(rgbTimestamp, depthPoint);
    }

    /**
     * Update the oldest line endpoint to the value passed into this function.
     * This will also flag the line for update on the next render pass.
     */
    private synchronized void updateLine(MeasuredPoint newPoint) {
        if (mPointSwitch) {
            mPointSwitch = !mPointSwitch;
            mMeasuredPoints[0] = newPoint;
            return;
        }
        mPointSwitch = !mPointSwitch;
        mMeasuredPoints[1] = newPoint;
    }

    /*
     * Remove all the points from the Scene.
     */
    private synchronized void clearLine() {
        mMeasuredPoints[0] = null;
        mMeasuredPoints[1] = null;
        mPointSwitch = true;
        mTangoRenderer.setLine(null);
    }

    /**
     * Set the color camera background texture rotation and save the display rotation.
     */
    private void setDisplayRotation() {
        Display display = getWindowManager().getDefaultDisplay();
        mDisplayRotation = display.getRotation();

        // We also need to update the camera texture UV coordinates. This must be run in the OpenGL
        // thread.
        surfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mIsConnected) {
                    mTangoRenderer.updateColorCameraTextureUvGlThread(mDisplayRotation);
                }
            }
        });
    }

    /**
     * Check to see if we have the necessary permissions for this app; ask for them if we don't.
     *
     * @return True if we have the necessary permissions, false if we don't.
     */
    private boolean checkAndRequestPermissions() {
        if (!hasCameraPermission()) {
            requestCameraPermission();
            return false;
        }
        return true;
    }

    /**
     * Check to see if we have the necessary permissions for this app.
     */
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request the necessary permissions for this app.
     */
    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION)) {
            showRequestPermissionRationale();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION},
                    CAMERA_PERMISSION_CODE);
        }
    }

    /**
     * If the user has declined the permission before, we have to explain that the app needs this
     * permission.
     */
    private void showRequestPermissionRationale() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(getResources().getString(R.string.alert_title))
                .setPositiveButton(getResources().getString(R.string.alert_pos), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(TangoScanActivity.this,
                                new String[]{CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
                    }
                })
                .create();
        dialog.show();
    }

    /**
     * Display toast on UI thread.
     *
     * @param resId The resource id of the string resource to use. Can be formatted text.
     * @param finish holds a boolean if we want to finish the activity after the Toast
     */
    private void showsToastAndFinishOnUiThread(final int resId, final boolean finish) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TangoScanActivity.this,
                        getString(resId), Toast.LENGTH_SHORT).show();
                if (finish)
                    finish();
            }
        });
    }

    /**
     * Result for requesting camera permission.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (hasCameraPermission()) {
            bindTangoService();
        } else {
            Toast.makeText(this, "Java Point to point Example requires camera permission",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * This method needs to check the Scanning mode to store the values and contimue.
     * We only call this method when we measured the width and the height of a library/book.
     * In case of LIBRARY_SCAN we store the values in the User Preferences and we finish the Activity.
     * In case of BOOK_SCAN we finish the Activity and we go to the ResultsActivity with the measured
     * values and the barcode of the book.
     */
    private void saveAndQuit() {
        switch (scanMode){
            case LIBRARY_SCAN:{
                userPreferences.setLibrary(new Library(1,measuredDistanceX,minX,maxX,measuredDistanceY,minY,maxY,true));
                Intent intent = getIntent();
                setResult(LIBRARY_SAVED, intent);
                intent.putExtra(IS_LIBRARY_SAVED, true);
                finish();
                break;
            }
            case BOOK_SCAN:{
                //Here we checked if we made a fifth touch on the screen and if we are ready to finish.
                if (counter == 5 && exit) {
                    exit = false;
                    Intent intent = new Intent(TangoScanActivity.this, ResultsActivity.class);
                    intent.putExtra(BARCODE_VALUE, barcodeValue);
                    intent.putExtra(BOOK_HEIGHT, measuredDistanceY);
                    intent.putExtra(BOOK_WIDTH, measuredDistanceY);
                    finish();
                    startActivity(intent);
                }
                break;
            }
            default:{
                throw new UnsupportedOperationException("Unknown ScanMode");
            }
        }
    }

    /**
     * This method sets the UI values. In more depth, after we make a measurement the values are
     * shown in the UI.
     * @param tv the TextView (Width/Height labels)
     * @param et the Edit text field
     * @param distanceString the distance (Width/Height) that we set in et
     */
    private void setUIValues(TextView tv, EditText et,String distanceString) {
        tv.setVisibility(View.VISIBLE);
        et.setText(distanceString);
        et.setVisibility(View.VISIBLE);
    }
}