package com.f6.functions;

import com.zkteco.biometric.FingerprintSensorErrorCode;
import com.zkteco.biometric.FingerprintSensorEx;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

        //Verificar si el lector se inicio
        if(FingerprintSensorErrorCode.ZKFP_ERR_OK != FingerprintSensorEx.Init()) {
            Lector.CloseDevice();
            return "Error initializing sensor";
        }


        //Verificar si el lector esta conectado
        int ret = FingerprintSensorEx.GetDeviceCount();
        if(ret < 0) {
            Lector.CloseDevice();
            return "No fingerprint sensor found";
        }


        //Verifica si el lector se abrio con exito
        if((devHandle = FingerprintSensorEx.OpenDevice(0)) == 0){
            Lector.CloseDevice();
            return "Error opening device";
        }


        //Verifica si la base de datos se inicializo
        if((dbHandle = FingerprintSensorEx.DBInit()) == 0) {
            Lector.CloseDevice();
            return "Error initializing database";
        }

        FingerprintSensorEx.DBSetParameter(dbHandle,  5010, 1);

        size[0] = 4;
        FingerprintSensorEx.GetParameters(devHandle, 1, paramValue, size);
        fpWidth = byteArrayToInt(paramValue);

        FingerprintSensorEx.GetParameters(devHandle, 2, paramValue, size);
        fpHeight = byteArrayToInt(paramValue);

        imgBuffer = new byte[fpWidth*fpHeight];

        mbStop = false;
//        workThread = new WorkThread();

        return "Hola mundo";
    }

    //Close the device
    public static void CloseDevice() {
        mbStop = true;
        try { //Tiempo de espera del hilo de escaneado
            Thread.sleep(1000);
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
        FingerprintSensorEx.Terminate();
    }

    public static String scanFinger() {
        // 1. Inicialización (Tu lógica actual) [cite: 269]
        if (FingerprintSensorEx.Init() != 0) return "Error initializing sensor";

        devHandle = FingerprintSensorEx.OpenDevice(0);
        if (devHandle == 0) return "Error opening device";

        dbHandle = FingerprintSensorEx.DBInit();
        if (dbHandle == 0) return "Error initializing database";

        // Configuración de dimensiones [cite: 269]
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

        // 2. Hilo de captura corregido
        new Thread(() -> {
            while (!mbStop && !isCaptured) {
                byte[] template = new byte[2048];
                int[] templateLen = new int[]{2048};

                // LLAMADA ÚNICA: Captura imagen y template a la vez
                int result = FingerprintSensorEx.AcquireFingerprint(devHandle, imgBuffer, template, templateLen);

                if (result == 0) {
                    // Convertir a Base64 el template obtenido
                    lastBase64Template = Base64.getEncoder().encodeToString(template);
                    isCaptured = true; // Detenemos el hilo

                    System.out.println("Huella capturada y convertida a Base64");

                    // Opcional: Guardar imagen [cite: 268]
                    try {
                        writeBitmap(imgBuffer, fpWidth, fpHeight, "fingerprint.bmp");
                    } catch (IOException e) { e.printStackTrace(); }
                }

                try { Thread.sleep(100); } catch (InterruptedException e) { break; }
            }
        }).start();

        // 3. Espera activa (Bloquea hasta que el hilo capture algo)
        // Nota: En una API real, podrías usar un CompletableFuture o un timeout
        long startTime = System.currentTimeMillis();
        while (!isCaptured && (System.currentTimeMillis() - startTime < 10000)) { // 10 seg timeout
            try { Thread.sleep(200); } catch (InterruptedException e) {}
        }

        if (isCaptured) {
            CloseDevice();
            return lastBase64Template;
        } else {
            mbStop = true; // Detener hilo por timeout
            CloseDevice();
            return "Timeout: No se detectó huella";
        }
    }

    // En Lector.java

    public static String verifyFinger(JSONArray huellasArray) {
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
            return "TIMEOUT_NO_FINGER";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }


//    public void compare(){
//        FingerprintSensorEx.
//    }
}
