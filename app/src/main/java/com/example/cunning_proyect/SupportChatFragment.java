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
    // Asegúrate de que tu Adapter de soporte se llame así, si no, cámbialo
    private SupportChatAdapter chatAdapter;
    private RecyclerView rvChat;
    private EditText etMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflamos el nuevo diseño de soporte
        return inflater.inflate(R.layout.fragment_support_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. VINCULAR VISTAS (Con los nuevos IDs de soporte)
        rvChat = view.findViewById(R.id.rvSupportChat);
        etMessage = view.findViewById(R.id.etSupportMessage);
        View btnSend = view.findViewById(R.id.btnSendSupport); // Ahora es un View/LinearLayout
        View btnBack = view.findViewById(R.id.btnBackSupport);

        // 2. CONFIGURAR LA LISTA
        // IMPORTANTE: Asumo que tienes un 'SupportChatAdapter' que sabe usar el nuevo diseño
        // y ocultar/mostrar los layouts de bot/usuario según el mensaje.
        chatAdapter = new SupportChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(chatAdapter);

        // 3. BOTÓN ATRÁS
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        // 4. MENSAJE DE BIENVENIDA DEL BOT (Como en tu diseño)
        if (chatMessages.isEmpty()) {
            // 'false' significa que NO es el usuario (es el bot)
            chatMessages.add(new ChatMessage("¡Hola! Soy María, tu asistente virtual de Cunning. ¿En qué puedo ayudarte hoy?", false));
            chatAdapter.notifyDataSetChanged();
        }

        // 5. BOTÓN ENVIAR
        btnSend.setOnClickListener(v -> {
            String messageText = etMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                // 'true' significa que SÍ es el usuario
                chatMessages.add(new ChatMessage(messageText, true));
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                rvChat.scrollToPosition(chatMessages.size() - 1);
                etMessage.setText(""); // Limpiar caja

                // AQUÍ IRÍA LA LLAMADA A TU API DE GEMINI/OPENAI PARA QUE RESPONDA
                // ...
            }
        });
    }
}