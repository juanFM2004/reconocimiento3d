# Reconocimiento3D

Proyecto Java orientado a la captura de vídeo y a la identificación de objetos para mostrar modelos 3D asociados. Incluye una ventana 3D que carga y centra el modelo de laptop, además de las herramientas de captura de vídeo con OpenCV.

## Estado actual

### Funcionalidades implementadas

- Compilación del proyecto con Maven.
- Ejecución de una aplicación de comprobación que confirma que Java se inicia correctamente.
- Carga local de OpenCV desde las dependencias del proyecto.
- Apertura de la cámara predeterminada del equipo, identificada como dispositivo `0`.
- Captura de una imagen individual y muestra de sus dimensiones.
- Visualización continua de la cámara en una ventana gráfica de 800 x 600 píxeles.
- Conversión de cada fotograma de OpenCV (`Mat`) a una imagen que Swing puede mostrar.
- Visualización de `assets/Modelos/computador/laptop.glb` en la ventana 3D `VentanaPrincipal`.
- Prueba unitaria básica de JUnit.

### Funcionalidades aún no implementadas

- `DetectorObjetos` no ejecuta reconocimiento: siempre devuelve `"ninguno"`.
- ONNX Runtime está declarado como dependencia, pero ningún código carga todavía un modelo `.onnx` ni realiza inferencia.
- `GestorModelos` existe como clase vacía; no carga, selecciona ni renderiza modelos 3D.
- La carpeta `models/` no contiene modelos actualmente.
- No existe todavía un flujo que conecte cámara, detector, modelo reconocido e interfaz 3D.

## Requisitos para instalarlo en otro dispositivo

### Requisitos mínimos

1. Un sistema operativo compatible con Java y Maven. Los comandos de esta guía están escritos para Windows PowerShell, pero el proyecto usa tecnologías multiplataforma.
2. JDK 17, no solamente un JRE. El `pom.xml` compila con `source`, `target` y `release` 17.
3. Apache Maven 3.8 o superior recomendado.
4. Una cámara web funcional si se desea ejecutar `Camara` o `VisorCamara`.
5. Un entorno gráfico disponible para `VisorCamara`; no funcionará en un servidor sin escritorio, una sesión remota sin GUI o un entorno completamente headless.
6. Conexión a Internet durante la primera compilación, para que Maven descargue las dependencias desde Maven Central. Después Maven podrá reutilizar su caché local.

### Comprobar la instalación

```powershell
java -version
javac -version
mvn -version
```

La salida de Java debe indicar la versión 17 o superior compatible con el nivel de compilación configurado. Si `java` funciona pero `javac` no existe, se instaló un JRE en lugar de un JDK. En Windows también hay que comprobar que `JAVA_HOME` apunta al directorio del JDK y que `%JAVA_HOME%\bin` está en `PATH`.

## Dependencias

Las dependencias se resuelven automáticamente desde `reconocimiento3d/pom.xml`:

| Dependencia | Versión | Uso actual |
| --- | --- | --- |
| `org.openpnp:opencv` | `4.9.0-0` | API Java y librerías nativas utilizadas para abrir la cámara, capturar fotogramas y convertir imágenes. Incluye la clase `nu.pattern.OpenCV`. |
| `com.microsoft.onnxruntime:onnxruntime` | `1.22.0` | Dependencia prevista para inferencia de modelos ONNX; todavía no es utilizada por las clases actuales. |
| `org.jmonkeyengine:jme3-core`, `jme3-desktop`, `jme3-lwjgl3`, `jme3-plugins` | `3.8.0-stable` | Renderizado de la ventana 3D y carga del modelo glTF/GLB. |
| `junit:junit` | `4.11` | Pruebas unitarias; solo se incluye durante la fase de test. |
| `org.hamcrest:hamcrest-core` | transitiva | Soporte de JUnit. Maven la descarga automáticamente. |

No es necesario instalar OpenCV manualmente ni copiar un `.dll` si se utiliza la dependencia configurada: las clases de cámara llaman a `OpenCV.loadLocally()`. Maven sí necesita poder escribir en el repositorio local del usuario, normalmente `%USERPROFILE%\.m2\repository`.

## Instalación y compilación

Desde la raíz del repositorio:

```powershell
cd .\reconocimiento3d
mvn clean test
```

Este comando:

1. Elimina el directorio de compilación `target/`.
2. Descarga las dependencias que falten.
3. Compila las clases de `src/main/java` con Java 17.
4. Compila y ejecuta la prueba de `src/test/java`.
5. Genera los archivos compilados en `target/classes` y `target/test-classes`.

Para generar también el JAR:

```powershell
mvn clean package
```

El JAR se crea en `target/reconocimiento3d-1.0-SNAPSHOT.jar`. No contiene una configuración de clase principal ejecutable ni empaqueta todas las dependencias, por lo que para ejecutar las clases con dependencias es más sencillo usar `mvn exec:java` o construir un classpath.

## Cómo ejecutar cada parte

### 1. Aplicación principal

```powershell
mvn exec:java -Dexec.mainClass="com.reconocimiento3d.App"
```

`App` solo imprime un encabezado y los mensajes `Proyecto iniciado correctamente` y `Java funcionando correctamente`. No abre la cámara ni la ventana gráfica.

Para abrir la ventana que muestra el modelo 3D, ejecuta desde `reconocimiento3d`:

```powershell
mvn exec:java -Dexec.mainClass="com.reconocimiento3d.Interfaz.VentanaPrincipal"
```

La aplicación carga `assets/Modelos/computador/laptop.glb`; usa el ratón para orbitar y la rueda para acercar o alejar.

### 2. Prueba de cámara sin interfaz gráfica

```powershell
mvn exec:java -Dexec.mainClass="com.reconocimiento3d.vision.Camara"
```

El flujo es:

1. Ejecuta `OpenCV.loadLocally()` para cargar OpenCV.
2. Crea `VideoCapture(0)` y solicita la cámara predeterminada.
3. Comprueba `isOpened()`.
4. Lee un solo fotograma en un objeto `Mat`.
5. Informa de la captura y de su ancho y alto.
6. Libera la cámara con `release()`.

Si no se abre la cámara, el programa imprime `No se pudo abrir la cámara` y termina. El índice `0` representa la cámara predeterminada; cambiarlo por `1` o `2` permite probar otros dispositivos.

### 3. Visor de cámara en tiempo real

```powershell
mvn exec:java -Dexec.mainClass="com.reconocimiento3d.vision.VisorCamara"
```

Esta clase abre una ventana Swing llamada `Reconocimiento 3D - Cámara`, crea un `JLabel` y, mientras la ventana está visible, ejecuta el siguiente ciclo:

1. Lee un fotograma de `VideoCapture` en un `Mat`.
2. Codifica el `Mat` como JPEG usando `Imgcodecs.imencode()`.
3. Convierte los bytes JPEG a `BufferedImage` mediante `ImageIO`.
4. Escala la imagen al tamaño actual del `JLabel`.
5. Actualiza el icono del `JLabel` para mostrar el fotograma.
6. Libera el buffer de bytes.

Al cerrar la ventana, el bucle deja de ejecutarse y se libera la cámara. El visor no detecta objetos ni superpone modelos 3D; únicamente muestra el vídeo capturado.

## Estructura del código

```text
reconocimiento3d/
├── pom.xml
├── assets/
│   └── Modelos/       # Modelos 3D (incluye computador/laptop.glb)
├── models/            # Actualmente vacío; reservado para modelos de IA o 3D
├── src/
│   ├── main/java/com/reconocimiento3d/
│   │   ├── App.java
│   │   ├── Interfaz/VentanaPrincipal.java
│   │   ├── modelos/GestorModelos.java
│   │   └── vision/
│   │       ├── Camara.java
│   │       ├── DetectorObjetos.java
│   │       └── VisorCamara.java
│   └── test/java/com/reconocimiento3d/AppTest.java
└── target/            # Generado por Maven; no es código fuente
```

### Responsabilidad de cada clase

- **`App`**: punto de entrada de demostración. No coordina todavía los demás componentes.
- **`Camara`**: prueba técnica mínima de OpenCV. Captura un fotograma y muestra sus dimensiones por consola.
- **`VisorCamara`**: prueba visual de captura continua. Une OpenCV con Swing y administra el ciclo de vida de la ventana y de la cámara.
- **`DetectorObjetos`**: API inicial para reconocimiento. Su método `detectarObjeto()` devuelve siempre `"ninguno"`; es un placeholder para conectar una futura IA.
- **`VentanaPrincipal`**: ventana 3D; carga, centra e ilumina el modelo de laptop y permite orbitar la cámara.
- **`GestorModelos`**: lugar previsto para administrar modelos 3D, actualmente sin atributos ni métodos.
- **`AppTest`**: prueba JUnit de ejemplo que verifica `assertTrue(true)`. Sirve para comprobar el pipeline de tests, pero no valida la cámara ni el reconocimiento.

## Flujo previsto del sistema

La arquitectura que sugieren los paquetes es:

```text
Cámara web
	↓
OpenCV / VideoCapture
	↓
Fotograma Mat
	↓
DetectorObjetos + modelo ONNX  (pendiente de implementar)
	↓
Nombre o clase del objeto
	↓
GestorModelos                     (pendiente de implementar)
	↓
VentanaPrincipal / visor 3D       (visor disponible; falta conectar la selección automática)
```

El visor 3D puede abrirse directamente con `VentanaPrincipal`; aún falta conectar la detección automática con la selección del modelo. La dependencia de ONNX Runtime no produce ningún resultado por sí sola: todavía sería necesario cargar un archivo `.onnx`, preparar el tensor de entrada, ejecutar la inferencia, interpretar las clases y conectar el resultado con el gestor de modelos.

## Solución de problemas

### Maven no reconoce `java` o `javac`

Instala un JDK 17, configura `JAVA_HOME` y reinicia la terminal o VS Code para que se actualice el `PATH`.

### Maven no puede descargar dependencias

Comprueba la conexión a Internet, el proxy corporativo y los permisos de `%USERPROFILE%\.m2`. Ejecuta de nuevo `mvn clean test` después de corregirlos.

### `No se pudo abrir la cámara`

Verifica que otra aplicación no la esté usando, concede permisos de cámara a Java/VS Code, conecta la cámara y prueba otro índice (`0`, `1` o `2`) en `VideoCapture`.

### Error al cargar OpenCV o una librería nativa

Usa la arquitectura de Java compatible con el sistema operativo, normalmente un JDK de 64 bits en Windows de 64 bits. Elimina la descarga incompleta de OpenCV del repositorio Maven local y vuelve a ejecutar `mvn clean test` si la primera descarga se interrumpió.

### El visor abre pero no muestra imagen

Comprueba que la cámara entrega fotogramas y que el `JLabel` tiene tamaño visible. La implementación actual escala cada imagen a las dimensiones del componente y no incluye controles de resolución ni tratamiento avanzado de errores.

## Desarrollo pendiente

Para convertir este prototipo en un sistema completo de reconocimiento 3D habría que implementar, al menos:

1. La captura desacoplada de la interfaz gráfica, evitando bloquear el hilo de Swing.
2. El preprocesamiento de imágenes requerido por el modelo de IA.
3. La carga y ejecución de un modelo ONNX en `DetectorObjetos`.
4. La asociación entre etiquetas detectadas y archivos de `assets/Modelos` o `models/`.
5. La selección automática del modelo 3D en `GestorModelos` a partir de las detecciones.
6. Manejo de errores, cierre seguro de recursos y configuración de cámara.
7. Pruebas que cubran captura, detección, selección de modelos y ejecución en equipos sin cámara.

## Licencia

Este repositorio no declara actualmente una licencia en el proyecto. Añade una licencia antes de distribuir el código o los modelos/assets a terceros.
