package com.example.bicismart;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

public class MediaSetupFragment extends Fragment
{
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch swMusic, swBuzzer;
    Button btnSave;

    public MediaSetupFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.tr_fragment_media_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        swMusic = view.findViewById(R.id.switch_music);
        //swSensor = view.findViewById(R.id.switch_sensor);
        swBuzzer = view.findViewById(R.id.switch_buzzer);
        btnSave = view.findViewById(R.id.btn_save);


        swMusic.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(swMusic.isChecked())
                    showToast("Musica Dinamica Activada");
            }
        });

//        swSensor.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v)
//            {
//                if(swSensor.isChecked())
//                    showToast("Control por Sensores Activado");
//            }
//        });

        swBuzzer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(swBuzzer.isChecked())
                    showToast("Buzzer Activado");
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showToast("Guardando Configuracion");
                Bundle bundle = new Bundle();
                bundle.putBoolean("Musica_Dinamica", swMusic.isChecked());
                bundle.putBoolean("Buzzer", swBuzzer.isChecked());
              //  bundle.putBoolean("Control_Sensors", swSensor.isChecked());
                getParentFragmentManager().setFragmentResult("datos", bundle);
            }
        });
    }

    private void showToast(String message)
    {
        Toast.makeText(requireActivity().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}