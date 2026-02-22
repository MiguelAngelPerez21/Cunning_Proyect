package com.example.cunning_proyect;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SupportFragment extends Fragment {

    private RecyclerView rvChat;
    private ChatAdapter adapter;
    private ArrayList<ChatMessage> messages = new ArrayList<>();
    private EditText etMessage;

    private OpenAIService aiService;

    private final String username = "Usuario"; // Nombre del usuario
    private final String botName = "CunningBot"; // Nombre del bot

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_support_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializamos el servicio de IA
        aiService = new OpenAIService();

        rvChat = view.findViewById(R.id.rvSupportChat);
        etMessage = view.findViewById(R.id.etSupportMessage);
        View btnSend = view.findViewById(R.id.btnSendSupport);

        adapter = new ChatAdapter(messages);
        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChat.setAdapter(adapter);

        // Mensaje de bienvenida del bot
        addBotMessage("¡Hola! Soy CunningBot 🤖. ¿En qué te puedo ayudar hoy?");

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                // Mensaje del usuario
                messages.add(new ChatMessage(username, text, true));
                adapter.notifyItemInserted(messages.size() - 1);
                rvChat.scrollToPosition(messages.size() - 1);
                etMessage.setText("");

                // Llamada a la IA (Mistral)
                aiService.getResponse(text, new OpenAIService.AIResponseListener() {
                    @Override
                    public void onResponse(String reply) {
                        addBotMessage(reply);
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                        addBotMessage("😴 La IA está durmiendo. Dale al botón de enviar otra vez para despertarla.");
                    }
                });
            }
        });
    }

    // Función para añadir mensaje del bot
    private void addBotMessage(String text) {
        messages.add(new ChatMessage(botName, text, false));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);
    }
}