package com.example.bicismart;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class TrainingSetupFragment extends Fragment {

    public static final int MAXIMUM_TIME_METERS_VALUE = 100000;
    RadioButton btnTime, btnMeters;
    private TextView tvTrainingParameter;
    private EditText etTrainingParameter;
    private Spinner spIntensity;
    Button btnStart;

    private final String[] intensities = new String[]
    {
        "Baja",
        "Media",
        "Alta",
    };

    private static String address = null;

    private boolean enableBuzzer = true;
    private boolean enableDinMusic = true;

    public TrainingSetupFragment() {
        // Required empty public constructor
    }

    public static TrainingSetupFragment newInstance(String address) {
        TrainingSetupFragment fragment = new TrainingSetupFragment();
        Bundle bundle = new Bundle();
        bundle.putString("Direccion_Bluetooth", address);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            address = getArguments().getString("Direccion_Bluetooth");
        }

        getParentFragmentManager().setFragmentResultListener("datos", this, (requestKey, result) -> {
            enableBuzzer = result.getBoolean("Buzzer");
            enableDinMusic = result.getBoolean("Musica_Dinamica");
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.tr_fragment_training_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnTime = view.findViewById(R.id.btn_tiempo);
        btnMeters = view.findViewById(R.id.btn_metros);
        tvTrainingParameter = view.findViewById(R.id.tvEntrenamiento);
        etTrainingParameter = view.findViewById(R.id.et_parametro_Entrenamiento);
        btnStart = view.findViewById(R.id.btn_start);
        spIntensity = view.findViewById(R.id.spinner_Intensidad);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity().getApplicationContext(), android.R.layout.simple_spinner_item, intensities);
        spIntensity.setAdapter(adapter);
        spIntensity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                TextView ch = ((TextView) spIntensity.getChildAt(0));
                if (ch != null) {
                    ch.setTextColor(Color.parseColor("#2E7D32"));
                }
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        Bundle args = getArguments();
        address = args.getString("Direccion_Bluetooth", address);
        showToast("Address: " + address);

        btnTime.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                tvTrainingParameter.setText("Ingresar Tiempo (en Minutos)");
            }
        });

        btnMeters.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                tvTrainingParameter.setText("Ingresar Metros");
            }
        });

        btnStart.setOnClickListener(v -> {
            String str = etTrainingParameter.getText().toString();
            if (str.isEmpty())
                showToast("Ingresar Parametros");
            else {
                int valor = Integer.parseInt(str);
                if (valor >= 1 && valor < MAXIMUM_TIME_METERS_VALUE) {
                    Intent i = new Intent(getActivity(), TrainingActivity.class);
                    i.putExtra("Direccion_Bluetooth", address);
                    i.putExtra("Duracion", valor);
                    i.putExtra("Por Tiempo", btnTime.isChecked());
                    i.putExtra("Intensidad", spIntensity.getSelectedItem().toString());
                    i.putExtra("Buzzer", enableBuzzer);
                    i.putExtra("Musica Dinamica", enableDinMusic);
                    startActivity(i);
                } else if (valor > MAXIMUM_TIME_METERS_VALUE) {
                    String youtubeVideoLink = "https://www.youtube.com/watch?v=BGljR9vf3Os";

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeVideoLink));
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    try {
                        startActivity(browserIntent);
                    }catch (Exception e){
                        showToast("Esto no era un easter egg...");
                    }

                } else if (btnTime.isChecked()) {
                    showToast("Tiempo de entrenamiento invalido");
                } else if (btnMeters.isChecked()) {
                    showToast("Metros de entrenamiento invalido");
                }
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(requireActivity().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}