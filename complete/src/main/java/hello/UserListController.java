package hello;

import com.google.api.core.ApiFuture;
import com.google.capillary.EncrypterManager;
import com.google.capillary.RsaEcdsaEncrypterManager;
import com.google.capillary.demo.common.Constants;
import com.google.capillary.demo.common.KeyAlgorithm;
import com.google.capillary.demo.common.SecureNotification;
import com.google.capillary.demo.common.SendMessageRequest;
import com.google.cloud.firestore.Blob;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.storage.Bucket;
import com.google.crypto.tink.subtle.Base64;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

import jdk.nashorn.internal.parser.JSONParser;

@Controller
public class UserListController {

    private static List<QueryDocumentSnapshot> mUserData;
    private static FcmSender sFcmSender;

    private static final String FIREBASE_PROJECT_ID_OPTION = "alarm-app-a3ee2";
    private static final String SERVICE_ACCOUNT_CREDENTIALS_PATH_OPTION = "library/resources/firebase/service-account.json";

    @GetMapping("/userlist")
    public String userlist(@RequestParam(name = "email", required = false, defaultValue = "World") String email, Model model) throws ExecutionException, InterruptedException {
        return getDataFromFireStore(model);
    }

    @PostMapping(path = "/userlist", consumes = "application/json", produces = "application/json")
    public void setActivateForUser(@RequestBody String object) throws ExecutionException, InterruptedException, FileNotFoundException {
        try {
            JSONObject jsonObject = new JSONObject(object);
            System.out.println("TVT go to testActivated function, object = " + jsonObject.toString());
            switch (jsonObject.get("type").toString()) {
                case "activated":
                    handleActivatedForUser(jsonObject.get("value").toString());
                    break;
                case "sendtext":
                    System.out.println("TVT go to sendText, email = " + jsonObject.get("value").toString());
                    sendEcryptTextToFirebase(jsonObject.get("value").toString());
                    break;
                case "sendimage":
                    System.out.println("TVT go to sendImage, email = " + jsonObject.get("value").toString());
                    encryptImage(jsonObject.get("value").toString());
                    break;
                default:
            }
        } catch (JSONException err) {

        }
    }

    private static String getDataFromFireStore(Model model) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> query = db.collection("users").get();
        QuerySnapshot querySnapshot = query.get();
        mUserData = querySnapshot.getDocuments();
        model.addAttribute("users", mUserData);

        return "userlist";
    }

    private static void handleActivatedForUser(String email) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference users = db.collection("users");
        Query query = users.whereEqualTo("email", email);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            db.collection("users").document(document.getId()).update("activated", true);
        }
    }

    private void sendEcryptTextToFirebase(String email) {
        for (QueryDocumentSnapshot document : mUserData) {
            if (document.getData().get("email").equals(email)) {
                ArrayList<Map<String, Object>> arr = (ArrayList<Map<String, Object>>) document.getData().get("fcmId");
                for (Map<String, Object> item : arr) {
                    System.out.println("TVT type device = " + item.get("device"));
                    // Encrypt data follow type of device
                    if (item.get("device").equals("android")) {
                        Blob blob = (Blob) item.get("publicKey");
                        byte[] publicKeyByte = blob.toBytes();
                        handleEncryptText(publicKeyByte, item.get("appId"), item.get("tokenId"));
                    } else {
                        try {
                            String publickeyStr = (String) item.get("publicKey");
                            handleEncryptTextIOS(publickeyStr, item.get("appId"), item.get("tokenId"));
                        } catch (IllegalBlockSizeException e) {
                            e.printStackTrace();
                        } catch (InvalidKeyException e) {
                            e.printStackTrace();
                        } catch (NoSuchPaddingException e) {
                            e.printStackTrace();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (BadPaddingException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            }
        }
    }

    private static void handleEncryptText(byte[] publicKey, Object appId, Object tokenId) {
        String appIdStr = (String) appId;
        String tokenIdStr = (String) tokenId;
        RsaEcdsaEncrypterManager rsaEcdsaEncrypterManager;
        try (FileInputStream senderSigningKey = new FileInputStream("../../resources/ecdsa/sender_signing_key.dat")) {
            rsaEcdsaEncrypterManager = new RsaEcdsaEncrypterManager(senderSigningKey);

            EncrypterManager encrypterManager = rsaEcdsaEncrypterManager;
            encrypterManager.loadPublicKey(publicKey);
            byte[] ciphertext = encrypterManager.encrypt(getMessageContent(appIdStr).getData().toByteArray());
            encrypterManager.clearPublicKey();
            String ciphertextString = Base64.encode(ciphertext);
            System.out.println("TVT cipherTextString = " + ciphertextString);

            Map<String, String> dataMap = new HashMap<>();
            dataMap.put(Constants.CAPILLARY_CIPHERTEXT_KEY, ciphertextString);
            dataMap.put(Constants.CAPILLARY_KEY_ALGORITHM_KEY, KeyAlgorithm.RSA_ECDSA.name());

            Map<String, String> dataMapIos = new HashMap<>();
            dataMapIos.put("body", ciphertextString);
            dataMapIos.put("title", KeyAlgorithm.RSA_ECDSA.name());

            try {
                sFcmSender = new FcmSender(FIREBASE_PROJECT_ID_OPTION, SERVICE_ACCOUNT_CREDENTIALS_PATH_OPTION);
                sFcmSender.sendDataMessage(tokenIdStr, dataMap, dataMapIos);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private void handleEncryptTextIOS(String publicKeyStr, Object appId, Object tokenId) throws IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {
        String tokenIdStr = (String) tokenId;

        System.out.println("TVT publickeyStr = " + publicKeyStr);
        String ciphertextString = java.util.Base64.getEncoder().encodeToString(RSAUtil.encrypt("Dhiraj is the author nguyen dinh lich dai hoc bach khoa ha noi ta quang buu bach khoa hai ba trung ha noi nguyen dinh lih nguyen dinh lich ", publicKeyStr));
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put(Constants.CAPILLARY_CIPHERTEXT_KEY, ciphertextString);
        dataMap.put(Constants.CAPILLARY_KEY_ALGORITHM_KEY, KeyAlgorithm.RSA_ECDSA.name());

        Map<String, String> dataMapIos = new HashMap<>();
        dataMapIos.put("body", ciphertextString);
        dataMapIos.put("title", KeyAlgorithm.RSA_ECDSA.name());

        try {
            sFcmSender = new FcmSender(FIREBASE_PROJECT_ID_OPTION, SERVICE_ACCOUNT_CREDENTIALS_PATH_OPTION);
            sFcmSender.sendDataMessage(tokenIdStr, dataMap, dataMapIos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static SendMessageRequest getMessageContent(String appId) {
        SendMessageRequest request = SendMessageRequest.newBuilder()
                .setUserId(appId)
                .setKeyAlgorithm(KeyAlgorithm.RSA_ECDSA)
                .setIsAuthKey(false)
                .setDelaySeconds(1)
                .setData(createSecureMessageBytes("Demo cipherText: Recorder - Firebase - AlarmApp", KeyAlgorithm.RSA_ECDSA, false)).build();
        return request;
    }

    private static ByteString createSecureMessageBytes(
            String title, KeyAlgorithm keyAlgorithm, boolean isAuthKey) {
        return SecureNotification.newBuilder()
                .setId((int) System.currentTimeMillis())
                .setTitle(title)
                .setBody(String.format("Algorithm=%s, IsAuth=%s", keyAlgorithm, isAuthKey))
                .build().toByteString();
    }

    private void encryptImage(String email) throws FileNotFoundException {
        File imageFile = new File("library/resources/imagetest.jpg");

        Key key = genImageKey();
        byte[] imageByte = getByteFromImage(imageFile);
        Cipher cipher;
        byte[] encrypted = null;
        try {
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            encrypted = cipher.doFinal(imageByte);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("TVT encrypted = " + encrypted);

        FileOutputStream fos = null;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");
            String string = dateFormat.format(new Date());
            String fileName = string + ".jpg";
            String filePath = "../../resources/decrypt_images/" + fileName;
            fos = new FileOutputStream(filePath);
            fos.write(encrypted);
            fos.close();

            // Upload encrypt image to cloud storage
            String keyString = java.util.Base64.getEncoder().encodeToString(key.getEncoded());

            sendEncryptImageToFirebase(email, keyString, filePath, fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Key genImageKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            Key key = keyGenerator.generateKey();
            System.out.println("TVT convert key to string = " + key.toString());
            return key;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] getByteFromImage(File imageFile) {
        InputStream is = null;
        try {
            is = new FileInputStream(imageFile);
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
        }
        byte[] content = null;
        try {
            content = new byte[is.available()];
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            is.read(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    private void sendEncryptImageToFirebase(String email, String keyString, String filePath, String fileName) throws FileNotFoundException {
        for (QueryDocumentSnapshot document : mUserData) {
            if (document.getData().get("email").equals(email)) {
                ArrayList<Map<String, Object>> arr = (ArrayList<Map<String, Object>>) document.getData().get("fcmId");
                if (arr != null) {
                    for (Map<String, Object> item : arr) {
                        System.out.println("TVT type device = " + item.get("device"));
                        Blob blob = (Blob) item.get("publicKey");
                        byte[] publicKeyByte = blob.toBytes();

                        // Encrypt data follow type of device
                        if (item.get("device").equals("android")) {
                            handEncryptImage(publicKeyByte, item.get("appId"), item.get("tokenId"), document.getId(), keyString, filePath, fileName);
                        } else {

                        }
                    }
                }
                break;
            }
        }
    }

    private static void handEncryptImage(byte[] publicKey, Object appId, Object tokenId, String userId, String imageKeyStr, String filePath, String fileName) throws FileNotFoundException {
        String imageLocation = userId + "/" + fileName;
        Bucket bucket = StorageClient.getInstance().bucket();
        bucket.create(imageLocation, new FileInputStream(filePath), "image/jpeg");

        String appIdStr = (String) appId;
        String tokenIdStr = (String) tokenId;
        RsaEcdsaEncrypterManager rsaEcdsaEncrypterManager;

        try (FileInputStream senderSigningKey = new FileInputStream("../../resources/ecdsa/sender_signing_key.dat")) {
            rsaEcdsaEncrypterManager = new RsaEcdsaEncrypterManager(senderSigningKey);

            EncrypterManager encrypterManager = rsaEcdsaEncrypterManager;
            encrypterManager.loadPublicKey(publicKey);

            byte[] ciphertext = encrypterManager.encrypt(getMessageContentImage(appIdStr, "gs://alarm-app-a3ee2.appspot.com/" + imageLocation, imageKeyStr).getData().toByteArray());
            encrypterManager.clearPublicKey();
            String ciphertextString = Base64.encode(ciphertext);
            System.out.println("TVT cipherTextString = " + ciphertextString);

            Map<String, String> dataMap = new HashMap<>();
            dataMap.put(Constants.CAPILLARY_CIPHERTEXT_KEY, ciphertextString);
            dataMap.put(Constants.CAPILLARY_KEY_ALGORITHM_KEY, KeyAlgorithm.RSA_ECDSA.name());

            Map<String, String> dataMapIos = new HashMap<>();
            dataMapIos.put("body", ciphertextString);
            dataMapIos.put("title", KeyAlgorithm.RSA_ECDSA.name());

            try {
                sFcmSender = new FcmSender(FIREBASE_PROJECT_ID_OPTION, SERVICE_ACCOUNT_CREDENTIALS_PATH_OPTION);
                sFcmSender.sendDataMessage(tokenIdStr, dataMap, dataMapIos);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private static SendMessageRequest getMessageContentImage(String appId, String imageLocation, String imageKeyStr) {
        SendMessageRequest request = SendMessageRequest.newBuilder()
                .setUserId(appId)
                .setKeyAlgorithm(KeyAlgorithm.RSA_ECDSA)
                .setIsAuthKey(false)
                .setDelaySeconds(1)
                .setData(createSecureMessageBytesImage(imageLocation, imageKeyStr))
                .build();
        return request;
    }

    private static ByteString createSecureMessageBytesImage(String imageLocation, String imageKeyStr) {
        return SecureNotification.newBuilder()
                .setId((int) System.currentTimeMillis())
                .setTitle(imageLocation)
                .setBody(imageKeyStr)
                .build().toByteString();
    }
}
