package edu.fje.testchecker.ui.scan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.objdetect.QRCodeDetector;
import org.opencv.android.Utils;
import org.opencv.utils.Converters;

import java.util.*;

import edu.fje.testchecker.R;
import edu.fje.testchecker.ui.shared.SharedViewModel;

public class ScanFragment extends Fragment {
    private AutoFitTextureView textureView;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size previewSize;
    private long ultimoProcesamiento = 0;

    static {
        System.loadLibrary("opencv_java4");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "No s'ha pogut inicialitzar OpenCV");
        } else {
            Log.i("OpenCV", "OpenCV inicialitzat correctament");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Infla el layout del fragment i assigna el TextureView per a la vista de càmera
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        textureView = view.findViewById(R.id.camera_view);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        return view;
    }

    // Listener que controla quan el TextureView està llest per mostrar la vista de la càmera
    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            obrirCamera();
        }

        @Override public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}
        @Override public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) { return true; }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            // Control de freqüència: només processa el fotograma actual cada 1 segon
            long actual = System.currentTimeMillis();
            if (actual - ultimoProcesamiento >= 1000) {
                ultimoProcesamiento = actual;
                processarFrame();
            }
        }
    };

    // Obre la càmera i configura la mida de la vista prèvia
    @SuppressLint("MissingPermission")
    private void obrirCamera() {
        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            previewSize = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(SurfaceTexture.class)[0];

            textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            Log.e("ScanFragment", "Error obrint càmera: " + e.getMessage());
        }
    }

    // Callback que gestiona l'estat de la càmera (oberta, desconnectada o amb error)
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            crearPreview();
        }

        @Override public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    // Configura la sessió de la càmera i inicia la transmissió de vídeo
    private void crearPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(texture);

            // Configura la càmera per mostrar vista prèvia
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            // Crea una sessió de càmera
            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        // Inicia la captura contínua
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        Log.e("ScanFragment", "Error configurant la vista prèvia: " + e.getMessage());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e("ScanFragment", "Fallada en configurar la càmera");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e("ScanFragment", "Error creant vista prèvia: " + e.getMessage());
        }
    }

    // Ordena els punts dels marcadors
    private Point[] ordenarPunts(List<Point> punts) {
        Point[] ordenat = new Point[4];
        double[] suma = new double[4];
        double[] resta = new double[4];
        for (int i = 0; i < 4; i++) {
            suma[i] = punts.get(i).x + punts.get(i).y;
            resta[i] = punts.get(i).x - punts.get(i).y;
        }
        ordenat[0] = punts.get(indexMin(suma)); // top-left
        ordenat[2] = punts.get(indexMax(suma)); // bottom-right
        ordenat[1] = punts.get(indexMin(resta)); // top-right
        ordenat[3] = punts.get(indexMax(resta)); // bottom-left
        return ordenat;
    }

    private int indexMin(double[] arr) {
        int idx = 0;
        for (int i = 1; i < arr.length; i++) if (arr[i] < arr[idx]) idx = i;
        return idx;
    }

    private int indexMax(double[] arr) {
        int idx = 0;
        for (int i = 1; i < arr.length; i++) if (arr[i] > arr[idx]) idx = i;
        return idx;
    }

    // Detecta les caselles marcades en l’examen
    private Map<Integer, Integer> detectarRespostes(Mat imgWarp) {
        int numPreguntes = 10;
        int numOpcions = 4;

        // Definició de la quadrícula
        int startX = (int) (0.52 * imgWarp.cols());
        int startY = (int) (0.14 * imgWarp.rows());
        int totalWidth = (int) (0.4 * imgWarp.cols());
        int totalHeight = (int) (0.675 * imgWarp.rows());
        int cellWidth = totalWidth / numOpcions;
        int cellHeight = totalHeight / numPreguntes;
        int umbralMinim = 3000;

        Map<Integer, Integer> respostes = new HashMap<>();

        // Dibuixa la quadrícula a sobre la imatge warp
        for (int i = 0; i <= numPreguntes; i++) {
            int y = startY + i * cellHeight;
            Imgproc.line(imgWarp, new Point(startX, y), new Point(startX + totalWidth, y), new Scalar(0, 255, 0), 2);
        }
        for (int j = 0; j <= numOpcions; j++) {
            int x = startX + j * cellWidth;
            Imgproc.line(imgWarp, new Point(x, startY), new Point(x, startY + totalHeight), new Scalar(0, 255, 0), 2);
        }

        // Converteix la imatge final a Bitmap i l’envia al ViewModel
        Bitmap resultat = Bitmap.createBitmap(imgWarp.cols(), imgWarp.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imgWarp, resultat);
        SharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        viewModel.setImagenResultado(resultat);

        // Bucle per detectar respostes
        for (int p = 0; p < numPreguntes; p++) {
            List<Integer> opcionsMarcades = new ArrayList<>();
            for (int o = 0; o < numOpcions; o++) {
                Rect casella = new Rect(startX + o * cellWidth, startY + p * cellHeight, cellWidth, cellHeight);

                // Processament de la casella
                Mat casellaGray = new Mat();
                Imgproc.cvtColor(imgWarp.submat(casella), casellaGray, Imgproc.COLOR_BGR2GRAY);
                Mat thresh = new Mat();
                Imgproc.threshold(casellaGray, thresh, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
                int blancs = Core.countNonZero(thresh);

                if (blancs >= umbralMinim) opcionsMarcades.add(o);
            }

            // Guarda la resposta
            if (opcionsMarcades.size() == 1) respostes.put(p + 1, opcionsMarcades.get(0));
            else if (opcionsMarcades.isEmpty()) respostes.put(p + 1, null);
            else respostes.put(p + 1, -1);
        }

        return respostes;
    }

    // Processa un fotograma: transforma, detecta QR, respostes i envia dades al ViewModel
    private void processarFrame() {
        if (textureView.getSurfaceTexture() == null) return;

        // Captura fotograma com Bitmap i el corregeix (rotació i mirall)
        Bitmap bitmap = textureView.getBitmap();
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        matrix.postScale(-1, 1);
        Bitmap correctedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // Converteix a format Mat per processament OpenCV
        Mat matColor = new Mat();
        Utils.bitmapToMat(correctedBitmap, matColor);

        // Converteix a gris i aplica binarització
        Mat matGray = new Mat();
        Imgproc.cvtColor(matColor, matGray, Imgproc.COLOR_BGR2GRAY);
        Mat thresh = new Mat();
        Imgproc.threshold(matGray, thresh, 150, 255, Imgproc.THRESH_BINARY_INV);

        // Detecta contorns a la imatge
        List<MatOfPoint> contorns = new ArrayList<>();
        Imgproc.findContours(thresh, contorns, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Filtra contorns que tenen forma quadrada (marques del test)
        List<MatOfPoint2f> marques = new ArrayList<>();
        for (MatOfPoint cnt : contorns) {
            double area = Imgproc.contourArea(cnt);
            if (area > 500 && area < 10000) {
                MatOfPoint2f cnt2f = new MatOfPoint2f(cnt.toArray());
                double perimetre = Imgproc.arcLength(cnt2f, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(cnt2f, approx, 0.02 * perimetre, true);
                if (approx.total() == 4) marques.add(approx);
            }
        }

        // Assegura que tenim només 4 marques (les cantonades del test)
        if (marques.size() < 4) {
            Log.i("processarFrame", "Només s'han detectat " + marques.size() + " marques. Calen 4.");
            return;
        } else if (marques.size() > 4) {
            marques.sort((a, b) -> Double.compare(Imgproc.contourArea(b), Imgproc.contourArea(a)));
            marques = marques.subList(0, 4);
        }

        // Calcula el centre de cada marcador
        List<Point> centres = new ArrayList<>();
        for (MatOfPoint2f m : marques) {
            Moments moment = Imgproc.moments(new MatOfPoint(m.toArray()));
            if (moment.get_m00() != 0) {
                centres.add(new Point(moment.get_m10() / moment.get_m00(), moment.get_m01() / moment.get_m00()));
            }
        }

        // Ordena els punts i aplica una transformació de perspectiva
        Point[] puntsOrdenats = ordenarPunts(centres);
        Mat src = Converters.vector_Point2f_to_Mat(Arrays.asList(puntsOrdenats));
        Mat dst = Converters.vector_Point2f_to_Mat(Arrays.asList(
                new Point(0, 0), new Point(1199, 0), new Point(1199, 1599), new Point(0, 1599)
        ));
        Mat transform = Imgproc.getPerspectiveTransform(src, dst);
        Mat imgWarp = new Mat();
        Imgproc.warpPerspective(matColor, imgWarp, transform, new org.opencv.core.Size(1200, 1600));

        // Detecta el codi QR dins la imatge corregida
        QRCodeDetector detector = new QRCodeDetector();
        String qr = detector.detectAndDecode(imgWarp);
        if (qr == null || qr.trim().isEmpty()) {
            Log.i("processarFrame", "No s'ha detectat cap codi QR.");
            return;
        }

        // Detecta les respostes marcades
        Map<Integer, Integer> respostes = detectarRespostes(imgWarp);

        // Envia les dades al ViewModel compartit
        SharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        viewModel.setQrData(qr);
        viewModel.setRespuestas(respostes);

        // Navega automàticament al fragment de resultats
        requireActivity().runOnUiThread(() -> {
            BottomNavigationView navView = requireActivity().findViewById(R.id.nav_view);
            navView.setSelectedItemId(R.id.navigation_notas);
        });
    }
}

