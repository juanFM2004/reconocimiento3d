# Reconocimiento3D

Sistema interactivo de visión artificial e inteligencia artificial en tiempo real desarrollado en **Java**, con detección de objetos mediante **YOLO** (en formato interoperable **ONNX**) y visualización 3D interactiva renderizada con **Three.js** y **jMonkeyEngine**.

---

## 🌟 Características Principales

* **Detección Dual en Tiempo Real (IA):**
  * **YOLO General (`yolo26n.onnx`):** Identificación de laptops/computadores portátiles y teléfonos celulares en tiempo real.
  * **YOLO Especializado (`glasses_lentes.onnx`):** Red neuronal dedicada a la detección precisa de lentes / gafas.
* **Procesamiento de Video con OpenCV:**
  * Captura de cámara web en vivo, conversión de espacios de color (BGR a RGB), reescalado a $640 \times 640$, normalización matricial y formato tensorial NCHW.
  * Algoritmo de filtrado **Non-Maximum Suppression (NMS)** con umbral de solapamiento IoU (*Intersection over Union*) para descartar predicciones redundantes.
* **Visor 3D Interactivo (Three.js WebGL):**
  * Carga asíncrona de modelos en formato glTF binario (`.glb`) con soporte para compresión Draco (`DRACOLoader`).
  * Iluminación y texturizado fotorrealista PBR (*Physically Based Rendering*) mediante `ACESFilmicToneMapping` y espacio de color `sRGB`.
  * **Normalización Dinámica de Escala y Centrado:** Ajusta de forma automática cualquier modelo 3D (venga en milímetros, centímetros o metros) a un tamaño estándar centrado en el origen `(0, 0, 0)`.
  * Control de cámara orbital (*orbit controls*) con prevención de corte o distorsión geométrica en zoom cercano (`camara.near = 0.001`).
  * **Modo Desarmado / Vista Explotada Interactiva (Laptop, Teléfono y Lentes):**
    * **Laptop (`computador`):**
      * Muestra la laptop armada (`laptop.glb`) con su panel de **especificaciones técnicas generales** (CPU, GPU, RAM, SSD, Pantalla, Batería, etc.).
      * Botón dinámico `[ 🔧 Desarmar laptop ]` / `[ 📦 Armar laptop ]` para alternar entre el modelo ensamblado y el modelo de vista explotada (`laptop_exploded_view.glb`).
      * **Inspección de Componentes por Clic:** Marcadores 3D flotantes con animación, raycasting sobre las piezas y barra de navegación inferior. Al seleccionar cualquier componente (Pantalla, CPU/GPU, RAM, SSD NVMe, Batería, Refrigeración, Placa Base, Teclado, Touchpad, Carcasa), despliega una ficha técnica con: ¿Qué es?, ¿Para qué sirve? y Capacidades detalladas.
    * **Teléfono Móvil (`telefono`):**
      * Muestra el smartphone ensamblado (`smartphone.glb`) con su ficha de **especificaciones técnicas completas** (SoC Snapdragon, RAM LPDDR5X, pantalla AMOLED LTPO, cámaras triple, batería Si-C, USB-C 3.2, etc.).
      * Botón dinámico `[ 🔧 Desarmar teléfono ]` / `[ 📦 Armar teléfono ]` para alternar con la vista explotada (`smartphone_disassembly.glb`).
      * **Inspección de Componentes por Clic:** 9 marcadores 3D flotantes con raycasting y ficha detallada para: Pantalla AMOLED, Placa Base/SoC, Batería Si-C, Cámara Triple Trasera, Altavoz Superior, Altavoz/USB-C Inferior, Cámara Selfie, Botón de Encendido/Huella y Botones de Volumen.
    * **Lentes / Gafas (`lentes`):**
      * Muestra los lentes armados (`glasses.glb`) con su ficha de **especificaciones ópticas y ergonómicas generales** (Material de Montura, Lentes CR-39 / Policarbonato, Tratamientos UV400 / Antirreflejo, Peso ultraligero, etc.).
      * Botón dinámico `[ 🔧 Desarmar lentes ]` / `[ 📦 Armar lentes ]` para alternar con la vista explotada (`eyeglasses_exploded.glb`).
      * **Inspección de Componentes por Clic:** Marcadores 3D flotantes y raycasting sobre las partes clave (Montura Frontal y Puente, Cristales/Lentes Derecho e Izquierdo, Patillas Ergonómicas Derecha e Izquierda, Plaquetas Nasales de Silicona Hipoalergénica, Bisagras Flex con Micro-tornillos de Acero Inoxidable) con descripción completa, función y especificaciones técnicas.
* **Sincronización Inteligente de Visor:**
  * Servidor socket interno en puerto `8001` sincronizado con eventos del navegador (`navigator.sendBeacon` en `beforeunload`).
  * Evita la apertura repetitiva o descontrolada de pestañas del navegador mientras un objeto 3D se encuentra en visualización activa.

---

## 🔄 Flujo de Arquitectura del Sistema

```text
               [ Cámara Web ]
                     │
                     ▼
             [ OpenCV VideoCapture ]
                     │
                     ▼  (BGR → RGB, 640x640, Normalización 0-1, NCHW)
           [ Tensor FloatBuffer ]
                     │
         ┌───────────┴───────────┐
         ▼                       ▼
  [ yolo26n.onnx ]     [ glasses_lentes.onnx ]
 (Laptop / Teléfono)          (Lentes)
         └───────────┬───────────┘
                     │  (Predicciones + NMS)
                     ▼
          [ Detecciones Finales ]
          (Cajas, Confianza, Clases)
                     │
                     ▼  (¿Visor ya está abierto?)
          [ Control de Estado ] ◄───────┐
                     │                  │  Notificación de cierre
          (Abrir si está cerrado)       │  (POST :8001/cerrar)
                     ▼                  │
        [ Navegador Predeterminado ] ───┘
         http://localhost:8000/visor3d.html?modelo={computador|telefono|lentes}
                     │
                     ▼
           [ Three.js + WebGL ]
       - ACES Filmic Tone Mapping
       - Escala normalizada (Box3)
       - Cámara orbital y texturas PBR
```

---

## 📁 Estructura del Repositorio

```text
Reconocimiento3D/
├── README.md                                # Documentación principal
├── reconocimiento3d/
│   ├── pom.xml                              # Dependencias Maven (OpenCV, ONNX Runtime, jME3)
│   ├── visor3d.html                         # Visor 3D web interactivo en Three.js
│   ├── generar_modelo_lentes.py             # Script de descarga y exportación a ONNX
│   ├── assets/
│   │   ├── draco/                           # Decodificadores locales de compresión Draco
│   │   └── Modelos/                         # Modelos 3D en formato GLB
│   │       ├── computador/laptop.glb        # Modelo 3D de computadora portátil ensamblada
│   │       ├── computador/laptop_exploded_view.glb # Modelo 3D de laptop desarmada (vista explotada)
│   │       ├── telefono/smartphone.glb      # Modelo 3D de teléfono móvil
│   │       ├── lentes/glasses.glb           # Modelo 3D de lentes / anteojos ensamblados
│   │       ├── lentes/eyeglasses_exploded.glb # Modelo 3D de lentes desarmados (vista explotada)
│   │       ├── telefono/smartphone.glb      # Modelo 3D de teléfono móvil ensamblado
│   │       └── telefono/smartphone_disassembly.glb # Modelo 3D de smartphone desarmado (vista explotada)
│   ├── models/                              # Modelos de Inteligencia Artificial
│   │   ├── yolo26n.onnx                     # Modelo YOLO COCO general
│   │   ├── glasses_lentes.pt                # Pesos base de PyTorch para lentes
│   │   └── glasses_lentes.onnx              # Modelo YOLO exportado a ONNX para lentes
│   └── src/
│       ├── main/java/com/reconocimiento3d/
│       │   ├── App.java                     # Comprobación básica de inicio
│       │   ├── Interfaz/
│       │   │   └── VentanaPrincipal.java    # Visor 3D nativo en escritorio (jMonkeyEngine)
│       │   ├── modelos/
│       │   │   └── GestorModelos.java       # Estructura para administración de modelos
│       │   └── vision/
│       │       ├── Camara.java              # Prueba elemental de captura OpenCV
│       │       ├── VisorCamara.java         # Transmisión de cámara continua en ventana Swing
│       │       ├── DetectorObjetos.java     # Prueba de inferencia YOLO sobre imagen estática
│       │       └── DetectorEnVivo.java      # Detector principal en vivo + apertura del visor
│       └── test/java/com/reconocimiento3d/
│           └── AppTest.java                 # Pruebas unitarias JUnit
```

---

## 🛠️ Requisitos e Instalación

### Requisitos Previos

1. **Java Development Kit (JDK 17 o superior)**.
2. **Apache Maven (3.8 o superior)** configurado en el `PATH`.
3. **Python 3.8+** (utilizado para el servidor HTTP local y exportación de modelos).
4. Cámara web integrada o USB.

### Verificar Herramientas en Consola

```powershell
java -version
mvn -version
python --version
```

### Compilar el Proyecto Java

Desde la carpeta `reconocimiento3d`:

```powershell
cd reconocimiento3d
mvn clean compile
```

---

## 🚀 Guía de Ejecución

Para el funcionamiento completo del sistema, se requieren **dos terminales**:

### Terminal 1: Servidor Local para el Visor 3D (Three.js)

Los navegadores bloquean la carga de modelos 3D locales (`file://`) por políticas de seguridad CORS. Por ello, iniciamos un servidor local rápido en el puerto `8000`:

```powershell
cd reconocimiento3d
python -m http.server 8000
```

> La interfaz web estará disponible en `http://localhost:8000/visor3d.html`.

---

### Terminal 2: Detector de Visión Artificial en Vivo (Java)

En una segunda terminal, ejecuta el detector inteligente con cámara:

```powershell
cd reconocimiento3d
mvn exec:java "-Dexec.mainClass=com.reconocimiento3d.vision.DetectorEnVivo"
```

* **Qué ocurrirá:**
  1. Se abrirá una ventana de Swing mostrando la cámara en tiempo real.
  2. Al enfocar una **laptop**, un **celular** o **lentes**, se dibujará la caja delimitadora con su porcentaje de confianza.
  3. Se abrirá automáticamente tu navegador web mostrando el modelo 3D correspondiente.
  4. Mientras la pestaña 3D esté abierta, el detector seguirá procesando video sin abrir nuevas ventanas.
  5. Al cerrar la pestaña del navegador, el sistema queda inmediatamente listo para escanear y abrir un nuevo objeto.

---

## 🧪 Pruebas Adicionales

### 1. Visor 3D Nativo de Escritorio (jMonkeyEngine)
Si deseas visualizar el modelo 3D en una ventana de escritorio nativa sin navegador web:
```powershell
cd reconocimiento3d
mvn exec:java "-Dexec.mainClass=com.reconocimiento3d.Interfaz.VentanaPrincipal"
```

### 2. Prueba de Inferencia YOLO sobre Imagen Estática
Para probar el pipeline matemático de YOLO procesando un archivo de imagen estático (`bus.jpg`):
```powershell
cd reconocimiento3d
mvn exec:java "-Dexec.mainClass=com.reconocimiento3d.vision.DetectorObjetos"
```

### 3. Re-exportar o Actualizar el Modelo de Lentes
Si deseas volver a descargar o generar el archivo `glasses_lentes.onnx`:
```powershell
cd reconocimiento3d
python generar_modelo_lentes.py
```

---

## 📦 Dependencias Principales (`pom.xml`)

| Dependencia | Versión | Propósito |
| --- | --- | --- |
| `org.openpnp:opencv` | `4.9.0-0` | Captura de video, procesamiento de imagen y pintado de interfaces. |
| `com.microsoft.onnxruntime:onnxruntime` | `1.20.0` | Motor de inferencia de redes neuronales ONNX de alto rendimiento en CPU/GPU. |
| `org.jmonkeyengine:*` | `3.8.0-stable` | Motor gráfico 3D nativo para Java con soporte para carga de archivos GLTF/GLB. |
| `junit:junit` | `4.11` | Suite para pruebas unitarias. |
