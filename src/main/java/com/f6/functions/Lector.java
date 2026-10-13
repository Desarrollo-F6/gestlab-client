package com.f6.functions;

import com.zkteco.biometric.FingerprintSensorErrorCode;
import com.zkteco.biometric.FingerprintSensorEx;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.Base64;


public class Lector {


    public static long devHandle = 0, dbHandle = 0;



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

    // Funcion para resetear el estado del lector
    public static void resetLectorState() {
        mbStop = true;
        isCaptured = false;
        lastBase64Template = "";
        lastBlobTemplate = "";
        if (imgBuffer != null) {
            java.util.Arrays.fill(imgBuffer, (byte) 0);
        }
        System.out.println("Estado del lector y búfer interno reiniciados.");
    }

    // Funcion para cerrar el dispositivo
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


    // TODO: ARREGLAR :V
    // Funcion para escanear la huella
    public static String scanFinger() {
        // 1. Aseguramos que las variables de control comiencen limpias ANTES de abrir el dispositivo
        mbStop = true;
        isCaptured = false;
        lastBase64Template = "";
        lastBlobTemplate = "";

        if (FingerprintSensorEx.Init() != 0) return "Error initializing sensor";

        devHandle = FingerprintSensorEx.OpenDevice(0);
        if (devHandle == 0) {
            CloseDevice();
            return "Error opening device";
        }

        dbHandle = FingerprintSensorEx.DBInit();
        if (dbHandle == 0) {
            CloseDevice();
            return "Error initializing database";
        }

        byte[] paramValue = new byte[4];
        int[] size = new int[]{4};
        FingerprintSensorEx.GetParameters(devHandle, 1, paramValue, size);
        fpWidth = byteArrayToInt(paramValue);
        FingerprintSensorEx.GetParameters(devHandle, 2, paramValue, size);
        fpHeight = byteArrayToInt(paramValue);

        imgBuffer = new byte[fpWidth * fpHeight];
        mbStop = false; // Activamos el ciclo del hilo

        new Thread(() -> {
            while (!mbStop && !isCaptured) {
                byte[] rawTemplate = new byte[2048];
                int[] rawTemplateLen = new int[]{2048};

                int result = FingerprintSensorEx.AcquireFingerprint(devHandle, imgBuffer, rawTemplate, rawTemplateLen);

                if (result == 0) {
                    System.out.println("Huella detectada. Generando imagen temporal...");
                    String rutaImagenTmp = "fingerprint_tmp.bmp";

                    try {
                        writeBitmap(imgBuffer, fpWidth, fpHeight, rutaImagenTmp);

                        byte[] standardTemplate = new byte[2048];
                        int[] standardTemplateLen = new int[]{2048};

                        int extractResult = FingerprintSensorEx.ExtractFromImage(dbHandle, rutaImagenTmp, 500, standardTemplate, standardTemplateLen);

                        if (extractResult == 0) {
                            byte[] finalTemplate = new byte[standardTemplateLen[0]];
                            System.arraycopy(standardTemplate, 0, finalTemplate, 0, standardTemplateLen[0]);

                            // Asignación directa y segura
                            lastBase64Template = Base64.getEncoder().encodeToString(finalTemplate);
                            isCaptured = true;
                            System.out.println("Template generado y codificado correctamente.");
                        } else {
                            System.out.println("Error en ExtractFromImage: " + extractResult);
                        }
                    } catch (IOException e) {
                        System.out.println("Error al procesar bitmap: " + e.getMessage());
                    } finally {
                        File tmpFile = new File(rutaImagenTmp);
                        if (tmpFile.exists()) tmpFile.delete();
                    }
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();

        // Bucle de espera activa
        long startTime = System.currentTimeMillis();
        while (!isCaptured && (System.currentTimeMillis() - startTime < 12000)) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }

        // Capturamos el resultado antes de cerrar el dispositivo
        String resultadoFinal;
        if (isCaptured && !lastBase64Template.isEmpty()) {
            resultadoFinal = lastBase64Template;
        } else {
            resultadoFinal = "Timeout: No se detectó huella";
        }

        CloseDevice(); // Cierra y limpia de forma segura al terminar
        return resultadoFinal;
    }

    // Funcion para verificar la huella
    public static synchronized String verifyFinger(JSONArray huellasArray) {
        // Limpieza previa
        resetLectorState();

        if (devHandle == 0 || imgBuffer == null) {
            String openRes = openDevice();
            if (!openRes.contains("exitosamente")) {
                CloseDevice();
                return "ERROR: " + openRes;
            }
        }

        String resultadoVerificacion = "";
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
                        if (!item.has("huella")) continue;

                        byte[] dbTemplate = Base64.getDecoder().decode(item.getString("huella").trim());
                        int score = FingerprintSensorEx.DBMatch(dbHandle, dbTemplate, currentTemplate);

                        if (score > 80) {
                            resultadoVerificacion = "MATCH_OK";
                            break;
                        }
                    }

                    if (resultadoVerificacion.isEmpty()) {
                        resultadoVerificacion = "NOT_MATCH";
                    }
                    break; // Rompemos el while apenas procesa una huella válida/inválida
                }
                Thread.sleep(150);
            }

            if (resultadoVerificacion.isEmpty()) {
                resultadoVerificacion = "TIMEOUT_NO_FINGER";
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resultadoVerificacion = "ERROR: Hilo interrumpido";
        } finally {
            // --- AQUÍ ESTÁ LA CLAVE ---
            // Cerramos el dispositivo obligatoriamente sin importar si hubo match o no
            CloseDevice();
        }

        return resultadoVerificacion;
    }


    // Método auxiliar interno para normalizar los tamaños exactos de los arrays pasados a la DLL
    private static byte[] dbTemplateFixedSize(byte[] source, int size) {
        byte[] fixed = new byte[size];
        System.arraycopy(source, 0, fixed, 0, Math.min(source.length, size));
        return fixed;
    }
}