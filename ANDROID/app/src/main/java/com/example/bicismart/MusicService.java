package com.example.bicismart;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class MusicService extends Service {
    private final IBinder binder = new LocalBinder();
    MediaPlayer mediaPlayer;
    private ArrayList<Integer> songList = new ArrayList<>();
    int currentSong = 0;

    @Override
    public void onCreate()
    {
        super.onCreate();
        this.listRaw();
    }

    @Override
    public void onDestroy()
    {
        Toast.makeText(this,"Servicio detenido",Toast.LENGTH_SHORT).show();
        if(mediaPlayer != null)
            mediaPlayer.stop();
    }

    public class LocalBinder extends Binder
    {
        MusicService getService()
        {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setDynamicMusic(String music)
    {
        //Se utiliza para liberar la instancia del reproductor antes de cambiar a la otra
        if(mediaPlayer != null)
        {
            mediaPlayer.release();
        }

        if(music.equals("Sad"))
        {
            mediaPlayer = MediaPlayer.create(this, R.raw.sad_music);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }
        if(music.equals("Neutral"))
        {
            mediaPlayer = MediaPlayer.create(this, R.raw.neutral_music);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }
        if(music.equals("Motivational"))
        {
            mediaPlayer = MediaPlayer.create(this, R.raw.motivational_music);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }
    }

    public void toggleMusic()
    {
        if(mediaPlayer == null)
            return ;
        if(mediaPlayer.isPlaying())
            mediaPlayer.pause();
        else
            mediaPlayer.start();
    }

    public void stopMusic(){
        if(mediaPlayer != null)
            mediaPlayer.stop();
    }

    public void listRaw()
    {
        Field[] fields=R.raw.class.getDeclaredFields();
        for (Field field : fields)
        {
            try
            {
                songList.add(field.getInt(field));
            } catch (IllegalAccessException e)
            {
                Log.e("Raw list", "Error al leer lista de canciones", e);
            }
        }
    }

    public void startUserMusic()
    {
        if(mediaPlayer != null)
            mediaPlayer.release();
        mediaPlayer = MediaPlayer.create(this, songList.get(0));
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    public void nextSong()
    {
        int cantSong = songList.size();
        if(currentSong == cantSong-1)
            currentSong = 0;
        else
            currentSong++;

        if(mediaPlayer != null)
            mediaPlayer.release();
        else
            return;

        mediaPlayer = MediaPlayer.create(this, songList.get(currentSong));
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

}
