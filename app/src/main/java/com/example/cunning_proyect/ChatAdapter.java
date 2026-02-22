package com.example.cunning_proyect;

import android.graphics.Color;
import android.view.Gravity;
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
        // Layout principal vertical
        LinearLayout layout = new LinearLayout(parent.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setPadding(8, 8, 8, 8);

        // TextView para el nombre del usuario
        TextView tvSender = new TextView(parent.getContext());
        tvSender.setTextSize(12f);
        tvSender.setTextColor(Color.LTGRAY);

        // CardView para el mensaje
        CardView card = new CardView(parent.getContext());
        card.setRadius(24f);
        card.setCardElevation(0f);

        // TextView para el contenido
        TextView tvMessage = new TextView(parent.getContext());
        tvMessage.setPadding(24, 16, 24, 16);
        tvMessage.setTextSize(16f);
        tvMessage.setTextColor(Color.WHITE);

        card.addView(tvMessage);
        layout.addView(tvSender);
        layout.addView(card);

        return new ChatViewHolder(layout, layout, tvSender, card, tvMessage);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);

        // Mostrar nombre y mensaje
        holder.tvSender.setText(msg.getSender());
        holder.tvMessage.setText(msg.getText());

        // Ajustar alineación y colores según sea propio o de otro
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        if (msg.isUser()) {
            holder.container.setGravity(Gravity.END);
            holder.card.setCardBackgroundColor(Color.parseColor("#2563EB")); // Azul
            cardParams.setMargins(100, 0, 0, 0);
        } else {
            holder.container.setGravity(Gravity.START);
            holder.card.setCardBackgroundColor(Color.parseColor("#374151")); // Gris
            cardParams.setMargins(0, 0, 100, 0);
        }

        holder.card.setLayoutParams(cardParams);
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        TextView tvSender;
        CardView card;
        TextView tvMessage;

        public ChatViewHolder(@NonNull View itemView, LinearLayout container,
                              TextView tvSender, CardView card, TextView tvMessage) {
            super(itemView);
            this.container = container;
            this.tvSender = tvSender;
            this.card = card;
            this.tvMessage = tvMessage;
        }
    }
}