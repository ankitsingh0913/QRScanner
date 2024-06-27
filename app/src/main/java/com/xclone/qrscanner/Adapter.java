package com.xclone.qrscanner;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private final List<Bitmap> frameList;

    public Adapter(List<Bitmap> frameList) {
        this.frameList = frameList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.frames, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.frameImageView.setImageBitmap(frameList.get(position));
    }

    @Override
    public int getItemCount() {
        return frameList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView frameImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            frameImageView = itemView.findViewById(R.id.frameImage);
        }
    }
}
