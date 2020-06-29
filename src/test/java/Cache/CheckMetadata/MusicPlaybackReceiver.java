package Cache.CheckMetadata;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicPlaybackReceiver extends BroadcastReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(MusicPlaybackReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        String artist = intent.getStringExtra("artist");
        String album = intent.getStringExtra("album");
        String track = intent.getStringExtra("track");


        MusicSpec musicSpec = new MusicSpec();

        LOG.info("Current track: " + artist + ", " + album + ", " + track);

        musicSpec.artist = artist;
        musicSpec.album = album;
        musicSpec.track = track;

    }


}