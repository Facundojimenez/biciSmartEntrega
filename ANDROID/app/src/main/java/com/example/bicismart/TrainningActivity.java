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
import android.view.KeyEvent;
import android.view.View;
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

public class TrainningActivity extends AppCompatActivity implements SensorEventListener
{
    TextView tvDuracion, tvIntensidad, tvBuzzer,  tvMusDin, tvAddress, tvTipoEntrenamiento, tvSummaryTiempo, tvSummaryMetrosRecorridos, tvSummaryVelocidadMedia, tvSummaryTitulo;
    static TextView tvEstado;
    //Parametros
    int duracion;
    String intensidad;
    boolean enableBuzzer, forTime;
    static boolean enableMusDin;
    //Musica
    static MusicService mService;
    static boolean mBound = false;
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
        tvDuracion = findViewById(R.id.tv_duracion);
        tvIntensidad = findViewById(R.id.tv_intensidad);
        tvBuzzer = findViewById(R.id.tv_buzzer);
        //tvSensores = findViewById(R.id.tv_Sensores);
        tvMusDin = findViewById(R.id.tv_MusicaDin);
        tvAddress = findViewById(R.id.tvAddress);
        tvTipoEntrenamiento = findViewById(R.id.tv_tipoEntrenamiento);
        tvEstado = findViewById(R.id.tv_estado);

        tvSummaryTiempo = findViewById(R.id.tvSummaryTiempo);
        tvSummaryMetrosRecorridos = findViewById(R.id.tvSummaryMetrosRecorridos);
        tvSummaryVelocidadMedia = findViewById(R.id.tvSummaryVelocidadMedia);
        tvSummaryTitulo = findViewById(R.id.tvSummaryTitulo);

        btnRestart = findViewById(R.id.btn_restart);
        btnRestart.setText("Cancelar Entrenamiento");

        Bundle bundle =getIntent().getExtras();
        address = bundle.getString("Direccion_Bluethoot");
        duracion = bundle.getInt("Duracion");
        intensidad = bundle.getString("Intensidad");
        enableBuzzer = bundle.getBoolean("Buzzer");

        enableMusDin = bundle.getBoolean("Musica Dinamica");
        forTime = bundle.getBoolean("Por Tiempo");

        tvTipoEntrenamiento.setText("Por Tiempo: " + forTime);
        tvDuracion.setText("Duracion: " + duracion);
        tvIntensidad.setText("Intensidad: " + intensidad);
        tvBuzzer.setText("Buzzer: " + enableBuzzer);

        tvMusDin.setText("Musica Dinamica: " + enableMusDin);
        tvAddress.setText("Address: " + address);

        btnRestart.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
              Intent intent = new Intent(TrainningActivity.this, PreTrainingActivity.class);
              intent.putExtra("Direccion_Bluethoot", address);
              if (!TRAINING_FINISHED){
                  mConnectedThread.write("CANCEL");
                  return;
              }
              startActivity(intent);
              finish();
          }
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

        if(forTime)
        {
            mConnectedThread.write(duracion + " 0 " + (enableMusDin? 1:0) + " " + (enableBuzzer?1:0) + " " + (intensidad.equals("Baja")?1:(intensidad.equals("Media")?2:3)));
        }
        else
        {
            mConnectedThread.write("0 " + duracion + " " + (enableMusDin? 1:0) + " " + (enableBuzzer?1:0) + " " + (intensidad.equals("Baja")?1:(intensidad.equals("Media")?2:3)));
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
        mBound = false;
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
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent event)
    {
        synchronized (this)
        {
            switch (event.sensor.getType())
            {
                case Sensor.TYPE_PROXIMITY :
                    if(firstReading)
                    {
                        lastProximitySensor = event.values[0];
                        firstReading = false;
                    }

                    if(lastProximitySensor != event.values[0])
                    {
                        if (event.values[0] <= 0)
                        {
                            if (trainingPaused)
                            {
                                mConnectedThread.write("RESUME");
                                tvEstado.setText("Entrenamiento en Curso");
                                trainingPaused = false;
                                playPauseMusic();
                            } else
                            {
                                mConnectedThread.write("PAUSE");
                                tvEstado.setText("Entrenamiento Pausado");
                                trainingPaused = true;
                                playPauseMusic();
                            }
                        }
                    }
                    lastProximitySensor = event.values[0];
                    break;
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
        if(mBound)
            mService.setMusic(music);
    }

    public void playPauseMusic()
    {
        if(mBound)
            mService.playPauseMusic();
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
                        String[] commandArguments = null;
                        String resumen = "";
                        int volume = 0;
                        if(commandName.startsWith("ENDED"))
                        {
                            int commandNameIndex = commandName.indexOf("|");
//                            commandArguments = commandName.substring(commandNameIndex + 1, commandName.length()).split("\\|");
//                            commandName = commandName.substring(0, commandNameIndex);
                            resumen = commandName.substring(commandNameIndex+1);
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
                                tvEstado.setText("Entrenamiento listo para comenzar");
                                break;
                            case "PAUSED":
                                tvEstado.setText("Entrenamiento pausado");
                                trainingPaused = true;
                                break;
                            case "ENDED":
                                //tvEstado.setText("Entrenamiento Finalizado\n" + resumen);
                                tvEstado.setText(resumen);
                                btnRestart.setText("Reiniciar");
                                mService.stopMusic();

                                TRAINING_FINISHED = true;
                                break;
                            case "STARTED":
                            case "RESUMED":
                                tvEstado.setText("Entrenamiento en Curso");
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
                                if(!enableMusDin)
                                    mService.nextSong();
                                break;
                            case "PLAY/STOP":
                                if (firstSong && !enableMusDin)
                                {
                                    //mService.listRaw();
                                    mService.startMusic();
                                    firstSong = false;
                                }
                                else
                                    playPauseMusic();
                                break;
                            case "VOL":
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0 );
                                showToast("Volumen: " + volume);
                            default:
                                showToast("Comando Erroneo");
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
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e)
            {

            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
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
                } catch (IOException e)
                {
                    break;
                }
            }
        }


        //write method
        public void write(String input)
        {
            byte[] msgBuffer = input.getBytes();
            try
            {
                mmOutStream.write(msgBuffer);
            } catch (IOException e)
            {
                showToast("La conexion fallo");
                finish();
            }
        }
    }
}