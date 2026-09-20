package com.reconocimiento3d.vision;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class Camara {

    public static void main(String[] args) {

        // Cargar OpenCV desde el proyecto
        OpenCV.loadLocally();

        System.out.println("OpenCV cargado correctamente");

        // Abrir la cámara predeterminada
        VideoCapture camara = new VideoCapture(0);

        if (!camara.isOpened()) {
            System.out.println("No se pudo abrir la cámara");
            return;
        }

        System.out.println("Cámara abierta correctamente");

        // Crear espacio para la imagen
        Mat imagen = new Mat();

        // Capturar una imagen
        if (camara.read(imagen)) {

            System.out.println("Imagen capturada correctamente");
            System.out.println("Ancho: " + imagen.width());
            System.out.println("Alto: " + imagen.height());

        } else {

            System.out.println("No se pudo capturar la imagen");
        }

        // Liberar la cámara
        camara.release();

        System.out.println("Cámara cerrada");
    }
}