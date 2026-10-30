package com.reconocimiento3d.vision;

import java.awt.Desktop;
import java.net.URI;
import ai.onnxruntime.*;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import com.reconocimiento3d.vision.DetectorEnVivo.Deteccion;

import org.opencv.imgcodecs.Imgcodecs;
import javax.swing.*;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.FloatBuffer;
import java.util.*;
import javax.imageio.ImageIO;

public class DetectorEnVivo {

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

    static final int TAMANO = 640;

    static final float UMBRAL_CONFIANZA = 0.50f;

    static final float UMBRAL_NMS = 0.45f;

    static class Deteccion {

        Rect caja;

        float confianza;

        int clase;

        Deteccion(Rect caja, float confianza, int clase) {

            this.caja = caja;

            this.confianza = confianza;

            this.clase = clase;

        }

    }

    public static void main(String[] args) {

        // Cargar OpenCV antes de crear cualquier objeto Mat
        OpenCV.loadLocally();
        System.out.println("OpenCV cargado correctamente");

        VideoCapture camara = null;
        OrtSession sesion = null;
        OrtEnvironment entorno = null;

        Mat imagen = new Mat();
        Mat redimensionada = new Mat();
        Mat rgb = new Mat();

        try {
            // 2. Cargar modelo YOLO

            entorno = OrtEnvironment.getEnvironment();

            sesion = entorno.createSession(

                    "models/yolo26n.onnx",

                    new OrtSession.SessionOptions()

            );

            System.out.println("YOLO cargado correctamente");

            // 3. Abrir cámara

            camara = new VideoCapture(0);

            if (!camara.isOpened()) {

                System.out.println("No se pudo abrir la cámara");

                return;

            }

            System.out.println("Cámara abierta correctamente");

            // 4. Crear ventana

            JFrame ventana = new JFrame("Reconocimiento 3D - Detector en vivo");

            JLabel pantalla = new JLabel();

            ventana.add(pantalla);

            ventana.setSize(1000, 750);

            ventana.setLocationRelativeTo(null);

            ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            ventana.setVisible(true);

            // 5. Procesar cada fotograma
            boolean visorAbierto = false;
            while (ventana.isDisplayable()) {

                if (!camara.read(imagen) || imagen.empty()) {

                    continue;

                }

                int anchoOriginal = imagen.cols();

                int altoOriginal = imagen.rows();

                // Preparar imagen para YOLO

                Imgproc.resize(

                        imagen,

                        redimensionada,

                        new Size(TAMANO, TAMANO)

                );

                Imgproc.cvtColor(

                        redimensionada,

                        rgb,

                        Imgproc.COLOR_BGR2RGB

                );

                // Convertir píxeles a formato NCHW

                byte[] pixeles = new byte[

                TAMANO * TAMANO * 3

                ];

                rgb.get(0, 0, pixeles);

                float[] datos = new float[

                3 * TAMANO * TAMANO

                ];

                int cantidadPixeles = TAMANO * TAMANO;

                for (int y = 0; y < TAMANO; y++) {

                    for (int x = 0; x < TAMANO; x++) {

                        int indicePixel =

                                (y * TAMANO + x) * 3;

                        int indiceTensor =

                                y * TAMANO + x;

                        for (int canal = 0; canal < 3; canal++) {

                            int valor =

                                    pixeles[indicePixel + canal] & 0xFF;

                            datos[canal * cantidadPixeles

                                    + indiceTensor] =

                                            valor / 255.0f;

                        }

                    }

                }

                // Ejecutar YOLO

                try (

                        OnnxTensor tensor = OnnxTensor.createTensor(

                                entorno,

                                FloatBuffer.wrap(datos),

                                new long[] { 1, 3, TAMANO, TAMANO }

                        );

                        OrtSession.Result resultado = sesion.run(

                                Collections.singletonMap("images", tensor)

                        )

                ) {

                    float[][][] salida =

                            (float[][][]) resultado.get(0).getValue();

                    float[][] predicciones = salida[0];

                    List<Deteccion> detecciones =

                            new ArrayList<>();

                    float escalaX =

                            anchoOriginal / (float) TAMANO;

                    float escalaY =

                            altoOriginal / (float) TAMANO;

                    // 6. Obtener detecciones

                    for (int i = 0;

                            i < predicciones[0].length;

                            i++) {

                        float mejorConfianza = 0;

                        int mejorClase = -1;

                        for (int c = 0; c < 80; c++) {

                            float confianza =

                                    predicciones[4 + c][i];

                            if (confianza > mejorConfianza) {

                                mejorConfianza = confianza;

                                mejorClase = c;

                            }

                        }

                        if (mejorClase == -1 ||

                                mejorConfianza < UMBRAL_CONFIANZA) {

                            continue;

                        }

                        float centroX = predicciones[0][i];

                        float centroY = predicciones[1][i];

                        float ancho = predicciones[2][i];

                        float alto = predicciones[3][i];

                        int x = Math.round(

                                (centroX - ancho / 2) * escalaX

                        );

                        int y = Math.round(

                                (centroY - alto / 2) * escalaY

                        );

                        int w = Math.round(ancho * escalaX);

                        int h = Math.round(alto * escalaY);

                        x = Math.max(0, Math.min(x, anchoOriginal - 1));

                        y = Math.max(0, Math.min(y, altoOriginal - 1));

                        w = Math.max(1, Math.min(w, anchoOriginal - x));

                        h = Math.max(1, Math.min(h, altoOriginal - y));

                        detecciones.add(

                                new Deteccion(

                                        new Rect(x, y, w, h),

                                        mejorConfianza,

                                        mejorClase

                                )

                        );

                    }

                    // 7. Ordenar por confianza: primero la más alta

                    detecciones.sort(

                            (a, b) -> Float.compare(

                                    b.confianza, a.confianza

                            )

                    );

                    // 8. Eliminar cajas duplicadas (NMS)

                    List<Deteccion> finales = new ArrayList<>();

                    for (Deteccion candidata : detecciones) {

                        boolean duplicada = false;

                        for (Deteccion aceptada : finales) {

                            if (candidata.clase != aceptada.clase) {

                                continue;

                            }

                            if (calcularIoU(

                                    candidata.caja,

                                    aceptada.caja

                            ) > UMBRAL_NMS) {

                                duplicada = true;

                                break;

                            }

                        }

                        if (!duplicada) {

                            finales.add(candidata);

                        }

                    }

                    // 9. Dibujar detecciones sobre la cámara
                    // Comprobar si YOLO detectó una laptop

                    boolean laptopDetectada = false;
                    boolean telefonoDetectado = false;

                    for (Deteccion d : finales) {
                        String clase = clases[d.clase];

                        if ("laptop".equals(clase)) {
                            laptopDetectada = true;
                        }

                        if ("cell phone".equals(clase)) {
                            telefonoDetectado = true;
                        }
                    }

                    // Abrir el visor 3D solamente una vez

                    try {
                        if (laptopDetectada && !visorAbierto) {
                            if (Desktop.isDesktopSupported()
                                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {

                                Desktop.getDesktop().browse(
                                        new URI("http://localhost:8000/visor3d.html?modelo=computador"));

                                visorAbierto = true;
                                System.out.println("Laptop detectada: abriendo modelo 3D");
                            }
                        } else if (telefonoDetectado && !visorAbierto) {
                            if (Desktop.isDesktopSupported()
                                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {

                                Desktop.getDesktop().browse(
                                        new URI("http://localhost:8000/visor3d.html?modelo=telefono"));

                                visorAbierto = true;
                                System.out.println("Teléfono detectado: abriendo modelo 3D");
                            }
                        }

                        // Permitir una nueva detección cuando ya no haya laptop ni teléfono
                        if (!laptopDetectada && !telefonoDetectado) {
                            visorAbierto = false;
                        }

                    } catch (Exception e) {
                        System.out.println(
                                "No se pudo abrir el visor 3D: " + e.getMessage());
                    }

                    for (Deteccion d : finales) {
                        Rect caja = d.caja;
                        Imgproc.rectangle(
                                imagen,
                                caja,
                                new Scalar(0, 255, 0),
                                2);

                        String claseDetectada = clases[d.clase];
                        String categoria = obtenerCategoria(claseDetectada);

                        String etiqueta;

                        if (categoria != null) {
                            etiqueta = String.format(
                                    Locale.US,
                                    "%s (%.0f%%)",
                                    categoria,
                                    d.confianza * 100);
                        } else {
                            etiqueta = String.format(
                                    Locale.US,
                                    "%s (%.0f%%)",
                                    claseDetectada,
                                    d.confianza * 100);
                        }

                        int textoY = Math.max(20, caja.y - 8);
                        Imgproc.putText(
                                imagen,
                                etiqueta,
                                new Point(caja.x, textoY),
                                Imgproc.FONT_HERSHEY_SIMPLEX,
                                0.65,
                                new Scalar(0, 255, 0),
                                2

                        );

                    }

                    // Mostrar cantidad de objetos

                    Imgproc.putText(

                            imagen,

                            "Objetos: " + finales.size(),

                            new Point(15, 30),

                            Imgproc.FONT_HERSHEY_SIMPLEX,

                            0.8,

                            new Scalar(0, 255, 255),

                            2

                    );

                }

                // 10. Mostrar fotograma en la ventana

                MatOfByte buffer = new MatOfByte();

                try {

                    if (Imgcodecs.imencode(".jpg", imagen, buffer)) {

                        BufferedImage imagenJava = ImageIO.read(

                                new ByteArrayInputStream(buffer.toArray())

                        );

                        if (imagenJava != null) {

                            Image escalada = imagenJava.getScaledInstance(

                                    pantalla.getWidth(),

                                    pantalla.getHeight(),

                                    Image.SCALE_FAST

                            );

                            pantalla.setIcon(new ImageIcon(escalada));

                        }

                    }

                } catch (Exception e) {

                    System.out.println(

                            "Error al mostrar el fotograma: " + e.getMessage()

                    );

                } finally {

                    buffer.release();

                }

            }

        } catch (Exception e) {

            System.out.println("Error en el detector en vivo");

            e.printStackTrace();

        } finally {

            // Liberar recursos

            if (camara != null) {

                camara.release();

            }

            imagen.release();

            redimensionada.release();

            rgb.release();

            if (sesion != null) {

                try {

                    sesion.close();

                } catch (Exception e) {

                    System.out.println("Error al cerrar YOLO");

                }

            }

            if (entorno != null) {

                try {

                    entorno.close();

                } catch (Exception e) {

                    System.out.println("Error al cerrar ONNX Runtime");

                }

            }

            System.out.println("Cámara y recursos liberados");

        }

    }

    // Calcular cuánto se superponen dos rectángulos

    private static float calcularIoU(Rect a, Rect b) {

        int izquierda = Math.max(a.x, b.x);
        int arriba = Math.max(a.y, b.y);
        int derecha = Math.min(a.x + a.width, b.x + b.width);
        int abajo = Math.min(a.y + a.height, b.y + b.height);
        int anchoInterseccion = Math.max(0, derecha - izquierda);
        int altoInterseccion = Math.max(0, abajo - arriba);
        double interseccion = (double) anchoInterseccion * altoInterseccion;
        double areaA = (double) a.width * a.height;
        double areaB = (double) b.width * b.height;
        double union = areaA + areaB - interseccion;

        if (union <= 0) {

            return 0;

        }

        return (float) (interseccion / union);

    }

    private static String obtenerCategoria(String clase) {
        switch (clase) {
            case "laptop":
                return "computador";

            case "book":
                return "cuaderno";

            case "cell phone":
                return "telefono";

            default:
                return null;
        }
    }

}
