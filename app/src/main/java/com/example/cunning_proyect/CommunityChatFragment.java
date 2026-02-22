package com.example.cunning_proyect;

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
                sendMessageToServer(messageText); // Enviar al servidor
                etMessage.setText("");
            }
        });

        // Inicializar username desde Firebase y conectar al servidor
        initUsernameFromFirebase();
    }

    // ------------------------
    // OBTENER NOMBRE DE USUARIO DESDE FIREBASE
    // ------------------------
    private void initUsernameFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Opción 1: usar DisplayName de FirebaseAuth
            username = currentUser.getDisplayName();

            // Opción 2: usar Firestore si guardas nombre personalizado
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
            // Usuario no logueado, conectar con valor por defecto
            connectToServer();
        }
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
                Log.e(TAG, "ERROR al conectar con el servidor. ¿Está el host activo?");
                e.printStackTrace();
            }
        }).start();
    }

    // ------------------------
    // ESCUCHAR MENSAJES DE TODOS LOS CLIENTES
    // ------------------------
    private void listenForMessages() {
        new Thread(() -> {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
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

                    uiHandler.post(() -> {
                        // isUser = true si el remitente es el cliente actual
                        chatMessages.add(new ChatMessage(finalSender, finalContent, finalSender.equals(username)));
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
    // ENVIAR MENSAJE AL SERVIDOR
    // ------------------------
    private void sendMessageToServer(String message) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Enviando mensaje: " + message);
                writer.write(username + ": " + message); // enviamos nombre + contenido
                writer.newLine();
                writer.flush();
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