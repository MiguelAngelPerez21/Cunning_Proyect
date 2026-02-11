package com.example.cunning_proyect;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

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
        // Inflamos el nuevo diseño
        return inflater.inflate(R.layout.fragment_community_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. VINCULAR LA INTERFAZ (Usando los nuevos IDs del diseño)
        rvChat = view.findViewById(R.id.rvChat);
        etMessage = view.findViewById(R.id.etMessageInput); // Nuevo ID de la caja de texto
        View btnSend = view.findViewById(R.id.btnSendMsg); // Nuevo ID del botón azul (Ahora es View/LinearLayout)

        // 2. CONFIGURAR CABECERA (Título y Botones de volver)
        TextView tvTitle = view.findViewById(R.id.tvChatCommName);
        if (tvTitle != null && getArguments() != null) {
            // Pillamos el nombre que le pasamos desde el mapa
            String commName = getArguments().getString("COMM_NAME", "Chat Grupal");
            tvTitle.setText(commName);
        }

        // Botón Atrás (Flecha superior izquierda)
        view.findViewById(R.id.btnBackFromChat).setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        // Tab "Mapa de Incidencias" (Vuelve al mapa)
        view.findViewById(R.id.btnTabMap).setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        // 3. CONFIGURAR LA LISTA DE MENSAJES (RecyclerView)
        chatAdapter = new ChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // Que empiece desde abajo como WhatsApp
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(chatAdapter);

        // Mensaje de bienvenida falso (solo se añade si la lista está vacía)
        if (chatMessages.isEmpty()) {
            chatMessages.add(new ChatMessage("¡Bienvenido al chat de la comunidad!", false));
            chatAdapter.notifyDataSetChanged();
        }

        // 4. LÓGICA DEL BOTÓN DE ENVIAR
        btnSend.setOnClickListener(v -> {
            String messageText = etMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                // 'true' indica que es un mensaje tuyo
                chatMessages.add(new ChatMessage(messageText, true));

                // Actualizamos la lista y bajamos al último mensaje
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                rvChat.scrollToPosition(chatMessages.size() - 1);

                // Limpiamos la caja de texto
                etMessage.setText("");
            }
        });
    }
}