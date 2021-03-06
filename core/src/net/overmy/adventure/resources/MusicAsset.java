/**
 * OVERMY.NET - Make your device live! *
 * <p/>
 * Games: http://play.google.com/store/apps/developer?id=OVERMY
 *
 * @author Andrey Mikheev (cb)
 */

package net.overmy.adventure.resources;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.math.MathUtils;

import net.overmy.adventure.DEBUG;

/*
     Created by Andrey Mikheev on 29.09.2017
     Contact me → http://vk.com/id17317
 */

public enum MusicAsset {
    WINDFILTER( "windfilter.ogg" ),
    FOREST( "forest.ogg" ),;

    private final String path;
    private        Music music = null;
    private static float timer = 0.01f;


    MusicAsset ( final String path ) {
        String DEFAULT_DIR = "music/";
        //String DEFAULT_EXT = ".mp3";
        this.path = DEFAULT_DIR + path;// + DEFAULT_EXT;
    }


/*

    public static void updateVolume( float newVolume ) {
        //float musicVolume = (float) Settings.MUSIC.getInteger() / 100.0f;
        currentTrack.setVolume( newVolume );
    }

*/


    public static void stopAll () {
        if ( DEBUG.ON_WINDOWS.get() ) {
            return;
        }

        for ( int i = 0; i < MusicAsset.values().length; i++ ) {
            if ( MusicAsset.values()[ i ].music != null ) {
                MusicAsset.values()[ i ].music.stop();
            }
        }
    }


    public static void build ( final AssetManager manager ) {
        if ( DEBUG.ON_WINDOWS.get() ) {
            return;
        }

        for ( int i = 0; i < MusicAsset.values().length; i++ ) {
            MusicAsset.values()[ i ].music = manager.get( MusicAsset.values()[ i ].path,
                                                          Music.class );
        }

        //currentTrack = MusicAsset.values()[ 0 ].music;
    }


    public static void load ( final AssetManager manager ) {
        if ( DEBUG.ON_WINDOWS.get() ) {
            return;
        }

        for ( int i = 0; i < MusicAsset.values().length; i++ ) {
            manager.load( MusicAsset.values()[ i ].path, Music.class );
        }
    }


    public static void unload ( final AssetManager manager ) {
        if ( DEBUG.ON_WINDOWS.get() ) {
            return;
        }

        //currentTrack = null;
        stopAll();
        for ( int i = 0; i < MusicAsset.values().length; i++ ) {

            if ( MusicAsset.values()[ i ].music != null ) {
                MusicAsset.values()[ i ].music.dispose();
                MusicAsset.values()[ i ].music = null;

                manager.unload( MusicAsset.values()[ i ].path );
            }
        }
    }


    private static int bars      = 0;
    private static int currentID = 0;


    public static void playRandom ( float delta ) {
        timer -= delta;

        if ( timer > 0 ) {
            return;
        }
        timer = 0.1f;
/*

        if ( currentTrack.isPlaying() ) {
            return;
        }
*/

        if ( bars > 0 ) {
            MusicAsset.values()[ currentID ].play();
            bars--;
        } else {
            bars = MathUtils.randomBoolean() ? 4 : 6;
            int tracks = MusicAsset.values().length;
            currentID = MathUtils.random( tracks - 1 );
            MusicAsset.values()[ currentID ].play();
        }
    }


    public void playNext ( float delta ) {
        timer -= delta;

        if ( timer > 0 ) {
            return;
        }
        timer = 0.1f;
/*

        if ( currentTrack.isPlaying() ) {
            return;
        }
*/

        //currentTrack.stop();
        this.play( true );
    }


    public void play ( boolean loop ) {
        if ( DEBUG.ON_WINDOWS.get() ) {
            return;
        }

   /*if ( currentTrack.isPlaying() ) {
            currentTrack.pause();
        }*/

        float musicVolume = (float) Settings.MUSIC.getInteger() / 100.0f;
        if ( musicVolume > 0 ) {
            //currentTrack = music;

            this.music.setVolume( musicVolume );
            this.music.setLooping( loop );

            Gdx.app.debug( "play( boolean loop ) volume" + musicVolume, "" + this.music );

            this.music.play();
        } else {
            stopAll();
        }
    }


    public void setPan ( float x ) {
        this.music.setPan( x, 1 );
    }


    public void play () {
        play( false );
    }


    public void stop () {
        this.music.stop();
    }

    // HARDCODE!
    //public Music get() { return Gdx.audio.newMusic( Gdx.files.internal( this.path ) ); }


    public Music get () {
        return this.music;
    }


    public void stopLoop () {
        if ( this.music.isPlaying() ) {
            this.music.setLooping( false );
        }
    }

}
