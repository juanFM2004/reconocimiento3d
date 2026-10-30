import ai.onnxruntime.OrtEnvironment;

public class TestONNX {

    public static void main(String[] args) {
        System.out.println("Java funcionando");
        System.out.println("Arquitectura: " + System.getProperty("os.arch"));

        try {
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            System.out.println("ONNX Runtime funciona correctamente");
            env.close();
        } catch (Exception e) {
            System.out.println("ONNX Runtime presentó un error:");
            e.printStackTrace();
        }
    }
}