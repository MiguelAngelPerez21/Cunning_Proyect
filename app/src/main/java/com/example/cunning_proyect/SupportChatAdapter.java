package com.example.cunning_proyect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SupportChatAdapter extends RecyclerView.Adapter<SupportChatAdapter.ViewHolder> {

    private List<ChatMessage> chatList;

    public SupportChatAdapter(List<ChatMessage> chatList) {
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos el diseño doble que creamos antes
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_support_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage message = chatList.get(position);

        // 🔥 LA MAGIA: OCULTAR Y MOSTRAR BURBUJAS 🔥
        if (message.isUser()) {
            // Si es el USUARIO: Mostramos la derecha (azul), ocultamos la izquierda (gris)
            holder.layoutUserMessage.setVisibility(View.VISIBLE);
            holder.layoutBotMessage.setVisibility(View.GONE);

            holder.tvUserText.setText(message.getText());
        } else {
            // Si es el BOT: Mostramos la izquierda (gris), ocultamos la derecha (azul)
            holder.layoutBotMessage.setVisibility(View.VISIBLE);
            holder.layoutUserMessage.setVisibility(View.GONE);

            holder.tvBotText.setText(message.getText());
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    // Vinculamos los IDs del XML (item_support_message.xml)
    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutBotMessage, layoutUserMessage;
        TextView tvBotText, tvUserText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutBotMessage = itemView.findViewById(R.id.layoutBotMessage);
            layoutUserMessage = itemView.findViewById(R.id.layoutUserMessage);
            tvBotText = itemView.findViewById(R.id.tvBotText);
            tvUserText = itemView.findViewById(R.id.tvUserText);
        }
    }
}