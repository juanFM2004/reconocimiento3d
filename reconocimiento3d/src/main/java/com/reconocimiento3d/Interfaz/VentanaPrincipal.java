
package com.reconocimiento3d.Interfaz;

import com.jme3.scene.Node;
import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.scene.Spatial;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.input.ChaseCamera;
import com.jme3.math.FastMath;
import java.nio.file.Files;
import java.nio.file.Path;

public class VentanaPrincipal extends SimpleApplication {

    private Spatial modelo;
    private ChaseCamera camaraOrbital;

    public static void main(String[] args) {

        VentanaPrincipal ventana = new VentanaPrincipal();

        AppSettings configuracion = new AppSettings(true);
        configuracion.setTitle("Reconocimiento 3D");
        configuracion.setResolution(1200, 800);
        configuracion.setVSync(true);

        ventana.setSettings(configuracion);
        ventana.setShowSettings(false);
        ventana.start();
    }

    @Override
    public void simpleInitApp() {

        viewPort.setBackgroundColor(
                new ColorRGBA(0.06f, 0.08f, 0.12f, 1f));
        setDisplayFps(false);
        setDisplayStatView(false);
        // Ajustar los planos de recorte de la cámara
        cam.setFrustumNear(0.01f);
        cam.setFrustumFar(10000f);

        try {

            Path directorioModelos = Path.of("assets", "Modelos");
            if (!Files.isDirectory(directorioModelos)) {
                directorioModelos = Path.of(
                        "reconocimiento3d", "assets", "Modelos");
            }
            if (!Files.isDirectory(directorioModelos)) {
                throw new IllegalStateException(
                        "No se encontró la carpeta de modelos desde "
                                + Path.of("").toAbsolutePath());
            }

            assetManager.registerLocator(
                    directorioModelos.toAbsolutePath().toString(),
                    FileLocator.class);

            System.out.println("Buscando modelo laptop.glb...");

            modelo = assetManager.loadModel(
                    "computador/laptop.glb");

            System.out.println("Modelo GLB cargado: " + modelo.getName());

            rootNode.attachChild(modelo);
            rootNode.updateGeometricState();
            System.out.println("Posición inicial: " + modelo.getWorldTranslation());
            System.out.println("Límites iniciales: " + modelo.getWorldBound());
            if (modelo instanceof Node) {
                System.out.println("Cantidad de hijos: "
                        + ((Node) modelo).getChildren().size());
            }

            // Iluminación ambiental moderada
            AmbientLight luzAmbiente = new AmbientLight();
            luzAmbiente.setColor(
                    new ColorRGBA(0.45f, 0.45f, 0.45f, 1f));
            rootNode.addLight(luzAmbiente);

            // Luz direccional principal
            DirectionalLight luzDireccional = new DirectionalLight();
            luzDireccional.setDirection(
                    new Vector3f(-1, -2, -3).normalizeLocal());
            luzDireccional.setColor(
                    new ColorRGBA(1f, 1f, 1f, 1f));
            rootNode.addLight(luzDireccional);

            // Calcular dimensiones del modelo
            if (!(modelo.getWorldBound() instanceof BoundingBox limites)) {
                throw new IllegalStateException(
                        "No se pudieron calcular los límites del modelo laptop.glb.");
            }

            float ancho = limites.getXExtent();
            float alto = limites.getYExtent();
            float profundidad = limites.getZExtent();
            if (ancho <= 0f || alto <= 0f || profundidad <= 0f) {
                throw new IllegalStateException(
                        "El modelo laptop.glb no tiene dimensiones válidas.");
            }

            System.out.println("Dimensiones del modelo:");
            System.out.println("Ancho: " + ancho);
            System.out.println("Alto: " + alto);
            System.out.println("Profundidad: " + profundidad);

            // Centrar modelo
            modelo.setLocalTranslation(
                    -limites.getCenter().x,
                    -limites.getCenter().y,
                    -limites.getCenter().z);
            rootNode.updateGeometricState();
            System.out.println("Posición después de centrar: "
                    + modelo.getWorldTranslation());

            float distancia = Math.max(
                    ancho,
                    Math.max(alto, profundidad)) * 1.5f;

            // Desactivar cámara de primera persona
            flyCam.setEnabled(false);

            // Seguir el centro visual, no el origen interno desplazado del GLB.
            Node objetivoCamara = new Node("Centro de la laptop");
            rootNode.attachChild(objetivoCamara);
            rootNode.updateGeometricState();

            camaraOrbital = new ChaseCamera(
                    cam, objetivoCamara, inputManager);

            camaraOrbital.setDefaultDistance(distancia);
            camaraOrbital.setMinDistance(distancia * 0.55f);
            camaraOrbital.setMaxDistance(distancia * 3f);

            camaraOrbital.setDefaultVerticalRotation(0.45f);
            camaraOrbital.setDefaultHorizontalRotation(
                    FastMath.PI * 1.5f);

            camaraOrbital.setRotationSpeed(3f);
            camaraOrbital.setZoomSensitivity(2f);
            camaraOrbital.setSmoothMotion(true);
            camaraOrbital.setSpatial(objetivoCamara);

            System.out.println("Visor 3D iniciado correctamente.");

        } catch (Exception e) {

            System.err.println("ERROR AL CARGAR EL MODELO 3D:");
            e.printStackTrace();
        }
    }
}
