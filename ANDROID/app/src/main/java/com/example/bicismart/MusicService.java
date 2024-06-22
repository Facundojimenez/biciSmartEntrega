package com.example.bicismart;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class MusicService extends Service {
    private final IBinder binder = new LocalBinder();
    MediaPlayer reproductor;
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
        if(reproductor != null)
            reproductor.stop();
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

    public void setMusic(String music)
    {
        if(reproductor != null)
        {
            reproductor.release();
        }

        if(music.equals("Sad"))
        {
            reproductor = MediaPlayer.create(this, R.raw.sad_music);
            reproductor.setLooping(true);
            reproductor.start();
        }
        if(music.equals("Neutral"))
        {
            reproductor = MediaPlayer.create(this, R.raw.neutral_music);
            reproductor.setLooping(true);
            reproductor.start();
        }
        if(music.equals("Motivational"))
        {
            reproductor = MediaPlayer.create(this, R.raw.motivational_music);
            reproductor.setLooping(true);
            reproductor.start();
        }
    }

    public void playPauseMusic()
    {
        if(reproductor == null)
            return ;
        if(reproductor.isPlaying())
            reproductor.pause();
        else
            reproductor.start();
    }

    public void stopMusic(){
        if(reproductor != null)
            reproductor.stop();
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
            }
        }
    }

    public void startMusic()
    {
        if(reproductor != null)
            reproductor.release();
        reproductor = MediaPlayer.create(this, songList.get(0));
        reproductor.setLooping(true);
        reproductor.start();
    }

    public void nextSong()
    {
        int cantSong = songList.size();
        if(currentSong == cantSong-1)
            currentSong = 0;
        else
            currentSong++;

        if(reproductor != null)
            reproductor.release();
        else
            return;

        reproductor = MediaPlayer.create(this, songList.get(currentSong));
        reproductor.setLooping(true);
        reproductor.start();
    }

    private void showToast(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
