package com.example.cunning_proyect;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;

public class CommunityChatFragment extends Fragment {

    private static final String TAG = "CHAT_DEBUG";

    private ArrayList<ChatMessage> chatMessages = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private RecyclerView rvChat;
    private EditText etMessage;

    // SOCKET
    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    private final String SERVER_IP = "10.0.2.2";
    private final int SERVER_PORT = 12345;

    private String username; // ahora se obtiene desde SharedPreferences

    private Handler uiHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_community_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChat = view.findViewById(R.id.rvChat);
        etMessage = view.findViewById(R.id.etMessageInput);
        View btnSend = view.findViewById(R.id.btnSendMsg);

        TextView tvTitle = view.findViewById(R.id.tvChatCommName);
        if (tvTitle != null && getArguments() != null) {
            String commName = getArguments().getString("COMM_NAME", "Chat Grupal");
            tvTitle.setText(commName);
        }

        view.findViewById(R.id.btnBackFromChat).setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        view.findViewById(R.id.btnTabMap).setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        // CONFIGURAR RECYCLER
        chatAdapter = new ChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(chatAdapter);

        // BOTÓN ENVIAR
        btnSend.setOnClickListener(v -> {
            String messageText = etMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessageToServer(messageText);
                etMessage.setText("");
            }
        });

        // 👇 NUEVO: obtener username desde SharedPreferences
        initUsernameFromLocalStorage();
    }

    // ------------------------
    // OBTENER USERNAME DESDE SHARED PREFERENCES
    // ------------------------
    private void initUsernameFromLocalStorage() {

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("UserPrefs", getContext().MODE_PRIVATE);

        username = prefs.getString("username", "Usuario");

        Log.d(TAG, "Username cargado: " + username);

        connectToServer();
    }

    // ------------------------
    // CONEXIÓN AL SERVIDOR
    // ------------------------
    private void connectToServer() {
        new Thread(() -> {
            try {
                Log.d(TAG, "Intentando conectar al servidor...");
                socket = new Socket(SERVER_IP, SERVER_PORT);
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                Log.d(TAG, "Conectado al servidor correctamente");

                listenForMessages();

            } catch (Exception e) {
                Log.e(TAG, "ERROR al conectar con el servidor");
                e.printStackTrace();
            }
        }).start();
    }

    // ------------------------
    // ESCUCHAR MENSAJES
    // ------------------------
    private void listenForMessages() {
        new Thread(() -> {
            try {
                String message;
                while ((message = reader.readLine()) != null) {

                    String sender = "";
                    String content = message;

                    if (message.contains(": ")) {
                        String[] parts = message.split(": ", 2);
                        sender = parts[0].trim();
                        content = parts[1].trim();
                    }

                    String finalSender = sender;
                    String finalContent = content;

                    uiHandler.post(() -> {

                        // 🔥 IGNORAR si el mensaje es mío
                        if (finalSender.equals(username)) {
                            return;
                        }

                        // 🔹 Mensaje normal
                        if (!finalSender.isEmpty()) {
                            chatMessages.add(new ChatMessage(
                                    finalSender,
                                    finalContent,
                                    false
                            ));
                        }
                        // 🔹 Mensaje del sistema (entra/sale)
                        else {
                            chatMessages.add(new ChatMessage(
                                    "Sistema",
                                    finalContent,
                                    false
                            ));
                        }

                        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                        rvChat.scrollToPosition(chatMessages.size() - 1);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Se perdió la conexión con el servidor");
                e.printStackTrace();
            }
        }).start();
    }

    // ------------------------
    // ENVIAR MENSAJE
    // ------------------------
    private void sendMessageToServer(String message) {

        // Mostrar propio mensaje
        uiHandler.post(() -> {
            chatMessages.add(new ChatMessage(username, message, true));
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            rvChat.scrollToPosition(chatMessages.size() - 1);
        });

        new Thread(() -> {
            try {
                writer.write(username + ": " + message);
                writer.newLine();
                writer.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (socket != null) {
                socket.close();
                Log.d(TAG, "Socket cerrado. Cliente desconectado.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al cerrar conexión");
            e.printStackTrace();
        }
    }
}