package com.reconocimiento3d.vision;

import ai.onnxruntime.*;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.FloatBuffer;
import java.util.Collections;

public class DetectorObjetos {

        // Clases del modelo COCO
        static String[] clases = {
                        "person", "bicycle", "car", "motorcycle", "airplane",
                        "bus", "train", "truck", "boat", "traffic light",
                        "fire hydrant", "stop sign", "parking meter", "bench",
                        "bird", "cat", "dog", "horse", "sheep", "cow",
                        "elephant", "bear", "zebra", "giraffe", "backpack",
                        "umbrella", "handbag", "tie", "suitcase", "frisbee",
                        "skis", "snowboard", "sports ball", "kite", "baseball bat",
                        "baseball glove", "skateboard", "surfboard", "tennis racket",
                        "bottle", "wine glass", "cup", "fork", "knife", "spoon",
                        "bowl", "banana", "apple", "sandwich", "orange", "broccoli",
                        "carrot", "hot dog", "pizza", "donut", "cake", "chair",
                        "couch", "potted plant", "bed", "dining table", "toilet",
                        "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
                        "microwave", "oven", "toaster", "sink", "refrigerator",
                        "book", "clock", "vase", "scissors", "teddy bear",
                        "hair drier", "toothbrush"
        };

        public static void main(String[] args) {

                try {

                        // ==============================
                        // 1. Cargar OpenCV
                        // ==============================

                        OpenCV.loadLocally();

                        System.out.println("OpenCV cargado correctamente");

                        // ==============================
                        // 2. Cargar ONNX Runtime
                        // ==============================

                        OrtEnvironment entorno = OrtEnvironment.getEnvironment();

                        OrtSession sesion = entorno.createSession(
                                        "models/yolo26n.onnx",
                                        new OrtSession.SessionOptions());

                        System.out.println("YOLO cargado correctamente");

                        // ==============================
                        // 3. Cargar imagen
                        // ==============================

                        Mat imagen = Imgcodecs.imread("bus.jpg");

                        if (imagen.empty()) {

                                System.out.println(
                                                "No se pudo cargar bus.jpg");

                                return;
                        }

                        System.out.println(
                                        "Imagen cargada correctamente");

                        // ==============================
                        // 4. Redimensionar
                        // ==============================

                        Mat redimensionada = new Mat();

                        Imgproc.resize(
                                        imagen,
                                        redimensionada,
                                        new Size(640, 640));

                        // ==============================
                        // 5. BGR -> RGB
                        // ==============================

                        Imgproc.cvtColor(
                                        redimensionada,
                                        redimensionada,
                                        Imgproc.COLOR_BGR2RGB);

                        System.out.println(
                                        "Imagen convertida a RGB");

                        // ==============================
                        // 6. Crear datos para YOLO
                        // ==============================

                        float[] datos = new float[1 * 3 * 640 * 640];

                        int posicion = 0;

                        for (int canal = 0; canal < 3; canal++) {

                                for (int y = 0; y < 640; y++) {

                                        for (int x = 0; x < 640; x++) {

                                                double[] pixel = redimensionada.get(y, x);

                                                datos[posicion++] = (float) (pixel[canal] / 255.0);
                                        }
                                }
                        }

                        System.out.println(
                                        "Imagen convertida al formato de YOLO");

                        System.out.println(
                                        "Cantidad de valores: " +
                                                        datos.length);

                        // ==============================
                        // 7. Crear tensor
                        // ==============================

                        OnnxTensor tensor = OnnxTensor.createTensor(
                                        entorno,
                                        FloatBuffer.wrap(datos),
                                        new long[] { 1, 3, 640, 640 });

                        System.out.println(
                                        "Tensor creado correctamente");

                        // ==============================
                        // 8. Ejecutar YOLO
                        // ==============================

                        OrtSession.Result resultado = sesion.run(
                                        Collections.singletonMap(
                                                        "images",
                                                        tensor));

                        System.out.println(
                                        "YOLO ejecutado correctamente");

                        // ==============================
                        // 9. Obtener salida
                        // ==============================

                        float[][][] salida = (float[][][]) resultado
                                        .get(0)
                                        .getValue();

                        System.out.println(
                                        "Dimensiones: " +
                                                        salida.length + " x " +
                                                        salida[0].length + " x " +
                                                        salida[0][0].length);

                        // ==============================
                        // 10. Buscar detecciones y cajas
                        // ==============================

                        float[][] datosYOLO = salida[0];

                        int cantidadDetecciones = datosYOLO[0].length;

                        float umbralConfianza = 0.50f;
                        float umbralNMS = 0.45f;

                        java.util.List<org.opencv.core.Rect> cajas = new java.util.ArrayList<>();

                        java.util.List<Float> confianzas = new java.util.ArrayList<>();

                        java.util.List<Integer> clasesDetectadas = new java.util.ArrayList<>();

                        int anchoImagen = imagen.cols();
                        int altoImagen = imagen.rows();

                        float escalaX = anchoImagen / 640.0f;
                        float escalaY = altoImagen / 640.0f;

                        for (int i = 0; i < cantidadDetecciones; i++) {

                                float centroX = datosYOLO[0][i];
                                float centroY = datosYOLO[1][i];
                                float ancho = datosYOLO[2][i];
                                float alto = datosYOLO[3][i];

                                float mejorConfianza = 0;
                                int mejorClase = -1;

                                for (int clase = 0; clase < 80; clase++) {

                                        float confianza = datosYOLO[4 + clase][i];

                                        if (confianza > mejorConfianza) {
                                                mejorConfianza = confianza;
                                                mejorClase = clase;
                                        }
                                }

                                if (mejorClase == -1 ||
                                                mejorConfianza < umbralConfianza) {
                                        continue;
                                }

                                int x = Math.round((centroX - ancho / 2) * escalaX);
                                int y = Math.round((centroY - alto / 2) * escalaY);
                                int w = Math.round(ancho * escalaX);
                                int h = Math.round(alto * escalaY);

                                x = Math.max(0, Math.min(x, anchoImagen - 1));
                                y = Math.max(0, Math.min(y, altoImagen - 1));
                                w = Math.max(1, Math.min(w, anchoImagen - x));
                                h = Math.max(1, Math.min(h, altoImagen - y));

                                cajas.add(new org.opencv.core.Rect(x, y, w, h));
                                confianzas.add(mejorConfianza);
                                clasesDetectadas.add(mejorClase);
                        }

                        // ==============================
                        // 11. Aplicar NMS por clase
                        // ==============================

                        java.util.List<Integer> seleccionadas = new java.util.ArrayList<>();

                        boolean[] eliminadas = new boolean[cajas.size()];

                        for (int i = 0; i < cajas.size(); i++) {

                                if (eliminadas[i]) {
                                        continue;
                                }

                                seleccionadas.add(i);

                                org.opencv.core.Rect a = cajas.get(i);

                                for (int j = i + 1; j < cajas.size(); j++) {

                                        if (eliminadas[j]) {
                                                continue;
                                        }

                                        // No comparar objetos de clases diferentes
                                        if (!clasesDetectadas.get(i).equals(
                                                        clasesDetectadas.get(j))) {
                                                continue;
                                        }

                                        org.opencv.core.Rect b = cajas.get(j);

                                        int izquierda = Math.max(a.x, b.x);
                                        int arriba = Math.max(a.y, b.y);
                                        int derecha = Math.min(a.x + a.width, b.x + b.width);
                                        int abajo = Math.min(a.y + a.height, b.y + b.height);

                                        int interseccion = Math.max(0, derecha - izquierda)
                                                        * Math.max(0, abajo - arriba);

                                        int areaA = a.width * a.height;
                                        int areaB = b.width * b.height;

                                        int union = areaA + areaB - interseccion;

                                        float iou = union > 0
                                                        ? (float) interseccion / union
                                                        : 0;

                                        if (iou > umbralNMS) {
                                                eliminadas[j] = true;
                                        }
                                }
                        }

                        // ==============================
                        // 12. Mostrar detecciones finales
                        // ==============================

                        System.out.println();
                        System.out.println("========== DETECCIONES ==========");

                        for (int i = 0; i < seleccionadas.size(); i++) {

                                int indice = seleccionadas.get(i);
                                org.opencv.core.Rect caja = cajas.get(indice);

                                System.out.println(
                                                (i + 1) + ". " +
                                                                clases[clasesDetectadas.get(indice)] +
                                                                " -> " +
                                                                String.format(
                                                                                java.util.Locale.US,
                                                                                "%.2f%%",
                                                                                confianzas.get(indice) * 100)
                                                                +
                                                                " | X: " + caja.x +
                                                                " | Y: " + caja.y +
                                                                " | Ancho: " + caja.width +
                                                                " | Alto: " + caja.height);
                        }

                        if (seleccionadas.isEmpty()) {
                                System.out.println("No se encontraron objetos válidos.");
                        }

                        System.out.println("================================");

                        // ==============================
                        // 12. Liberar recursos
                        // ==============================

                        resultado.close();
                        tensor.close();

                        imagen.release();
                        redimensionada.release();

                        sesion.close();
                        entorno.close();

                        System.out.println(
                                        "Proceso finalizado correctamente");

                } catch (Exception e) {

                        System.out.println(
                                        "Error durante el procesamiento");

                        e.printStackTrace();
                }
        }
}