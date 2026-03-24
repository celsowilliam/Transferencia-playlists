import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {

        String spotifyPlaylistId = "37i9dQZF1EIXS32nOqD9Nf";

        SpotifyService spotify = new SpotifyService();
        YouTubeService youtube = new YouTubeService();

        System.out.println("Iniciando busca de músicas no Spotify...");
        List<Models.Track> tracks = spotify.getTracks(spotifyPlaylistId);

        System.out.println("Criando playlist no YouTube...");
        String playlistId = youtube.createPlaylist("Importada do Spotify");

        for (Models.Track t : tracks) {
            String query = t.title + " " + t.artist;
            String videoId = youtube.searchVideo(query);

            if (videoId != null) {
                youtube.addToPlaylist(playlistId, videoId);
                System.out.println("✔ " + t.title);
            } else {
                System.out.println("✖ Não encontrado: " + t.title);
            }
        }

        System.out.println("\nSincronização Finalizada com sucesso!");
    }
}