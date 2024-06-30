package com.xclone.qrscanner;

import android.graphics.Bitmap;
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

import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.size.SizeSelectors;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private CameraView cameraView;
    private ImageView imageView;
    private List<Bitmap> frameList = new ArrayList<>();
    private FrameAdapter frameAdapter;
    private boolean isRecording = false;
    private TextView recordingIndicator;
    private long lastFrameTime = 0;

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

//        setupRecyclerView();
        setupCameraView();
        setupRecordingControls();
    }

//    private void setupRecyclerView() {
//        RecyclerView recyclerView = findViewById(R.id.recyclerView);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
//        frameAdapter = new FrameAdapter(frameList);
//        recyclerView.setAdapter(frameAdapter);
//    }

    private void setupCameraView() {
        cameraView = findViewById(R.id.camera);
        cameraView.setLifecycleOwner(this);
        cameraView.setPreviewStreamSize(SizeSelectors.and(SizeSelectors.maxHeight(1920), SizeSelectors.maxWidth(1080)));
    }

    private void setupRecordingControls() {
        recordingIndicator = findViewById(R.id.recordingIndicator);
        findViewById(R.id.startButton).setOnClickListener(view -> startRecording());
        findViewById(R.id.stopButton).setOnClickListener(view -> stopRecording());
    }

    private void startRecording() {
        isRecording = true;
        recordingIndicator.setVisibility(View.VISIBLE);
        lastFrameTime = 0;

//        cameraView.setFilter(Filters.BLACK_AND_WHITE.newInstance());
        cameraView.addFrameProcessor(frameProcessor);
        Log.d(TAG, "Recording started");
    }

    private void stopRecording() {
        isRecording = false;
        cameraView.clearFrameProcessors();
        recordingIndicator.setVisibility(View.GONE);
        Log.d(TAG, "Recording stopped");
    }

    private final FrameProcessor frameProcessor = new FrameProcessor() {
        @Override
        public void process(@NonNull Frame frame) {
            if (!isRecording) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFrameTime >= 1000) {
                lastFrameTime = currentTime;
                Log.d(TAG, "Processing frame");
                Log.d(TAG, "Frame size: " + frame.getSize());


                ByteBuffer buffer = frame.getData();
                int width = frame.getSize().getWidth();
                int height = frame.getSize().getHeight();

                if (frame.getDataClass() == byte[].class){
                    byte[] data = frame.getData();
                    buffer.get(data);
                    Bitmap bitmap = createBitmapFromFrame(data, width, height);
                    if (bitmap != null) {
//                    addFrameToList(bitmap);
                        updateImageView(bitmap);
                        startRecording();
                    }
                }
                else if (frame.getDataClass() == Image.class) {
                    Image image = frame.getData();
                    Bitmap bitmap = createBitmapFromImage(image);
                    if (bitmap != null) {
                        updateImageView(bitmap);
                    }
                }

            }
        }
    };

    private void updateImageView(Bitmap bitmap) {
        runOnUiThread(() -> {
            imageView.setImageBitmap(bitmap);
            Log.d(TAG, "ImageView updated with new frame");
        });
    }

    private Bitmap createBitmapFromFrame(byte[] data, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        try {
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(data));
            Log.d(TAG, "Bitmap created");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create bitmap from frame data", e);
            return null;
        }
        return bitmap;
    }

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

    private void addFrameToList(Bitmap bitmap) {
        runOnUiThread(() -> {
            synchronized (frameList) {
//                frameList.add(bitmap);
                frameList.add(bitmap);
                frameAdapter.notifyItemInserted(frameList.size() - 1);
            }
            Log.d(TAG, "Frame added to list, total frames: " + frameList.size());
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.destroy();
        }
    }


}
