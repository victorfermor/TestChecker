package edu.fje.testchecker.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import edu.fje.testchecker.R;
import edu.fje.testchecker.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.textHomeTitle.setText("🎯 TestChecker");
        binding.textHomeSubtitle.setText("Corrector de exámenes tipo test");

        String instrucciones = "📌 ¿Cómo funciona?\n" +
                "1️⃣ Apunta la cámara al examen del estudiante.\n" +
                "2️⃣ Escanea el código QR del examen para cargar las respuestas correctas.\n" +
                "3️⃣ Detecta automáticamente las respuestas marcadas.\n" +
                "4️⃣ Compara con las respuestas correctas y muestra la puntuación final.\n\n" +
                "⭐ Ahorra tiempo.\n" +
                "⭐ Reduce errores de corrección.\n" +
                "⭐ Mejora el seguimiento de resultados.\n\n" +
                "¡Empieza desde el menú inferior!";

        binding.textHomeInstructions.setText(instrucciones);

        binding.imageHomeLogo.setImageResource(R.drawable.logo_app);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
