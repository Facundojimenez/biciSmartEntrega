package com.example.bicismart;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

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

    RadioButton btnTime, btnMeters;
    private TextView tvTrainningParameter;
    private EditText etTrainningParameter;
    private Spinner spItensity;
    Button btnStart;

    private final String[] intensidades = new String[]
            {
                    "Baja",
                    "Media",
                    "Alta",
            };

    private static String address = null;

    private boolean enableBuzzer = true;
    //private boolean enableSensor = true;
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

        getParentFragmentManager().setFragmentResultListener("datos", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                enableBuzzer = result.getBoolean("Buzzer");

                enableDinMusic = result.getBoolean("Musica_Dinamica");
            }
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
        tvTrainningParameter = view.findViewById(R.id.tvEntrenamiento);
        etTrainningParameter = view.findViewById(R.id.et_parametro_Entrenamiento);
        btnStart = view.findViewById(R.id.btn_start);
        spItensity = view.findViewById(R.id.spinner_Intensidad);

        ArrayAdapter<String> adaptador = new ArrayAdapter<>(requireActivity().getApplicationContext(), android.R.layout.simple_spinner_item, intensidades);
        spItensity.setAdapter(adaptador);
        // esto es para cambiar el color del 1er item seleccionado
        spItensity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                TextView ch = ((TextView) spItensity.getChildAt(0));
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
                tvTrainningParameter.setText("Ingresar Tiempo (en Minutos)");
            }
        });

        btnMeters.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                tvTrainningParameter.setText("Ingresar Metros");
            }
        });

        btnStart.setOnClickListener(v -> {
            String str = etTrainningParameter.getText().toString();
            if (str.isEmpty())
                showToast("Ingresar Parametros");
            else {
                int valor = Integer.parseInt(str);
                if (valor > 1 && valor < 100000) {
                    Intent i = new Intent(getActivity(), TrainningActivity.class);
                    i.putExtra("Direccion_Bluethoot", address);
                    i.putExtra("Duracion", valor);
                    i.putExtra("Por Tiempo", btnTime.isChecked());
                    i.putExtra("Intensidad", spItensity.getSelectedItem().toString());
                    i.putExtra("Buzzer", enableBuzzer);
                    i.putExtra("Musica Dinamica", enableDinMusic);
                    startActivity(i);
                } else if (valor > 100000) {
                    String youtubeVideoLink = "https://www.youtube.com/watch?v=BGljR9vf3Os";

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeVideoLink));
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    try {
                        startActivity(browserIntent);
                    }catch (Exception e){
                        showToast("Esto no era un eater egg...");
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