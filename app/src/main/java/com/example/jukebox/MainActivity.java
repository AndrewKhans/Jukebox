/*
    Color on very top bar of screen is purple
 */

package com.example.jukebox;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TracksPager;

import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;


public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "<Insert your spotify key here>";
    private static final String REDIRECT_URI = "http://com.andrew.jukebox/callback";

    private static SpotifyAppRemote mSpotifyAppRemote;
    private static SpotifyApi api = new SpotifyApi();
    private static String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        MySmsReceiver receiver = new MySmsReceiver();
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[] {
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.INTERNET,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.READ_PHONE_NUMBERS,
                        Manifest.permission.READ_PHONE_STATE,}, 1);

        TelephonyManager tMgr = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        assert tMgr != null;

        phoneNumber = tMgr.getLine1Number();
        phoneNumber = phoneNumber.substring(0,3) + "-" + phoneNumber.substring(3,6) + "-" +phoneNumber.substring(6,10);
        Log.d("Pepis", "Your phone:" + phoneNumber);

        TextView phoneText = (TextView)findViewById(R.id.textView4);
        phoneText.setText("To add a song to the queue, text " + phoneNumber + "\n\"#queue SongName\"");

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
                    Log.d("MainActivity", "Connected!");

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
                    // Handle errors here, if necessary
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
        // If this message's format isn't right, return
        if (!message.toLowerCase().matches("#\\w+\\s.+")) return;
        // If the message doesn't contain a command, return
        String[] splits = message.toLowerCase().split(" ", 2);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                SpotifyService spotify = api.getService();

                try {
                    if (splits[0].equals("#play")) {
                        TracksPager tp = spotify.searchTracks(splits[1]);
                        Track track = tp.tracks.items.get(0);
                        Log.d("Pepis", "Playing " + track.name + "by " + track.artists.get(0));
                        mSpotifyAppRemote.getPlayerApi().play(track.uri);
                        Messenger.sendMessage(phoneNumber, "Playing " + track.name + " by " + track.artists.get(0).name);
                    } else if (splits[0].equals("#queue")) {
                        TracksPager tp = spotify.searchTracks(splits[1]);
                        Track track = tp.tracks.items.get(0);
                        Log.d("Pepis", "Queueing " + track.name);
                        mSpotifyAppRemote.getPlayerApi().queue(track.uri);
                        Messenger.sendMessage(phoneNumber, "Added " + track.name + " by " + track.artists.get(0).name + " to the queue");
                    } else {
                        Log.d("Pepis", "unrecognized command");
                    }
                } catch (Exception e) {
                    Log.d("Pepis", "Exception:" + e.toString());
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
                    String accessToken = response.getAccessToken();
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
