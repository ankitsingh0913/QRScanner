package com.xclone.qrscanner;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FrameAdapter extends RecyclerView.Adapter<FrameAdapter.FrameViewHolder> {

    private final List<Bitmap> frameList;

    public FrameAdapter(List<Bitmap> frameList) {
        this.frameList = frameList;
    }

    @NonNull
    @Override
    public FrameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.frame_item, parent, false);
        return new FrameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FrameViewHolder holder, int position) {
        Bitmap frame = frameList.get(position);
        holder.imageView.setImageBitmap(frame);
    }

    @Override
    public int getItemCount() {
        return frameList.size();
    }

    static class FrameViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public FrameViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.frameImage);
        }
    }
}
