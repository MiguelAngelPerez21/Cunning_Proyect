package com.example.cunning_proyect;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.example.cunning_proyect.BuildConfig;
import com.example.cunning_proyect.BuildConfig;

public class OpenAIService {

    private static final String GOOGLE_API_KEY = BuildConfig.GEMINI_API_KEY;

    // Usamos v1beta, que es la casa nativa del modelo Flash.
    private static final String URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + GOOGLE_API_KEY;

    private final OkHttpClient client;

    public OpenAIService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public interface AIResponseListener {
        void onResponse(String text);
        void onError(String error);
    }

    public void getResponse(String userMessage, AIResponseListener listener) {
        JSONObject jsonBody = new JSONObject();
        try {
            // 🔥 AQUÍ ESTÁ EL SUPER PROMPT DETALLADO 🔥
            String systemPrompt = "Eres María, la asistente virtual experta de la aplicación Cunning. " +
                    "Cunning es una plataforma de colaboración ciudadana que permite a los vecinos reportar incidencias en tiempo real, " +
                    "crear y gestionar comunidades privadas, chatear y visualizar un mapa interactivo con códigos de colores para urgencias. " +
                    "La app destaca por su tecnología 'Offline-First' que permite funcionar sin conexión. " +
                    "TUS DIRECTRICES SON: " +
                    "1. Tono: Amable, cercano, paciente y muy resolutivo. " +
                    "2. Formato: Respuestas breves, directas, fáciles de leer y con emojis que acompañen el texto. " +
                    "3. Presentación: Si el usuario te saluda (ej: 'hola', 'buenas') o te pregunta quién eres, preséntate SIEMPRE primero " +
                    "diciendo: '¡Hola! Soy María, tu asistente de Cunning...' y explícale brevemente cómo puedes ayudarle con el mapa, las incidencias o su comunidad. " +
                    "4. Limítate a responder dudas sobre el uso de la app o asistencia general relacionada con seguridad vecinal y convivencia. " +
                    "\n\nMensaje del usuario: ";

            // Unimos las instrucciones de personalidad con lo que ha escrito el usuario
            String finalPrompt = systemPrompt + userMessage;

            JSONObject part = new JSONObject();
            part.put("text", finalPrompt);

            JSONArray parts = new JSONArray();
            parts.put(part);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            jsonBody.put("contents", contents);

        } catch (Exception e) {
            listener.onError("Error interno");
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> listener.onError("Sin conexión"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);

                        String botText = jsonResponse.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        String finalText = botText.trim();
                        new Handler(Looper.getMainLooper()).post(() -> listener.onResponse(finalText));

                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(() -> listener.onError("Error procesando respuesta"));
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "N/A";
                    Log.e("GEMINI_FAIL", "Código: " + response.code() + " Error: " + errorBody);

                    new Handler(Looper.getMainLooper()).post(() ->
                            listener.onError("Error " + response.code() + ": Crea una clave en 'New Project' en Google AI Studio")
                    );
                }
            }
        });
    }
}