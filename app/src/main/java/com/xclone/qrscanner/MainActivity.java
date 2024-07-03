package com.xclone.qrscanner;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.size.SizeSelectors;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private CameraView cameraView;
    private ImageView imageView;
    private List<Bitmap> bitmapList = new ArrayList<>();
    private FrameAdapter frameAdapter;
    private boolean isRecording = false;
    private TextView recordingIndicator;
    private long lastFrameTime = 0;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Handle edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        imageView = findViewById(R.id.imageView);

        setupRecyclerView();
        setupCameraView();
        setupRecordingControls();
    }

    //Setup RecyclerView
    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        frameAdapter = new FrameAdapter(bitmapList);
        recyclerView.setAdapter(frameAdapter);
    }
    //Setup Camera View
    private void setupCameraView() {
        cameraView = findViewById(R.id.camera);
        cameraView.setLifecycleOwner(this);
        cameraView.setPreviewStreamSize(SizeSelectors.and(SizeSelectors.maxHeight(1920), SizeSelectors.maxWidth(1080)));
    }

    //Setup Recording Controls
    private void setupRecordingControls() {
        recordingIndicator = findViewById(R.id.recordingIndicator);
        findViewById(R.id.startButton).setOnClickListener(view -> startRecording());
        findViewById(R.id.stopButton).setOnClickListener(view -> stopRecording());
    }

    //Start Recording
    private void startRecording() {
        isRecording = true;
        recordingIndicator.setVisibility(View.VISIBLE);
        lastFrameTime = 0;

//        cameraView.setFilter(Filters.BLACK_AND_WHITE.newInstance());
        cameraView.addFrameProcessor(frameProcessor);
        Log.d(TAG, "Recording started");
    }

    //Stop Recording
    private void stopRecording() {
        isRecording = false;
        cameraView.clearFrameProcessors();
        recordingIndicator.setVisibility(View.GONE);
        Log.d(TAG, "Recording stopped");
    }

    //Process Frame
    private final FrameProcessor frameProcessor = new FrameProcessor() {
        @Override
        public void process(@NonNull Frame frame) {
            if (!isRecording) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFrameTime >= 1000) {
                lastFrameTime = currentTime;
                Log.d(TAG, "---------X---------");
                Log.d(TAG, "Processing frame");
                Log.d(TAG, "Frame size: " + frame.getSize());
                Log.d(TAG, "Frame Data: " + frame.getData());
                Log.d(TAG, "Frame class: " + frame.getDataClass());

                int width = frame.getSize().getWidth();
                int height = frame.getSize().getHeight();

                Bitmap bitmap = null;
                if (frame.getDataClass() == byte[].class) {
                    Log.d(TAG, "Frame is in BYTES");
                    byte[] data = frame.getData();
                    bitmap = createBitmapFromFrame(data, width, height);
                    if (bitmap != null) {
                        updateImageView(bitmap);
                        addBitmapToList(bitmap);
                    }
                    printBitmapList();
                } else {
                    Log.d(TAG, "Frame is in IMAGE format");
                    Image image = frame.getData();
                    bitmap = createBitmapFromImage(image);
                    imageView.setImageBitmap(bitmap);
                }



            }
        }
    };

   //Update ImageView
    private void updateImageView(Bitmap bitmap) {
        runOnUiThread(() -> {
            imageView.setImageBitmap(bitmap);
            Log.d(TAG, "ImageView updated with new frame");
        });
    }

    //Create Bitmap from Frame
    public static Bitmap createBitmapFromFrame(byte[] frameData, int width, int height) {
        // Convert YUV_420_888 to ARGB_8888
        YuvImage yuvImage = new YuvImage(frameData, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        byte[] imageBytes = out.toByteArray();

        // Create Bitmap from the converted ARGB_8888 data
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Log.d(TAG, "Bitmap created");
        Log.d(TAG, "Bitmap: " + bitmap);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap portraitBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(),bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        return portraitBitmap;
    }

    //Create Bitmap from Image
    private Bitmap createBitmapFromImage(Image image) {
        if (image == null) return null;

        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes));

        image.close();

        return bitmap;
    }

    //Add Bitmap to List
    private void addBitmapToList(Bitmap bitmap) {
        Log.d(TAG, "Adding bitmap to list");
        runOnUiThread(() -> {
            synchronized (bitmapList) {
                if (bitmap != null) {
                    bitmapList.add(bitmap);
                    frameAdapter.notifyItemInserted(bitmapList.size() - 1);
                    Log.d(TAG, "Bitmap added, list size: " + bitmapList.size());
                } else {
                    Log.d(TAG, "Bitmap is null, not adding to list");
                }
            }
        });
    }

    //Check if List of Bitmap is empty, if not then print the list of Bitmap
    private void printBitmapList() {
        if (bitmapList.isEmpty()) {
            Log.d(TAG, "Bitmap list is empty");
        } else {
            for (int i = 0; i < bitmapList.size(); i++) {
                Bitmap bitmap = bitmapList.get(i);
                Log.d(TAG, "Bitmap " + i + ": Width = " + bitmap.getWidth() + ", Height = " + bitmap.getHeight() + ", HashCode = " + bitmap.hashCode());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.destroy();
        }
    }


}
//public static Bitmap createBitmapFromFrame(byte[] frameData, int width, int height) {
//    // Convert YUV_420_888 to ARGB_8888
//    YuvImage yuvImage = new YuvImage(frameData, ImageFormat.NV21, width, height, null); // Assuming NV21 as a common YUV format
//    ByteArrayOutputStream out = new ByteArrayOutputStream();
//    yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
//    byte[] imageBytes = out.toByteArray();
//    int bufferSize = width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
//    ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
//    buffer.put(frameData);
//    buffer.rewind();
//
//    // Create Bitmap from the converted ARGB_8888 data
//    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
//    Log.d(TAG, "Bitmap created");
//    Log.d(TAG, "Bitmap: " + bitmap);
//    bitmap.copyPixelsFromBuffer(buffer);
//    Log.d(TAG, "Bitmap copied from buffer: " + bitmap);
//    return bitmap;
//}
