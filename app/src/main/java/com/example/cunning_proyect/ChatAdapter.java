package com.example.cunning_proyect;

import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private ArrayList<ChatMessage> messages;

    public ChatAdapter(ArrayList<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Creamos el layout del mensaje por código para no crear otro XML
        LinearLayout layout = new LinearLayout(parent.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setPadding(8, 8, 8, 8);

        CardView card = new CardView(parent.getContext());
        card.setRadius(24f);
        card.setCardElevation(0f);

        TextView tv = new TextView(parent.getContext());
        tv.setPadding(24, 16, 24, 16);
        tv.setTextSize(16f);
        tv.setTextColor(Color.WHITE);

        card.addView(tv);
        layout.addView(card);

        return new ChatViewHolder(layout, layout, card, tv);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.textView.setText(msg.getText());

        if (msg.isUser()) {
            // USUARIO: Derecha, Azul
            holder.container.setGravity(Gravity.END);
            holder.card.setCardBackgroundColor(Color.parseColor("#2563EB"));
            ((LinearLayout.LayoutParams) holder.card.getLayoutParams()).setMargins(100, 0, 0, 0);
        } else {
            // BOT: Izquierda, Gris
            holder.container.setGravity(Gravity.START);
            holder.card.setCardBackgroundColor(Color.parseColor("#374151"));
            ((LinearLayout.LayoutParams) holder.card.getLayoutParams()).setMargins(0, 0, 100, 0);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        CardView card;
        TextView textView;

        public ChatViewHolder(View itemView, LinearLayout container, CardView card, TextView textView) {
            super(itemView);
            this.container = container;
            this.card = card;
            this.textView = textView;
        }
    }
}