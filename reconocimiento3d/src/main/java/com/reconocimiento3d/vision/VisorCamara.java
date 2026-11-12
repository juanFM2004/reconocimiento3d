package com.reconocimiento3d.vision;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

public class VisorCamara {

    public static void main(String[] args) {

        // Cargar OpenCV
        OpenCV.loadLocally();

        System.out.println("OpenCV cargado correctamente");

        // Abrir cámara (DirectShow prioritario en Windows para evitar errores MSMF)
        VideoCapture camara = new VideoCapture(0, Videoio.CAP_DSHOW);
        if (!camara.isOpened()) {
            camara = new VideoCapture(0);
        }

        if (!camara.isOpened()) {
            System.out.println("No se pudo abrir la cámara");
            return;
        }

        System.out.println("Cámara abierta correctamente");

        // Crear ventana
        JFrame ventana = new JFrame("Reconocimiento 3D - Cámara");

        JLabel pantalla = new JLabel();

        ventana.add(pantalla);

        ventana.setSize(800, 600);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setVisible(true);

        // Mat para almacenar cada imagen
        Mat imagen = new Mat();

        // Video en tiempo real
        while (ventana.isVisible()) {

            if (camara.read(imagen)) {

                // Convertir imagen de OpenCV a imagen de Java
                MatOfByte buffer = new MatOfByte();

                Imgcodecs.imencode(".jpg", imagen, buffer);

                try {

                    BufferedImage imagenJava = ImageIO.read(
                        new ByteArrayInputStream(buffer.toArray())
                    );

                    if (imagenJava != null) {
                        int ancho = pantalla.getWidth() > 0 ? pantalla.getWidth() : 800;
                        int alto = pantalla.getHeight() > 0 ? pantalla.getHeight() : 600;
                        Image imagenEscalada = imagenJava.getScaledInstance(
                            ancho,
                            alto,
                            Image.SCALE_FAST
                        );

                        pantalla.setIcon(new ImageIcon(imagenEscalada));
                    }

                } catch (Exception e) {

                    System.out.println("Error al mostrar la imagen");
                    e.printStackTrace();
                }

                buffer.release();
            }
        }

        // Cerrar cámara
        camara.release();

        System.out.println("Cámara cerrada");
    }
}