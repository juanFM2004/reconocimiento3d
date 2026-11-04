"""
Genera un modelo ONNX para detección de lentes/anteojos usando Ultralytics YOLOv8.
Descarga un modelo preentrenado de Roboflow Universe (dataset público de lentes)
y lo exporta a formato ONNX para usarlo desde Java.
"""

from ultralytics import YOLO
import os, sys

API_KEY = "YOUR_ROBOFLOW_API_KEY"  # Reemplaza con tu API key de roboflow.com (gratis)

def main():
    print("=== Generador de modelo ONNX para lentes ===\n")

    if API_KEY == "YOUR_ROBOFLOW_API_KEY":
        # Sin API key: usar fine-tuning rápido sobre YOLOv8n con dataset integrado de glasses
        # Usamos el modelo Tanishjain9/yolov8n-ppe-detection-6classes de Hugging Face
        # que incluye goggles/glasses como clase
        print("Sin API key de Roboflow. Usando método alternativo con Hugging Face...")
        generar_sin_apikey()
    else:
        generar_con_roboflow()

def generar_sin_apikey():
    """
    Descarga el modelo YOLOv8n de PPE (incluye clase 'glasses/goggles')
    desde Hugging Face y lo exporta a ONNX.
    """
    import urllib.request

    modelo_hf = "https://huggingface.co/Tanishjain9/yolov8n-ppe-detection-6classes/resolve/main/best.pt"
    ruta_pt = "models/glasses_lentes.pt"
    ruta_onnx = "models/glasses_lentes.onnx"

    os.makedirs("models", exist_ok=True)

    if not os.path.exists(ruta_pt):
        print(f"Descargando modelo desde Hugging Face...")
        urllib.request.urlretrieve(modelo_hf, ruta_pt)
        print(f"Modelo descargado: {ruta_pt}")
    else:
        print(f"Modelo ya existe: {ruta_pt}")

    print("Exportando a ONNX...")
    model = YOLO(ruta_pt)
    model.export(format="onnx", imgsz=640, simplify=True)

    # Ultralytics guarda el ONNX en la misma carpeta con el mismo nombre
    onnx_generado = ruta_pt.replace(".pt", ".onnx")
    if os.path.exists(onnx_generado):
        print(f"\n✅ Modelo ONNX generado: {onnx_generado}")
        print(f"Clases del modelo:")
        m = YOLO(ruta_pt)
        for i, nombre in m.names.items():
            print(f"  [{i}] {nombre}")
    else:
        print("❌ No se encontró el archivo ONNX generado.")
        sys.exit(1)

def generar_con_roboflow():
    """Descarga dataset de lentes de Roboflow y entrena YOLOv8n."""
    print("Conectando a Roboflow...")
    rf = Roboflow(api_key=API_KEY)
    project = rf.workspace("yolov8glasses").project("eyeglasses-cd3iy")
    version = project.version(1)
    dataset = version.download("yolov8")

    print("Entrenando YOLOv8n en dataset de lentes (10 épocas)...")
    model = YOLO("yolov8n.pt")
    model.train(
        data=f"{dataset.location}/data.yaml",
        epochs=10,
        imgsz=640,
        project="models",
        name="lentes_train",
        exist_ok=True
    )

    mejor = "models/lentes_train/weights/best.pt"
    print("Exportando a ONNX...")
    model = YOLO(mejor)
    model.export(format="onnx", imgsz=640, simplify=True)
    print(f"\n✅ Modelo ONNX generado en: models/lentes_train/weights/best.onnx")

if __name__ == "__main__":
    main()
