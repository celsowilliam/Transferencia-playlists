import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory; // Use GsonFactory em vez de Jackson
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;

import java.io.*;
import java.util.*;

public class YouTubeService {

    private static final String APPLICATION_NAME = "Spotify Sync";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList("https://www.googleapis.com/auth/youtube");

    private YouTube youtube;

    public YouTubeService() throws Exception {
        Credential credential = authorize();
        youtube = new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential
        ).setApplicationName(APPLICATION_NAME).build();
    }

    private Credential authorize() throws Exception {
    InputStream in = new FileInputStream("credentials.json");
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JSON_FACTORY,
            clientSecrets,
            SCOPES)
            .setDataStoreFactory(new com.google.api.client.util.store.FileDataStoreFactory(new File("tokens")))
            .setAccessType("offline")
            .build();

    // O LocalServerReceiver tenta abrir o navegador padrão do Windows (se for o Brave, abrirá o Brave)
    return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
}

    public String createPlaylist(String title) throws Exception {
        PlaylistSnippet snippet = new PlaylistSnippet();
        snippet.setTitle(title);

        PlaylistStatus status = new PlaylistStatus();
        status.setPrivacyStatus("private");

        Playlist playlist = new Playlist();
        playlist.setSnippet(snippet);
        playlist.setStatus(status);

        Playlist response = youtube.playlists()
                .insert(Arrays.asList("snippet", "status"), playlist)
                .execute();

        return response.getId();
    }

    public String searchVideo(String query) throws Exception {
        YouTube.Search.List search = youtube.search().list(Arrays.asList("snippet"));
        search.setQ(query);
        search.setMaxResults(1L);
        search.setType(Arrays.asList("video"));

        SearchListResponse response = search.execute();
        if (response.getItems().isEmpty()) return null;

        return response.getItems().get(0).getId().getVideoId();
    }

    public void addToPlaylist(String playlistId, String videoId) throws Exception {
        ResourceId resourceId = new ResourceId();
        resourceId.setKind("youtube#video");
        resourceId.setVideoId(videoId);

        PlaylistItemSnippet snippet = new PlaylistItemSnippet();
        snippet.setPlaylistId(playlistId);
        snippet.setResourceId(resourceId);

        PlaylistItem item = new PlaylistItem();
        item.setSnippet(snippet);

        youtube.playlistItems()
                .insert(Arrays.asList("snippet"), item)
                .execute();
    }
}