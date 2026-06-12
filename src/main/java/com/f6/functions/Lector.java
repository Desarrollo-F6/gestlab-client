package com.f6.functions;

import com.machinezoo.sourceafis.FingerprintImage;
import com.machinezoo.sourceafis.FingerprintMatcher;
import com.machinezoo.sourceafis.FingerprintTemplate;
import com.zkteco.biometric.FingerprintSensorErrorCode;
import com.zkteco.biometric.FingerprintSensorEx;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;



public class Lector {

    //Handle = manejo
    public static long devHandle = 0, dbHandle = 0;

    //    @Autowired
//    private SimpMessagingTemplate messagingTemplate;

    private static boolean mbStop = true; //Para detener el escaneo

    public static byte[] imgBuffer = null;
    private static byte[] template = new byte[2048];
    private static int[] templateLen = new int[1];

    private static int fid = 0, ret = 0; //ID de la huella

    private static String lastBase64Template = "";
    private static String lastBlobTemplate = "";
    private static boolean isCaptured = false;

    //La hanchura de la huella
    public static int fpWidth = 0;
    //La altura de la huella
    public static int fpHeight = 0;

    //Convierte la huella a imagen solo pruebas
    public static void decodeBase64ToImage(String base64String, String outputPath) {
        try {
            // Decodifica la cadena Base64 a un arreglo de bytes
            byte[] imageBytes = Base64.getDecoder().decode(base64String);

            // Escribe los bytes en un archivo
            try (OutputStream outputStream = new FileOutputStream(outputPath)) {
                outputStream.write(imageBytes);
                System.out.println("Imagen guardada en: " + outputPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error al decodificar la imagen.");
        }
    }

    public static int byteArrayToInt(byte[] bytes) {
        int number = bytes[0] & 0xFF;
        number |= ((bytes[1] << 8) & 0xFF00);
        number |= ((bytes[2] << 16) & 0xFF0000);
        number |= ((bytes[3] << 24) & 0xFF000000);
        return number;
    }

    public static byte[] intToByteArray(int number) {
        return new byte[] {
                (byte) (number & 0xFF),
                (byte) ((number >> 8) & 0xFF),
                (byte) ((number >> 16) & 0xFF),
                (byte) ((number >> 24) & 0xFF)
        };
    }

    public static void writeBitmap(byte[] imageBuf, int nWidth, int nHeight,
                                   String path) throws IOException, FileNotFoundException {
        FileOutputStream fos = new FileOutputStream(path);
        java.io.DataOutputStream dos = new java.io.DataOutputStream(fos);

        System.out.println();

        int w = (((nWidth+3)/4)*4);
        int bfType = 0x424d;
        int bfSize = 54 + 1024 + w * nHeight;
        int bfReserved1 = 0;
        int bfReserved2 = 0;
        int bfOffBits = 54 + 1024;

        dos.writeShort(bfType);
        dos.write(intToByteArray(bfSize), 0, 4);
        dos.write(intToByteArray(bfReserved1), 0, 2);
        dos.write(intToByteArray(bfReserved2), 0, 2);
        dos.write(intToByteArray(bfOffBits), 0, 4);

        int biSize = 40;
        int biWidth = nWidth;
        int biHeight = nHeight;
        int biPlanes = 1;
        int biBitcount = 8;
        int biCompression = 0;
        int biSizeImage = w * nHeight;
        int biXPelsPerMeter = 0;
        int biYPelsPerMeter = 0;
        int biClrUsed = 0;
        int biClrImportant = 0;

        dos.write(intToByteArray(biSize), 0, 4);
        dos.write(intToByteArray(biWidth), 0, 4);
        dos.write(intToByteArray(biHeight), 0, 4);
        dos.write(intToByteArray(biPlanes), 0, 2);
        dos.write(intToByteArray(biBitcount), 0, 2);
        dos.write(intToByteArray(biCompression), 0, 4);
        dos.write(intToByteArray(biSizeImage), 0, 4);
        dos.write(intToByteArray(biXPelsPerMeter), 0, 4);
        dos.write(intToByteArray(biYPelsPerMeter), 0, 4);
        dos.write(intToByteArray(biClrUsed), 0, 4);
        dos.write(intToByteArray(biClrImportant), 0, 4);

        for (int i = 0; i < 256; i++) {
            dos.writeByte(i);
            dos.writeByte(i);
            dos.writeByte(i);
            dos.writeByte(0);
        }

        byte[] filter = null;
        if (w > nWidth)
        {
            filter = new byte[w-nWidth];
        }

        for(int i=0;i<nHeight;i++)
        {
            dos.write(imageBuf, (nHeight-1-i)*nWidth, nWidth);
            if (w > nWidth)
                dos.write(filter, 0, w-nWidth);
        }
        dos.flush();
        dos.close();
        fos.close();
    }

    // Funcion el cual abre el dispositivo
    public static String openDevice(){
        byte[] paramValue = new byte[4];
        int[] size = new int[1];

        if(FingerprintSensorErrorCode.ZKFP_ERR_OK != FingerprintSensorEx.Init()) {
            Lector.CloseDevice();
            return "Error initializing sensor";
        }

        int ret = FingerprintSensorEx.GetDeviceCount();
        if(ret < 0) {
            Lector.CloseDevice();
            return "No fingerprint sensor found";
        }

        if((devHandle = FingerprintSensorEx.OpenDevice(0)) == 0){
            Lector.CloseDevice();
            return "Error opening device";
        }

        if((dbHandle = FingerprintSensorEx.DBInit()) == 0) {
            Lector.CloseDevice();
            return "Error initializing database";
        }

        // 5010 es el parámetro de formato del Algoritmo en el DBInit
        // 0 = ZKFinger V10.0 (Propietario)
        // 1 = ISO 19794-2
        // 2 = ANSI 378 (Estándar habitual de las APIs de huellas)
        FingerprintSensorEx.DBSetParameter(dbHandle, 5010, 2);

        size[0] = 4;
        FingerprintSensorEx.GetParameters(devHandle, 1, paramValue, size);
        fpWidth = byteArrayToInt(paramValue);

        FingerprintSensorEx.GetParameters(devHandle, 2, paramValue, size);
        fpHeight = byteArrayToInt(paramValue);

        imgBuffer = new byte[fpWidth*fpHeight];
        mbStop = false;

        return "Sensor abierto exitosamente en modo ANSI";
    }

    public static void resetLectorState() {
        mbStop = true;
        if (imgBuffer != null) {
            java.util.Arrays.fill(imgBuffer, (byte) 0);
        }
        System.out.println("Estado del lector reiniciado para nueva captura.");
    }

    //Close the device
    public static void CloseDevice() {
        mbStop = true;
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(dbHandle != 0) {
            FingerprintSensorEx.DBFree(dbHandle);
            dbHandle = 0;
        }
        if (devHandle != 0) {
            FingerprintSensorEx.CloseDevice(devHandle);
            devHandle = 0;
        }
        resetLectorState();
        FingerprintSensorEx.Terminate();
    }

//    public static String scanFinger() {
//        // 1. Inicialización (Tu lógica actual) [cite: 269]
//        if (FingerprintSensorEx.Init() != 0) return "Error initializing sensor";
//
//        devHandle = FingerprintSensorEx.OpenDevice(0);
//        if (devHandle == 0) return "Error opening device";
//
//        dbHandle = FingerprintSensorEx.DBInit();
//        if (dbHandle == 0) return "Error initializing database";
//
//        // Configuración de dimensiones [cite: 269]
//        byte[] paramValue = new byte[4];
//        int[] size = new int[]{4};
//        FingerprintSensorEx.GetParameters(devHandle, 1, paramValue, size);
//        fpWidth = byteArrayToInt(paramValue);
//        FingerprintSensorEx.GetParameters(devHandle, 2, paramValue, size);
//        fpHeight = byteArrayToInt(paramValue);
//
//        imgBuffer = new byte[fpWidth * fpHeight];
//        mbStop = false;
//        isCaptured = false;
//        lastBase64Template = "";
//        lastBlobTemplate = "";
//
//        // 2. Hilo de captura corregido
//        new Thread(() -> {
//            while (!mbStop && !isCaptured) {
//                byte[] template = new byte[2048];
//                int[] templateLen = new int[]{2048};
//
//
//                // LLAMADA ÚNICA: Captura imagen y template a la vez
//                int result = FingerprintSensorEx.AcquireFingerprint(devHandle, imgBuffer, template, templateLen);
//
//                if (result == 0) {
//                    // Convertir a Base64 el template obtenido
//                    lastBase64Template = Base64.getEncoder().encodeToString(template);
//                    isCaptured = true; // Detenemos el hilo
//
//                    System.out.println("Huella capturada y convertida a Base64");
//
//                    // Opcional: Guardar imagen [cite: 268]
//                    try {
//                        writeBitmap(imgBuffer, fpWidth, fpHeight, "fingerprint.bmp");
//                    } catch (IOException e) { e.printStackTrace(); }
//                }
//
//                try { Thread.sleep(100); } catch (InterruptedException e) { break; }
//            }
//        }).start();
//
//        // 3. Espera activa (Bloquea hasta que el hilo capture algo)
//        // Nota: En una API real, podrías usar un CompletableFuture o un timeout
//        long startTime = System.currentTimeMillis();
//        while (!isCaptured && (System.currentTimeMillis() - startTime < 10000)) { // 10 seg timeout
//            try { Thread.sleep(200); } catch (InterruptedException e) {}
//        }
//
//        if (isCaptured) {
//            CloseDevice();
//            return lastBase64Template;
//        } else {
//            mbStop = true; // Detener hilo por timeout
//            CloseDevice();
//            return "Timeout: No se detectó huella";
//        }
//    }


    // TODO: ARREGLAR :V
    public static String scanFinger() {
        if (FingerprintSensorEx.Init() != 0) return "Error initializing sensor";

        devHandle = FingerprintSensorEx.OpenDevice(0);
        if (devHandle == 0) return "Error opening device";

        dbHandle = FingerprintSensorEx.DBInit();
        if (dbHandle == 0) return "Error initializing database";

        byte[] paramValue = new byte[4];
        int[] size = new int[]{4};
        FingerprintSensorEx.GetParameters(devHandle, 1, paramValue, size);
        fpWidth = byteArrayToInt(paramValue);
        FingerprintSensorEx.GetParameters(devHandle, 2, paramValue, size);
        fpHeight = byteArrayToInt(paramValue);

        imgBuffer = new byte[fpWidth * fpHeight];
        mbStop = false;
        isCaptured = false;
        lastBase64Template = "";
        lastBlobTemplate = "";

        // 2. Hilo de captura utilizando el archivo físico
        new Thread(() -> {
            while (!mbStop && !isCaptured) {
                byte[] rawTemplate = new byte[2048];
                int[] rawTemplateLen = new int[]{2048};

                int result = FingerprintSensorEx.AcquireFingerprint(devHandle, imgBuffer, rawTemplate, rawTemplateLen);

                if (result == 0) {
                    System.out.println("Huella detectada. Guardando imagen temporal...");

                    String rutaImagenTmp = "fingerprint_tmp.bmp";

                    try {
                        writeBitmap(imgBuffer, fpWidth, fpHeight, rutaImagenTmp);

                        byte[] standardTemplate = new byte[2048];
                        int[] standardTemplateLen = new int[]{2048};

                        // Extraemos la huella desde el archivo guardado
                        int extractResult = FingerprintSensorEx.ExtractFromImage(dbHandle, rutaImagenTmp, 500, standardTemplate, standardTemplateLen);

                        if (extractResult == 0) {
                            byte[] finalTemplate = new byte[standardTemplateLen[0]];
                            System.arraycopy(standardTemplate, 0, finalTemplate, 0, standardTemplateLen[0]);

                            // Convertimos a Base64
                            lastBase64Template = Base64.getEncoder().encodeToString(finalTemplate);
                            isCaptured = true;

                            // =================================================================
                            // PRINT DE LA HUELLA ESCANEADA NUEVA (COMPATIBLE)
                            // =================================================================
                            System.out.println("\n=================================================================");
                            System.out.println("HUELLA ESCANEADA LOCAL (BASE64 ESTÁNDAR):");
                            System.out.println(lastBase64Template);
                            System.out.println("=================================================================\n");

                        } else {
                            System.out.println("Error en ExtractFromImage. Código de error: " + extractResult);
                        }

                    } catch (IOException e) {
                        System.out.println("Error al escribir el archivo de mapa de bits: " + e.getMessage());
                    }
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();

        long startTime = System.currentTimeMillis();
        while (!isCaptured && (System.currentTimeMillis() - startTime < 10000)) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
        }

        if (isCaptured) {
            CloseDevice();
            return lastBase64Template;
        } else {
            mbStop = true;
            CloseDevice();
            return "Timeout: No se detectó huella";
        }
    }

    public static synchronized String verifyFinger(JSONArray huellasArray) {
        if (devHandle == 0 || imgBuffer == null) {
            openDevice();
        }
        try {
            System.out.println("Sensor activo: Coloque el dedo...");
            long timeout = System.currentTimeMillis() + 15000;

            while (System.currentTimeMillis() < timeout) {
                byte[] currentTemplate = new byte[2048];
                int[] templateLen = new int[]{2048};
                int result = FingerprintSensorEx.AcquireFingerprint(devHandle, imgBuffer, currentTemplate, templateLen);

                if (result == 0) {
                    for (int i = 0; i < huellasArray.length(); i++) {
                        JSONObject item = huellasArray.getJSONObject(i);
                        byte[] dbTemplate = Base64.getDecoder().decode(item.getString("huella").trim());
                        int score = FingerprintSensorEx.DBMatch(dbHandle, dbTemplate, currentTemplate);

                        if (score > 80) return "MATCH_OK";
                    }
                    return "NOT_MATCH";
                }
                Thread.sleep(150);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "";
    }



//    public static synchronized String verifyFinger(JSONArray huellasArray) {
//        if (devHandle == 0 || imgBuffer == null) {
//            openDevice();
//        }
//
//        try {
//            System.out.println("Sensor activo (SourceAFIS - Java 21): Coloque el dedo...");
//            long timeout = System.currentTimeMillis() + 15000;
//
//            // Captura nativa estándar (ZKFinger V10) para el buffer de imagen
//            byte[] formatParam = intToByteArray(1);
//            FingerprintSensorEx.SetParameters(devHandle, 10001, formatParam, 4);
//
//            byte[] currentTemplate = new byte[2048];
//            int[] templateLen = new int[]{2048};
//
//            while (System.currentTimeMillis() < timeout) {
//                templateLen[0] = 2048;
//                int result = FingerprintSensorEx.AcquireFingerprint(devHandle, imgBuffer, currentTemplate, templateLen);
//
//                if (result == 0) {
//                    System.out.println("Huella detectada. Generando bitmap para análisis de SourceAFIS...");
//
//                    // 1. Guardar la imagen en vivo a un archivo temporal .bmp usando tu método nativo
//                    String rutaImagenTmp = "fingerprint_verify_tmp.bmp";
//                    writeBitmap(imgBuffer, fpWidth, fpHeight, rutaImagenTmp);
//
//                    // 2. Leer los bytes de la imagen generada
//                    byte[] imageBytes = Files.readAllBytes(new File(rutaImagenTmp).toPath());
//
//                    // 3. Crear el Template en la versión moderna (3.18.1)
//                    FingerprintTemplate probe = new FingerprintTemplate(
//                            new FingerprintImage()
//                                    .dpi(500) // Configuración de resolución nativa de los lentes de ZK
//                                    .decode(imageBytes)
//                    );
//
//                    FingerprintMatcher matcher = new FingerprintMatcher(probe);
//
//                    // 4. Bucle iterativo sobre los registros que te envía NestJS
//                    for (int i = 0; i < huellasArray.length(); i++) {
//                        JSONObject item = huellasArray.getJSONObject(i);
//                        if (!item.has("huella") || item.getString("huella").trim().isEmpty()) continue;
//
//                        String huellaBase64 = item.getString("huella").trim();
//
//                        // Seguimos ignorando de forma segura los viejos strings propietarios encriptados
////                        if (huellaBase64.startsWith("TWFT") || huellaBase64.startsWith("TWNT")) {
////                            System.out.println("⚠️ Registro [" + i + "] omitido: Formato propietario cerrado incompatible.");
////                            continue;
////                        }
//
//                        try {
//                            // Decodificar la huella ANSI limpia de tu API
//                            byte[] rawDbTemplate = Base64.getDecoder().decode(huellaBase64);
//
//                            // =================================================================
//                            // SOLUCIÓN AL ERROR DE TEMPLATE:
//                            // Importamos explícitamente los bytes leyéndolos como formato ANSI-378
//                            // =================================================================
//                            FingerprintTemplate candidate = new FingerprintTemplate()
//                                    .convert(rawDbTemplate);
//
//                            // Comparación matemática ejecutada puramente en la JVM
//                            double score = matcher.match(candidate);
//                            System.out.println("-> Registro ANSI [" + i + "] analizado con éxito - Score: " + score);
//
//                            // El umbral estándar para dar un match seguro en SourceAFIS es de 40.0
//                            if (score > 40.0) {
//                                new File(rutaImagenTmp).delete(); // Eliminar temporal
//                                return "MATCH_OK";
//                            }
//                        } catch (Exception e) {
//                            // Aquí es donde caía antes, ahora te mostrará si hay algún byte corrupto pero de forma segura
//                            System.out.println("❌ Error analítico en índice " + i + ": " + e.getMessage());
//                        }
//                    }
//
//                    new File(rutaImagenTmp).delete(); // Eliminar temporal si no hubo match
//                    return "NOT_MATCH";
//                }
//                Thread.sleep(150);
//            }
//            return "TIMEOUT_NO_FINGER";
//
//        } catch (Exception e) {
//            System.out.println("Error crítico en verifyFinger: " + e.getMessage());
//            return "ERROR: " + e.getMessage();
//        }
//    }

    // Método auxiliar interno para normalizar los tamaños exactos de los arrays pasados a la DLL
    private static byte[] dbTemplateFixedSize(byte[] source, int size) {
        byte[] fixed = new byte[size];
        System.arraycopy(source, 0, fixed, 0, Math.min(source.length, size));
        return fixed;
    }
}