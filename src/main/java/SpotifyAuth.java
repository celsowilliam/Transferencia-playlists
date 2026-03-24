import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.Desktop;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;

public class SpotifyAuth {
    private static final String CLIENT_ID = "bb79a0ce8de5431c88931e14f860d897";
    private static final String CLIENT_SECRET = "cfc8b8a90edf4a09a6911d372da18ce2";
    private static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
    
    // URLs Oficiais da API do Spotify
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    
    // Escopos necessários para ler as músicas da sua playlist
    private static final String SCOPE = "playlist-read-private%20playlist-read-collaborative";
    private static final String TOKEN_FILE = "spotify_token.json";
    private final Gson gson = new Gson();

    public String getAccessToken() throws Exception {
        File file = new File(TOKEN_FILE);

        if (file.exists() && file.length() > 0) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject saved = gson.fromJson(reader, JsonObject.class);
                if (saved.has("refresh_token")) {
                    String refreshToken = saved.get("refresh_token").getAsString();
                    return refreshAccessToken(refreshToken);
                }
            } catch (Exception e) {
                System.out.println("Token antigo corrompido, iniciando nova autenticação...");
            }
        }
        return firstAuth();
    }

    private String firstAuth() throws Exception {
        // Monta a URL de Autorização
        String authUrl = AUTH_URL + "?client_id=" + CLIENT_ID + 
                         "&response_type=code&redirect_uri=" + REDIRECT_URI + 
                         "&scope=" + SCOPE;

        System.out.println("--- AUTENTICAÇÃO SPOTIFY ---");
        System.out.println("Abrindo o navegador para você autorizar o app...");
        
        // CHAMA O MÉTODO PARA ABRIR O BRAVE/CHROME
        abrirNavegador(authUrl);

        System.out.println("\n1. No navegador, clique em 'Aceito'.");
        System.out.println("2. Você será redirecionado para uma página de erro (é normal).");
        System.out.println("3. COPIE o código que aparece na URL após 'code='.");
        System.out.print("\nCole o código aqui e dê ENTER: ");

        Scanner scanner = new Scanner(System.in);
        String code = scanner.nextLine();
        
        return exchangeCodeForToken(code);
    }

    private String exchangeCodeForToken(String code) throws Exception {
        String data = "grant_type=authorization_code&code=" + code + "&redirect_uri=" + REDIRECT_URI;
        return sendTokenRequest(data);
    }

    private String refreshAccessToken(String refreshToken) throws Exception {
        String data = "grant_type=refresh_token&refresh_token=" + refreshToken;
        return sendTokenRequest(data);
    }

    private String sendTokenRequest(String data) throws Exception {
        URL url = new URL(TOKEN_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        String auth = CLIENT_ID + ":" + CLIENT_SECRET;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(data.getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() != 200) {
            throw new Exception("Erro ao obter token do Spotify: " + conn.getResponseCode());
        }

        Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";
        
        // Salva o JSON completo (com refresh_token) para uso futuro
        try (FileWriter writer = new FileWriter(TOKEN_FILE)) {
            writer.write(response);
        }

        JsonObject json = gson.fromJson(response, JsonObject.class);
        return json.get("access_token").getAsString();
    }

    private void abrirNavegador(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Caso o Java não detecte o Desktop (comum no Windows via terminal)
                Runtime runtime = Runtime.getRuntime();
                runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
            }
        } catch (Exception e) {
            System.out.println("Não foi possível abrir o navegador automaticamente.");
            System.out.println("Link para autorização: " + url);
        }
    }
}