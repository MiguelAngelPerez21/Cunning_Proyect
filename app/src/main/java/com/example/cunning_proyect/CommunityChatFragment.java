package com.example.cunning_proyect;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class CommunityChatFragment extends Fragment {

    private ArrayList<ChatMessage> chatMessages = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private RecyclerView rvChat;
    private EditText etMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community_chat, container, false);

        rvChat = view.findViewById(R.id.rvChat);
        etMessage = view.findViewById(R.id.etChatMessage);
        ImageButton btnSend = view.findViewById(R.id.btnSendChat);

        chatAdapter = new ChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // Que empiece desde abajo
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(chatAdapter);

        // Mensaje de prueba
        // Usa 'false' para indicar que NO es el usuario (es el sistema/bot)
        chatMessages.add(new ChatMessage("¡Bienvenido al chat...!", false));
        chatAdapter.notifyDataSetChanged();

        btnSend.setOnClickListener(v -> {
            String messageText = etMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                // Por ahora el usuario es "Yo", luego será el real
                // Ponemos 'true' para indicar que el mensaje lo envía el Usuario
                chatMessages.add(new ChatMessage(messageText, true));
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                rvChat.scrollToPosition(chatMessages.size() - 1);
                etMessage.setText(""); // Limpiar el input
            }
        });

        return view;
    }
}