import okhttp3.*;
import com.google.gson.*;
import java.util.*;

public class SpotifyService {

    OkHttpClient client = new OkHttpClient();
    Gson gson = new Gson();
    private SpotifyAuth auth;

    public SpotifyService() {
        this.auth = new SpotifyAuth();
    }

    public List<Models.Track> getTracks(String playlistId) throws Exception {
        List<Models.Track> list = new ArrayList<>();
        String token = auth.getAccessToken();

        // URL base da API do Spotify
        // ADICIONE A BARRA "/" DEPOIS DE playlists
        String url = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks";

        // DICA DE OURO PARA ADS: Adicione um print para conferir se a URL está bonita
        // no terminal
        System.out.println("Acessando: " + url);

        Request req = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response res = client.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new Exception("Erro na API do Spotify: " + res.code());

            String body = res.body().string();
            JsonObject json = gson.fromJson(body, JsonObject.class);
            JsonArray items = json.getAsJsonArray("items");

            for (JsonElement e : items) {
                JsonObject itemObj = e.getAsJsonObject();
                if (itemObj.get("track").isJsonNull())
                    continue;

                JsonObject trackObj = itemObj.getAsJsonObject("track");
                String name = trackObj.get("name").getAsString();

                // Pega o nome do primeiro artista
                String artist = trackObj.getAsJsonArray("artists")
                        .get(0).getAsJsonObject().get("name").getAsString();

                list.add(new Models.Track(name, artist));
            }
        }
        return list;
    }
}