package edu.fje.testchecker.ui.shared;

import android.graphics.Bitmap;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Map;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<String> qrData = new MutableLiveData<>();
    private final MutableLiveData<Bitmap> imagenResultado = new MutableLiveData<>();
    private final MutableLiveData<Map<Integer, Integer>> respuestas = new MutableLiveData<>();

    public void setQrData(String data) {
        qrData.setValue(data);
    }

    public LiveData<String> getQrData() {
        return qrData;
    }

    public void setImagenResultado(Bitmap bitmap) {
        imagenResultado.setValue(bitmap);
    }

    public LiveData<Bitmap> getImagenResultado() {
        return imagenResultado;
    }

    public void setRespuestas(Map<Integer, Integer> respuestasMap) {
        respuestas.setValue(respuestasMap);
    }

    public LiveData<Map<Integer, Integer>> getRespuestas() {
        return respuestas;
    }
}
