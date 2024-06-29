package com.xclone.qrscanner;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private CameraView cameraView;
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

        setupRecyclerView();
        setupCameraView();
        setupRecordingControls();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        frameAdapter = new FrameAdapter(frameList);
        recyclerView.setAdapter(frameAdapter);
    }

    private void setupCameraView() {
        cameraView = findViewById(R.id.camera);
        cameraView.setLifecycleOwner(this);
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
            if (currentTime - lastFrameTime >= 1000) { // Process a frame every second
                lastFrameTime = currentTime;
                Log.d(TAG, "Processing frame");

                ByteBuffer buffer = frame.getData();
                int width = frame.getSize().getWidth();
                int height = frame.getSize().getHeight();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);

                Bitmap bitmap = createBitmapFromFrame(data, width, height);
                if (bitmap != null) {
                    addFrameToList(bitmap);
                }
            }
        }
    };

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

    private void addFrameToList(Bitmap bitmap) {
        runOnUiThread(() -> {
            synchronized (frameList) {
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
