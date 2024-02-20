package com.uid2.cstg;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Cstg {

    public static final String CLIENT_SIDE_TOKEN_GENERATE_SERVER_PUBLIC_KEY = "UID2-X-L-MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEtXJdTSZAYHvoRDWiehMHoWF1BNPuqLs5w2ZHiAZ1IJc7O4/z0ojPTB0V+KYX/wxQK0hxx6kxCvHj335eI/ZQsQ==";
    public static final String CLIENT_SIDE_TOKEN_GENERATE_SUBSCRIPTION_ID = "4WvryDGbR5";
    public static final String BASE_URL = "http://localhost:8080/";
    public static final String APP_NAME = "" ;
    public static final int PUBLIC_KEY_PREFIX_LENGTH = 9;
    public static final int AUTHENTICATION_TAG_LENGTH_BITS = 128;
    public static final int IV_BYTES = 12;
    public static final String CSTG_REQUEST = "{\"email_hash\":\"eVvLS/Vg+YZ6+z3i0NOpSXYyQAfEXqCZ7BTpAjFUBUc=\"}";

    // cloned into com.uid2.UID2Client.cstg method
//    public static String getV2ClientSideTokenGenerateEnvelope()  throws Exception {
//
//        final byte[] serverPublicKeyBytes = base64ToByteArray(CLIENT_SIDE_TOKEN_GENERATE_SERVER_PUBLIC_KEY.substring(PUBLIC_KEY_PREFIX_LENGTH));
//
//        final PublicKey serverPublicKey = KeyFactory.getInstance("EC")
//            .generatePublic(new X509EncodedKeySpec(serverPublicKeyBytes));
//
//        final KeyPair keyPair = generateKeyPair();
//        final SecretKey sharedSecret = generateSharedSecret(serverPublicKey, keyPair);
//
//        final JsonObject cstgEnvelope = createCstgEnvelope(CSTG_REQUEST, CLIENT_SIDE_TOKEN_GENERATE_SUBSCRIPTION_ID, keyPair.getPublic(), sharedSecret);
//
//        return cstgEnvelope.toString();
//
//        final Request.Builder requestBuilder = new Request.Builder()
//            .url(BASE_URL + "/v2/token/client-generate")
//            .addHeader("Origin", APP_NAME)
//            .post(RequestBody.create(cstgEnvelope.toString(), HttpClient.JSON));
//        final String encryptedResponse = HttpClient.execute(requestBuilder.build(), HttpClient.HttpMethod.POST);
//        return v2DecryptResponseWithoutNonce(encryptedResponse, sharedSecret.getEncoded());
//    }

    private static final int GCM_TAG_LENGTH_BYTES = 16;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    public static JsonNode v2DecryptResponseWithoutNonce(String response, byte[] secretBytes) throws Exception {
        final byte[] responseBytes = Base64.getDecoder().decode(response);
        byte[] payload;

        try {
            final SecretKey key = new SecretKeySpec(secretBytes, "AES");
            final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BYTES * 8, responseBytes, 0, GCM_IV_LENGTH_BYTES);
            final Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
            payload = c.doFinal(responseBytes, GCM_IV_LENGTH_BYTES, responseBytes.length - GCM_IV_LENGTH_BYTES);
        } catch (Exception e) {
            throw new RuntimeException("Unable to Decrypt", e);
        }
        byte[] resultBytes = payload;

        //example of resultStr
        /*
        {
            "body" : {
            "advertising_token" : "AgAAAGU....dqzWQ==",
                "refresh_token" : "AAAAAG....794RHvjV",
                "identity_expires" : 1708410149086,
                "refresh_expires" : 1708492949086,
                "refresh_from" : 1708407449086,
                "refresh_response_key" : "FkwHuwv....8YBOX5ifyR8="
        },
            "status" : "success"
        }
        */
        final String resultStr = new String(resultBytes, StandardCharsets.UTF_8);
        return Mapper.OBJECT_MAPPER.readTree(resultStr);
    }

    public static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static JsonObject createCstgEnvelope(String request, String subscriptionId, PublicKey clientPublicKey, SecretKey sharedSecret, String appName) {
        final long now = Clock.systemUTC().millis();

        final byte[] iv = new byte[IV_BYTES];
        SECURE_RANDOM.nextBytes(iv);

        final JsonArray aad = new JsonArray();
        aad.add(now);

        final byte[] payload = encryptCSTG(request.getBytes(StandardCharsets.UTF_8),
            iv,
            aad.toString().getBytes(StandardCharsets.UTF_8),
            sharedSecret);

        final JsonObject body = new JsonObject();

        body.addProperty("payload", byteArrayToBase64(payload));
        body.addProperty("iv", byteArrayToBase64(iv));
        body.addProperty("public_key", byteArrayToBase64(clientPublicKey.getEncoded()));
        body.addProperty("timestamp", now);
        body.addProperty("subscription_id", subscriptionId);
        body.addProperty("app_name", appName);

        return body;
    }

    public static byte[] encryptCSTG(byte[] plaintext, byte[] iv, byte[] aad, SecretKey key) {
        final Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AUTHENTICATION_TAG_LENGTH_BITS, iv);
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }

        cipher.updateAAD(aad);

        try {
            return cipher.doFinal(plaintext);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public static SecretKey generateSharedSecret(PublicKey serverPublicKey, KeyPair clientKeypair) {
        try {
            final KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(clientKeypair.getPrivate());
            ka.doPhase(serverPublicKey, true);
            return new SecretKeySpec(ka.generateSecret(), "AES");
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyPair generateKeyPair() {
        final KeyPairGenerator keyPairGenerator;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        final ECGenParameterSpec ecParameterSpec = new ECGenParameterSpec("secp256r1");
        try {
            keyPairGenerator.initialize(ecParameterSpec);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
        return keyPairGenerator.genKeyPair();
    }

    public static byte[] base64ToByteArray(String str) {
        return Base64.getDecoder().decode(str);
    }

    public static String byteArrayToBase64(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }

}
