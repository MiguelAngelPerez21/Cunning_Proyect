package com.example.cunning_proyect;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class SupportChatFragment extends Fragment {

    private ArrayList<ChatMessage> chatMessages = new ArrayList<>();
    private SupportChatAdapter chatAdapter;
    private RecyclerView rvChat;
    private EditText etMessage;

    private String username = "Usuario"; // Nombre del usuario actual
    private String botName = "María";    // Nombre del bot

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_support_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChat = view.findViewById(R.id.rvSupportChat);
        etMessage = view.findViewById(R.id.etSupportMessage);
        View btnSend = view.findViewById(R.id.btnSendSupport);
        View btnBack = view.findViewById(R.id.btnBackSupport);

        // CONFIGURAR RECYCLER
        chatAdapter = new SupportChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(chatAdapter);

        // BOTÓN ATRÁS
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        // MENSAJE DE BIENVENIDA DEL BOT
        if (chatMessages.isEmpty()) {
            chatMessages.add(new ChatMessage(
                    botName,
                    "¡Hola! Soy María, tu asistente virtual de Cunning. ¿En qué puedo ayudarte hoy?",
                    false // es el bot, no el usuario
            ));
            chatAdapter.notifyDataSetChanged();
        }

        // BOTÓN ENVIAR
        btnSend.setOnClickListener(v -> {
            String messageText = etMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                // Añadir mensaje del usuario
                chatMessages.add(new ChatMessage(username, messageText, true));
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                rvChat.scrollToPosition(chatMessages.size() - 1);
                etMessage.setText("");

                // AQUÍ IRÍA LA LLAMADA A LA API DEL BOT / GEMINI / OPENAI
                // Ejemplo de respuesta simulada del bot:
                simulateBotResponse("Estoy procesando tu mensaje...");
            }
        });
    }

    // Función opcional para simular respuesta del bot
    private void simulateBotResponse(String responseText) {
        chatMessages.add(new ChatMessage(botName, responseText, false));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rvChat.scrollToPosition(chatMessages.size() - 1);
    }
}