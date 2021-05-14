package com.example.jukebox;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TracksPager;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import java.util.List;

//import static com.spotify.sdk.android.auth.LoginActivity.REQUEST_CODE;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "4e97f6ceceb24ef7ac68e5fd5764a966";
    private static final String REDIRECT_URI = "http://com.andrew.jukebox/callback";
    private static SpotifyAppRemote mSpotifyAppRemote;
    private static String accessToken = "";
    private static SpotifyApi api = new SpotifyApi();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MySmsReceiver receiver = new MySmsReceiver();
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.RECEIVE_SMS},
                1);
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.INTERNET},
                1);
        Log.d("Pepis", "start");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set the connection parameters
        ConnectionParams connectionParams =
            new ConnectionParams.Builder(CLIENT_ID)
                    .setRedirectUri(REDIRECT_URI)
                    .showAuthView(true)
                    .build();
    SpotifyAppRemote.connect(this, connectionParams,
            new Connector.ConnectionListener() {
                @Override
                public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                    mSpotifyAppRemote = spotifyAppRemote;
                    Log.d("MainActivity", "Connected! Yay!");

                    // Now you can start interacting with App Remote
                    try {
                        connected();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    Log.e("MainActivity", throwable.getMessage(), throwable);
                    // Something went wrong when attempting to connect! Handle errors here
                }
            }
        );
    }

    private void connected() throws InterruptedException {
        // Play a playlist
//        mSpotifyAppRemote.getPlayerApi().play("spotify:playlist:37i9dQZF1DX2sUQwD7tbmL");
//        mSpotifyAppRemote.getPlayerApi()
//                .subscribeToPlayerState()
//                .setEventCallback(playerState -> {
//                    final Track track = playerState.track;
//                    if (track != null) {
//                        TextView tv1 = (TextView)findViewById(R.id.textview);
//                        tv1.setText(("Playing" + track.name + " by " + track.artist.name));
//                    }
//                });


        this.login();
//        api.setAccessToken(accessToken);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

    public static void processMessage(String phoneNumber, String message) throws InterruptedException {
        Log.d("Pepis", "Processing message");
        if (!message.substring(0,6).toLowerCase().equals("#play ")
            && !message.substring(0,7).toLowerCase().equals("#queue ")) { return; }
        SpotifyService spotify = api.getService();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("Pepis", "Searching tracks");
                    TracksPager val = spotify.searchTracks(message.substring(6));
                    Pager<Track> val2 = val.tracks;
                    List<Track> val3 = val2.items;
                    Track val4 = val3.get(0);
                    String val5 = val4.uri;
                    Log.d("Pepis", val5);
                    String command = message.substring(0,6).toLowerCase();
                    if (command.equals("#play ")) {
                        mSpotifyAppRemote.getPlayerApi().play(val5);
                        Messenger.sendMessage(phoneNumber, message);
                    } else if (command.equals("#queue ")) {
                        mSpotifyAppRemote.getPlayerApi().queue(val5);
                    } else {
                        
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        thread.join();      // Wait for thread to finish
    }

    public void login(){
        int REQUEST_CODE = 1337;
        final String REDIRECT_URI = "http://com.andrew.jukebox/callback";

        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"streaming"});
        AuthorizationRequest request = builder.build();

        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == 1337) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    Log.d("Pepis", "got the token!");
                    accessToken = response.getAccessToken();
                    api.setAccessToken(accessToken);
                    // Handle successful response
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
            }
        }
    }
}
