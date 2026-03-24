import com.google.gson.*;
import okhttp3.*;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Base64;

public class SpotifyAuth {

    private static final String CLIENT_ID = "bb79a0ce8de5431c88931e14f860d897";
    private static final String CLIENT_SECRET = "cfc8b8a90edf4a09a6911d372da18ce2";
    private static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";

    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";

    private static final String SCOPE = "playlist-read-private playlist-read-collaborative";

    private static final String TOKEN_FILE = "spotify_token.json";

    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new Gson();

    public String getAccessToken() throws Exception {

        File file = new File(TOKEN_FILE);

        if (file.exists()) {
            JsonObject saved = gson.fromJson(new FileReader(file), JsonObject.class);

            String refreshToken = saved.get("refresh_token").getAsString();
            return refreshAccessToken(refreshToken);
        }

        return firstAuth();
    }

    private void abrirNavegador(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Caso o Java não consiga detectar o Desktop (comum em alguns ambientes)
                Runtime runtime = Runtime.getRuntime();
                runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
            }
        } catch (Exception e) {
            System.out.println("Não foi possível abrir o navegador automaticamente. Cole este link no Brave: " + url);
        }
    }

    private String firstAuth() throws Exception {

        String url = AUTH_URL + "?client_id=" + CLIENT_ID +
                "&response_type=code" +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8") +
                "&scope=" + URLEncoder.encode(SCOPE, "UTF-8");

        System.out.println("Abrindo navegador...");
        Desktop.getDesktop().browse(new URI(url));

        // servidor local para capturar o code
        ServerSocket server = new ServerSocket(8888);
        Socket socket = server.accept();

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String line = in.readLine();

        String code = line.split("code=")[1].split(" ")[0];

        String response = "HTTP/1.1 200 OK\r\n\r\nLogin realizado! Pode fechar.";
        socket.getOutputStream().write(response.getBytes());

        socket.close();
        server.close();

        return requestToken(code);
    }

    private String requestToken(String code) throws Exception {

        String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        String basic = Base64.getEncoder().encodeToString(credentials.getBytes());

        RequestBody body = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(body)
                .addHeader("Authorization", "Basic " + basic)
                .build();

        Response response = client.newCall(request).execute();

        String json = response.body().string();

        FileWriter writer = new FileWriter(TOKEN_FILE);
        writer.write(json);
        writer.close();

        JsonObject obj = gson.fromJson(json, JsonObject.class);

        return obj.get("access_token").getAsString();
    }

    private String refreshAccessToken(String refreshToken) throws Exception {

        String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        String basic = Base64.getEncoder().encodeToString(credentials.getBytes());

        RequestBody body = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(body)
                .addHeader("Authorization", "Basic " + basic)
                .build();

        Response response = client.newCall(request).execute();

        String json = response.body().string();

        JsonObject obj = gson.fromJson(json, JsonObject.class);

        return obj.get("access_token").getAsString();
    }
}