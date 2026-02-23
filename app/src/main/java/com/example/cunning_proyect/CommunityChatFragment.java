package com.example.cunning_proyect;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
    private DataOutputStream dos;
    private DataInputStream dis;

    private final String SERVER_IP = "10.0.2.2"; // Emulador ve la PC
    private final int SERVER_PORT = 12345;

    private String username = "UsuarioAndroid"; // valor por defecto

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
                // --- AGREGAR MENSAJE LOCAL ---
                chatMessages.add(new ChatMessage(username, messageText, true));
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                rvChat.scrollToPosition(chatMessages.size() - 1);

                // --- ENVIAR MENSAJE AL SERVIDOR ---
                sendMessageToServer(messageText);
                etMessage.setText("");
            }
        });

        // Inicializar username desde Firebase y conectar al servidor
        initUsernameFromFirebase();
    }

    // ------------------------
    // OBTENER NOMBRE DE USUARIO DESDE FIREBASE CON DETECCIÓN DE RED
    // ------------------------
    private void initUsernameFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            username = currentUser.getDisplayName();

            // Verificar si hay conexión a internet
            ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            if (isConnected) {
                // Firestore solo si hay internet
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users").document(currentUser.getUid()).get()
                        .addOnSuccessListener(document -> {
                            if (document.exists()) {
                                String nameFromFirestore = document.getString("username");
                                if (nameFromFirestore != null && !nameFromFirestore.isEmpty()) {
                                    username = nameFromFirestore;
                                }
                            }
                            connectToServer();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "No se pudo obtener username de Firestore", e);
                            connectToServer(); // conectar incluso si falla
                        });
            } else {
                // No hay internet, usar valor por defecto
                Log.d(TAG, "No hay red. Usando username por defecto: " + username);
                connectToServer();
            }
        } else {
            // Usuario no logueado
            connectToServer();
        }
    }

    // ------------------------
    // CONEXIÓN AL SERVIDOR USANDO DataOutputStream/DataInputStream
    // ------------------------
    private void connectToServer() {
        new Thread(() -> {
            try {
                Log.d(TAG, "Intentando conectar al servidor...");
                socket = new Socket(SERVER_IP, SERVER_PORT);
                dos = new DataOutputStream(socket.getOutputStream());
                dis = new DataInputStream(socket.getInputStream());

                Log.d(TAG, "Conectado al servidor correctamente");

                listenForMessages();

            } catch (Exception e) {
                Log.e(TAG, "ERROR al conectar con el servidor. ¿Está el host activo?");
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
                while (true) {
                    message = dis.readUTF(); // usar DataInputStream
                    Log.d(TAG, "Mensaje recibido: " + message);

                    String sender = "Otro";
                    String content = message;

                    if (message.contains(": ")) {
                        String[] parts = message.split(": ", 2);
                        sender = parts[0];
                        content = parts[1];
                    }

                    String finalSender = sender;
                    String finalContent = content;

                    // Solo agregar si no es tu propio mensaje
                    if (!finalSender.equals(username)) {
                        uiHandler.post(() -> {
                            chatMessages.add(new ChatMessage(finalSender, finalContent, false));
                            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                            rvChat.scrollToPosition(chatMessages.size() - 1);
                        });
                    }
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
        new Thread(() -> {
            try {
                Log.d(TAG, "Enviando mensaje: " + message);
                dos.writeUTF(username + ": " + message); // usar DataOutputStream
                dos.flush();
            } catch (Exception e) {
                Log.e(TAG, "Error al enviar mensaje");
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