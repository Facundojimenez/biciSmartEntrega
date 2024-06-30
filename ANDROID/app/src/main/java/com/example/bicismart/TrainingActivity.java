package com.example.bicismart;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

public class TrainingActivity extends AppCompatActivity implements SensorEventListener
{
    TextView tvDuration, tvIntensity, tvBuzzer, tvDynamicMusic, tvAddress, tvTrainingType;
    static TextView tvStatus;
    //Parametros
    int duration;
    String intensity;
    boolean enableBuzzer, trainingByTime;
    static boolean enableDynamicMusic;
    //Musica
    static MusicService mService;
    static boolean mMediaPlayerServiceIsBound = false;
    static boolean firstSong;
    //Volumen
    AudioManager audioManager;
    //Sensor proximidad
    private SensorManager mSensorManager;
    float lastProximitySensor;
    boolean firstReading = true;
    //Reiniciar entrenamiento
    static Button btnRestart;
    static boolean TRAINING_FINISHED;
    //Pausar entrenamiento
    boolean trainingPaused = false;
    //Bluetooth
    Handler bluetoothIn;
    final int handlerState = 0;
    private SingletonSocket mSocket;
    private final StringBuilder recDataString = new StringBuilder();
    private ConnectedThread mConnectedThread;
    private static String address = null;

    @SuppressLint({"SetTextI18n", "MissingPermission"})
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trainning);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) ->
        {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firstSong = true;

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        TRAINING_FINISHED = false;
        tvDuration = findViewById(R.id.tv_duracion);
        tvIntensity = findViewById(R.id.tv_intensidad);
        tvBuzzer = findViewById(R.id.tv_buzzer);
        tvDynamicMusic = findViewById(R.id.tv_MusicaDin);
        tvAddress = findViewById(R.id.tvAddress);
        tvTrainingType = findViewById(R.id.tv_tipoEntrenamiento);
        tvStatus = findViewById(R.id.tv_estado);


        btnRestart = findViewById(R.id.btn_restart);
        btnRestart.setText("Cancelar Entrenamiento");
        btnRestart.setEnabled(false);

        Bundle bundle =getIntent().getExtras();
        address = bundle.getString("Direccion_Bluetooth");
        duration = bundle.getInt("Duracion");
        intensity = bundle.getString("Intensidad");
        enableBuzzer = bundle.getBoolean("Buzzer");

        enableDynamicMusic = bundle.getBoolean("Musica Dinamica");
        trainingByTime = bundle.getBoolean("Por Tiempo");

        tvTrainingType.setText("Por Tiempo: " + trainingByTime);
        tvDuration.setText("Duracion: " + duration);
        tvIntensity.setText("Intensidad: " + intensity);
        tvBuzzer.setText("Buzzer: " + enableBuzzer);

        tvDynamicMusic.setText("Musica Dinamica: " + enableDynamicMusic);
        tvAddress.setText("Address: " + address);

        btnRestart.setOnClickListener(v -> {
            Intent intent = new Intent(TrainingActivity.this, PreTrainingActivity.class);
            intent.putExtra("Direccion_Bluetooth", address);
            if (!TRAINING_FINISHED){
                mConnectedThread.write("CANCEL");
                return;
            }
            startActivity(intent);
            finish();
        });

        mSocket = SingletonSocket.getInstance("", null);

        if(!mSocket.handleWasCreated())
        {
            bluetoothIn = Handler_Msg_Hilo_Principal();
            mSocket.setHandle(bluetoothIn);
            mSocket.handleCreated();
        }
        else
        {
            bluetoothIn = mSocket.getHandler();
        }

        mConnectedThread = new ConnectedThread(mSocket.getBtSocket());
        mConnectedThread.start();

        if(trainingByTime)
        {
            int timeInSeconds = duration * 60;
            mConnectedThread.write(timeInSeconds + " 0 " + (enableDynamicMusic ? 1:0) + " " + (enableBuzzer?1:0) + " " + (intensity.equals("Baja")?1:(intensity.equals("Media")?2:3)));
        }
        else
        {
            int durationInMeters = duration;
            mConnectedThread.write("0 " + durationInMeters + " " + (enableDynamicMusic ? 1:0) + " " + (enableBuzzer?1:0) + " " + (intensity.equals("Baja")?1:(intensity.equals("Media")?2:3)));
        }

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        // Bind to LocalService.
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume()
    {
        super.onResume();
        mConnectedThread = new ConnectedThread(mSocket.getBtSocket());
        mConnectedThread.start();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onRestart()
    {
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
        super.onRestart();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        unbindService(connection);
        firstSong = true;
        mMediaPlayerServiceIsBound = false;
    }

    @Override
    protected void onDestroy()
    {
        mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY));
        firstSong = true;
        super.onDestroy();
    }

    private void showToast(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private final ServiceConnection connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            mService = binder.getService();
            mMediaPlayerServiceIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mMediaPlayerServiceIsBound = false;
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent event)
    {
        synchronized (this)
        {
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                if (firstReading) {
                    lastProximitySensor = event.values[0];
                    firstReading = false;
                }

                if (lastProximitySensor != event.values[0]) {
                    if (event.values[0] <= 0) {
                        if (trainingPaused) {
                            mConnectedThread.write("RESUME");
                            tvStatus.setText("Entrenamiento en Curso");
                            trainingPaused = false;
                            playPauseMusic();
                        } else {
                            mConnectedThread.write("PAUSE");
                            tvStatus.setText("Entrenamiento Pausado");
                            trainingPaused = true;
                            playPauseMusic();
                        }
                    }
                }
                lastProximitySensor = event.values[0];
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            {
                super.onKeyDown(keyCode, event);
                int volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                showToast("Volumen: " + volumeLevel);
                return super.onKeyDown(keyCode, event);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void playMusic(String music)
    {
        if(mMediaPlayerServiceIsBound)
            mService.setDynamicMusic(music);
    }

    public void playPauseMusic()
    {
        if(mMediaPlayerServiceIsBound)
            mService.toggleMusic();
    }

    private Handler Handler_Msg_Hilo_Principal ()
    {
        return  new Handler(Looper.getMainLooper())
        {
            @SuppressLint("SetTextI18n")
            public void handleMessage(@NonNull Message msg)
            {
                if (msg.what == handlerState)
                {
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.indexOf("\r\n");
                    if (endOfLineIndex > 0)
                    {
                        String commandName = recDataString.substring(0, endOfLineIndex).replaceAll("(\\r)", "");
                        String summary = "";
                        int volume = 0;
                        if(commandName.startsWith("ENDED"))
                        {
                            int commandNameIndex = commandName.indexOf("|");
                            summary = commandName.substring(commandNameIndex+1);
                            commandName = commandName.substring(0,commandNameIndex);
                        }
                        if(commandName.startsWith("VOL"))
                        {
                            int commandNameIndex = commandName.indexOf(" ");
                            volume = Integer.parseInt(commandName.substring(commandNameIndex + 1));
                            commandName = commandName.substring(0, commandNameIndex);
                        }
                        switch(commandName)
                        {
                            case "WAITTING":
                                tvStatus.setText("Entrenamiento listo para comenzar");
                                break;
                            case "PAUSED":
                                tvStatus.setText("Entrenamiento pausado");
                                trainingPaused = true;
                                break;
                            case "ENDED":

                                tvStatus.setText(summary);
                                btnRestart.setText("Reiniciar");
                                mService.stopMusic();

                                TRAINING_FINISHED = true;
                                break;
                            case "STARTED":
                                btnRestart.setEnabled(true);
                            case "RESUMED":
                                tvStatus.setText("Entrenamiento en Curso");
                                break;
                            case "Sad Music":
                                playMusic("Sad");
                                break;
                            case "Neutral Music":
                                playMusic("Neutral");
                                break;
                            case "Motivational Music":
                                playMusic("Motivational");
                                break;
                            case "NEXT":
                                if(!enableDynamicMusic)
                                    mService.nextSong();
                                break;
                            case "PLAY/STOP":
                                if (firstSong && !enableDynamicMusic)
                                {
                                    mService.startUserMusic();
                                    firstSong = false;
                                }
                                else
                                    playPauseMusic();
                                break;
                            case "VOL":
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0 );
                                showToast("Volumen: " + volume);
                            default:
                                break;
                        }
                        recDataString.delete(0, recDataString.length());
                    }
                }
            }
        };
    }

    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream temporalInput = null;
            OutputStream temporalOutput = null;

            try
            {
                temporalInput = socket.getInputStream();
                temporalOutput = socket.getOutputStream();
            } catch (IOException e)
            {
                Log.e("ConnectedThread", "Error getting input/output streams", e);
            }

            mmInStream = temporalInput;
            mmOutStream = temporalOutput;
        }

        public void run()
        {
            byte[] buffer = new byte[256];
            int bytes;

            while (true)
            {
                try
                {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                }  catch (SocketTimeoutException e) {
                    // Handle timeout, maybe retry
                    Log.w("BluetoothThread", "Read timeout", e);
                } catch (IOException e) {
                    Log.e("BluetoothThread", "Error reading data", e);
                    break;
                }
            }
        }


        //write method
        public void write(String input)
        {
            try
            {
                byte[] msgBuffer = input.getBytes();
                mmOutStream.write(msgBuffer);
            } catch (IOException e)
            {
                Log.e("BluetoothCommunication", "Error writing data", e);
                showToast("La conexion fallo");
                finish();
            }
        }
    }
}