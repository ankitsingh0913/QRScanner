package com.xclone.qrscanner;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private Handler frameHandler;
    private Runnable frameRunnable;
    private List<Bitmap> frameList = new ArrayList<>();
    private Adapter frameAdapter;
    private boolean isRecording = false;
    private TextView recordingIndicator;
    private long lastFrameTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        frameAdapter = new Adapter(frameList);
        recyclerView.setAdapter(frameAdapter);

        recordingIndicator = findViewById(R.id.recordingIndicator);
        findViewById(R.id.startButton).setOnClickListener(view -> startRecording());
        findViewById(R.id.stopButton).setOnClickListener(view -> stopRecording());

        cameraView = findViewById(R.id.camera);
        cameraView.setLifecycleOwner(this);
    }

    private void startRecording() {
        isRecording = true;
        recordingIndicator.setVisibility(View.VISIBLE);
        lastFrameTime = 0;
        cameraView.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull Frame frame) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastFrameTime >= 1000){
                    lastFrameTime = currentTime;
                    Log.d(TAG, "Processing frame");
                    Log.d(TAG, "Frame format: " + frame.getFormat());
                    Log.d(TAG, "Frame size: " + frame.getSize().getWidth() + "x" + frame.getSize().getHeight());

                    ByteBuffer buffer = frame.getData();
                    int width = frame.getSize().getWidth();
                    int height = frame.getSize().getHeight();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    Log.d(TAG, "buffer: " + data.length);
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(data));
                    Log.d(TAG, "Bitmap created");

                    runOnUiThread(() -> {
                        synchronized (frameList) {
                            frameList.add(bitmap);
                            frameAdapter.notifyItemInserted(frameList.size() - 1);
                        }
                        Log.d(TAG, "Frame added to list, total frames: " + frameList.size());
                    });
                }
            }
        });

        frameHandler = new Handler(Looper.getMainLooper());
        frameHandler.post(frameRunnable);
        Log.d(TAG, "Recording started");
    }


    private void stopRecording() {
        isRecording = false;
        if (frameHandler != null && frameRunnable != null) {
            frameHandler.removeCallbacks(frameRunnable);
            recordingIndicator.setVisibility(View.GONE);
        }
        Log.d(TAG, "Recording stopped");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.destroy();
        }
    }
}