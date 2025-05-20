package edu.fje.testchecker.ui.notas;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import edu.fje.testchecker.databinding.FragmentNotasBinding;
import edu.fje.testchecker.ui.shared.SharedViewModel;

public class NotasFragment extends Fragment {

    private FragmentNotasBinding binding;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotasBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        SharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Mostra imatge amb quadrícula
        viewModel.getImagenResultado().observe(getViewLifecycleOwner(), bitmap -> {
            binding.imagenNotas.setImageBitmap(bitmap);
        });

        // Mostra respostes i resultats
        viewModel.getQrData().observe(getViewLifecycleOwner(), qrData -> {
            viewModel.getRespuestas().observe(getViewLifecycleOwner(), respuestasDetectadas -> {
                if (qrData == null || qrData.isEmpty() || respuestasDetectadas == null) {
                    binding.textNotas.setText("❗ Datos incompletos.");
                    return;
                }

                try {
                    JSONObject qrObject = new JSONObject(qrData);
                    JSONObject respuestasCorrectasObj = qrObject.getJSONObject("respuestas_correctas");

                    Map<Integer, Integer> respuestasCorrectas = new HashMap<>();
                    Iterator<String> keys = respuestasCorrectasObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        int pregunta = Integer.parseInt(key);
                        int respuesta = respuestasCorrectasObj.getInt(key);
                        respuestasCorrectas.put(pregunta, respuesta);
                    }

                    SpannableStringBuilder resultadoTexto = new SpannableStringBuilder();
                    resultadoTexto.append("📋 RESULTADOS DEL TEST\n\n");

                    int correctas = 0;
                    int incorrectas = 0;
                    int invalidas = 0;
                    int respondidas = 0;

                    for (Map.Entry<Integer, Integer> entry : respuestasCorrectas.entrySet()) {
                        int pregunta = entry.getKey();
                        int correcta = entry.getValue();

                        Integer marcada = respuestasDetectadas.get(pregunta);
                        resultadoTexto.append("Pregunta ").append(String.valueOf(pregunta)).append(": ");

                        if (marcada == null) {
                            resultadoTexto.append("➖ No respondida\n");
                        } else if (marcada == -1) {
                            resultadoTexto.append("⚠️ Respuesta inválida\n");
                            invalidas++;
                        } else if (marcada.equals(correcta)) {
                            resultadoTexto.append("✅ Correcta (Opción ")
                                    .append((char) ('A' + marcada)).append(")\n");
                            correctas++;
                            respondidas++;
                        } else {
                            resultadoTexto.append("❌ Incorrecta (Marcada ")
                                    .append((char) ('A' + marcada))
                                    .append(", Correcta ")
                                    .append((char) ('A' + correcta))
                                    .append(")\n");
                            incorrectas++;
                            respondidas++;
                        }
                    }

                    double puntuacionTotal = correctas * 1.0 + incorrectas * (-0.25);

                    resultadoTexto.append("\n📊 RESUMEN FINAL\n\n")
                            .append("📌 Preguntas respondidas: ").append(String.valueOf(respondidas)).append("\n")
                            .append("✅ Correctas: ").append(String.valueOf(correctas)).append(" (+" + correctas + ")\n")
                            .append("❌ Incorrectas: ").append(String.valueOf(incorrectas)).append(" (-" + (incorrectas * 0.25) + ")\n")
                            .append("⚠️ Inválidas: ").append(String.valueOf(invalidas)).append(" (+0)\n")
                            .append("🎯 Puntuación total: ").append(String.format("%.2f", puntuacionTotal))
                            .append(" / ").append(String.valueOf(respuestasCorrectas.size())).append("\n");

                    binding.textNotas.setText(resultadoTexto);

                    Log.i("NotasFragment", resultadoTexto.toString());

                } catch (JSONException e) {
                    binding.textNotas.setText("❗ Error al procesar el QR.");
                    Log.e("NotasFragment", "Error parsing QR JSON", e);
                }
            });
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
