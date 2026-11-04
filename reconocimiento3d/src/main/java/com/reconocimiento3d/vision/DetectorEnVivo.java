package com.reconocimiento3d.vision;

import java.awt.Desktop;
import java.net.URI;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
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

        String nombreClase;

        Deteccion(Rect caja, float confianza, int clase, String nombreClase) {

            this.caja = caja;

            this.confianza = confianza;

            this.clase = clase;

            this.nombreClase = nombreClase;

        }

    }

    public static void main(String[] args) {

        // Cargar OpenCV antes de crear cualquier objeto Mat
        OpenCV.loadLocally();
        System.out.println("OpenCV cargado correctamente");

        // Flag compartido entre el hilo principal y el servidor de cierre
        AtomicBoolean visorAbierto = new AtomicBoolean(false);

        // Servidor HTTP ligero en puerto 8001: el visor3d.html avisa cuando se cierra
        Thread servidorCierre = new Thread(() -> {
            try (ServerSocket servidor = new ServerSocket(8001)) {
                System.out.println("Servidor de cierre escuchando en puerto 8001");
                while (!Thread.currentThread().isInterrupted()) {
                    try (Socket cliente = servidor.accept()) {
                        byte[] buffer = new byte[2048];
                        int leidos = cliente.getInputStream().read(buffer);
                        if (leidos > 0) {
                            String solicitud = new String(buffer, 0, leidos);
                            // Responder con CORS para evitar bloqueos del navegador
                            String respuesta =
                                "HTTP/1.1 200 OK\r\n" +
                                "Access-Control-Allow-Origin: *\r\n" +
                                "Access-Control-Allow-Methods: POST, OPTIONS\r\n" +
                                "Content-Length: 0\r\n\r\n";
                            cliente.getOutputStream().write(respuesta.getBytes());
                            cliente.getOutputStream().flush();
                            if (solicitud.startsWith("POST /cerrar")) {
                                visorAbierto.set(false);
                                System.out.println("Visor 3D cerrado: listo para nueva detección");
                            }
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                System.out.println("Error en servidor de cierre: " + e.getMessage());
            }
        });
        servidorCierre.setDaemon(true);
        servidorCierre.start();

        VideoCapture camara = null;
        OrtSession sesion = null;
        OrtSession sesionLentes = null;
        OrtEnvironment entorno = null;

        Mat imagen = new Mat();
        Mat redimensionada = new Mat();
        Mat rgb = new Mat();

        try {
            // 2. Cargar modelos YOLO
            entorno = OrtEnvironment.getEnvironment();

            String rutaYolo = java.nio.file.Files.exists(java.nio.file.Path.of("models/yolo26n.onnx"))
                    ? "models/yolo26n.onnx" : "reconocimiento3d/models/yolo26n.onnx";
            sesion = entorno.createSession(rutaYolo, new OrtSession.SessionOptions());
            System.out.println("YOLO general cargado correctamente");

            String rutaLentes = java.nio.file.Files.exists(java.nio.file.Path.of("models/glasses_lentes.onnx"))
                    ? "models/glasses_lentes.onnx" : "reconocimiento3d/models/glasses_lentes.onnx";
            if (java.nio.file.Files.exists(java.nio.file.Path.of(rutaLentes))) {
                sesionLentes = entorno.createSession(rutaLentes, new OrtSession.SessionOptions());
                System.out.println("Modelo de lentes (glasses_lentes.onnx) cargado correctamente");
            } else {
                System.out.println("Aviso: No se encontró el modelo de lentes en " + rutaLentes);
            }

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

                                        mejorClase,

                                        clases[mejorClase]

                                )

                        );

                    }

                    // Inferencia del modelo de lentes (goggles)
                    if (sesionLentes != null) {
                        try (
                            OnnxTensor tensorLentes = OnnxTensor.createTensor(
                                    entorno,
                                    FloatBuffer.wrap(datos),
                                    new long[] { 1, 3, TAMANO, TAMANO }
                            );
                            OrtSession.Result resultadoLentes = sesionLentes.run(
                                    Collections.singletonMap("images", tensorLentes)
                            )
                        ) {
                            float[][][] salidaL = (float[][][]) resultadoLentes.get(0).getValue();
                            float[][] predL = salidaL[0];
                            int canalGoggles = 6; // Clase goggles en índice 2 (4 + 2 = 6)

                            for (int i = 0; i < predL[0].length; i++) {
                                float confLentes = predL[canalGoggles][i];
                                if (confLentes < 0.35f) {
                                    continue;
                                }

                                float centroX = predL[0][i];
                                float centroY = predL[1][i];
                                float ancho = predL[2][i];
                                float alto = predL[3][i];

                                int x = Math.round((centroX - ancho / 2) * escalaX);
                                int y = Math.round((centroY - alto / 2) * escalaY);
                                int w = Math.round(ancho * escalaX);
                                int h = Math.round(alto * escalaY);

                                x = Math.max(0, Math.min(x, anchoOriginal - 1));
                                y = Math.max(0, Math.min(y, altoOriginal - 1));
                                w = Math.max(1, Math.min(w, anchoOriginal - x));
                                h = Math.max(1, Math.min(h, altoOriginal - y));

                                detecciones.add(new Deteccion(new Rect(x, y, w, h), confLentes, 100, "lentes"));
                            }
                        } catch (Exception ignored) {}
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

                            if (!candidata.nombreClase.equals(aceptada.nombreClase)) {

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

                    // 9. Comprobar qué detectó YOLO

                    boolean laptopDetectada = false;
                    boolean telefonoDetectado = false;
                    boolean lentesDetectados = false;

                    for (Deteccion d : finales) {
                        if ("laptop".equals(d.nombreClase)) laptopDetectada = true;
                        if ("cell phone".equals(d.nombreClase)) telefonoDetectado = true;
                        if ("lentes".equals(d.nombreClase) || "goggles".equals(d.nombreClase)) lentesDetectados = true;
                    }

                    // Abrir el visor 3D solamente una vez.
                    // visorAbierto solo vuelve a false cuando el tab del navegador se cierra
                    // (el HTML llama a POST localhost:8001/cerrar en beforeunload).
                    try {
                        if (!visorAbierto.get() && Desktop.isDesktopSupported()
                                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {

                            String modelo = null;
                            if (lentesDetectados)        modelo = "lentes";
                            else if (laptopDetectada)    modelo = "computador";
                            else if (telefonoDetectado) modelo = "telefono";

                            if (modelo != null) {
                                Desktop.getDesktop().browse(
                                        new URI("http://localhost:8000/visor3d.html?modelo=" + modelo));
                                visorAbierto.set(true);
                                System.out.println("Objeto detectado (" + modelo + "): abriendo modelo 3D");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("No se pudo abrir el visor 3D: " + e.getMessage());
                    }

                    for (Deteccion d : finales) {
                        Rect caja = d.caja;
                        Scalar color = "lentes".equals(d.nombreClase)
                                ? new Scalar(255, 165, 0)
                                : new Scalar(0, 255, 0);

                        Imgproc.rectangle(
                                imagen,
                                caja,
                                color,
                                2);

                        String categoria = obtenerCategoria(d.nombreClase);

                        String etiqueta = String.format(
                                Locale.US,
                                "%s (%.0f%%)",
                                categoria != null ? categoria : d.nombreClase,
                                d.confianza * 100);

                        int textoY = Math.max(20, caja.y - 8);
                        Imgproc.putText(
                                imagen,
                                etiqueta,
                                new Point(caja.x, textoY),
                                Imgproc.FONT_HERSHEY_SIMPLEX,
                                0.65,
                                color,
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

            if (sesionLentes != null) {

                try {

                    sesionLentes.close();

                } catch (Exception e) {

                    System.out.println("Error al cerrar modelo de lentes");

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

            case "glasses":
            case "goggles":
            case "lentes":
                return "lentes";

            default:
                return null;
        }
    }

}
