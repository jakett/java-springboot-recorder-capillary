package hello;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RSAUtil {

    private static String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1h6NUjwuUnrBgkH3CiULGZbElXfLPdc/AI3k5bv5AcxkCkZ3kbvHePtQpgyggBCf4vNxxnnIog5iogR5ZdJ4y54JJ5Z79NjMAbY1P7wamf2Q+Pi8qHQZqF5jwHxHQ1/w5UF8pvIBdkLJ0XbC3qWteZEGOeCS88KnwBtzNu+tY3NIZTKhcFVUnRX7EBBy2QmdEWyGKYpLiD867SLTBsXDyFxFeQUHAlSBXXwW/GmxhrOYnJOOc34zUcOy4z+ZwJ2zdqhmYx2RsKBvIobWnkA2oCb8VzYZJha8sHDe+ud7S36+e4vfXhKIJM9p8+lDThaVu46E2eR3Wntlsm81dmDxhQIDAQAB";
    private static String privateKey = "MIIEowIBAAKCAQEApjiFvsEwRBtlPOnNjHL0yO7MRPBrT0RMHLgTiDkJBINkE5I4eiBD3H5lmSPIbkF61I2neKwvPDsBc/OnwIfHGSnjl+NiPfYZlckwA0lES3SFWN1U/sdHzL7T+zcmn15PFRXcd6HHmnm25QJ3ZMGBl7P041nSBoO/GXYgzHzqNjtmtuVdlsCuYYaaXdXEaYHy/qr3Y/cGjpKehk9jBsnNzSOxEA03XMVXoUX/54BfJ2MdfVByQ7ClwXK5+wpDSKyi1jJPIhhlOCZjIHszBMNNWsMO7Qh+0PrsKtHucKD9dAkFlsOF6QIk6TRSSODL+AoTnmvr2VafPaj75MbOcoVXeQIDAQABAoIBAAivVNuj6/HvDYHahH0lZ4FVKtKiTjUQ0DpC6vBX7I7yD4lPP0iwRWNwrj3LZxZNsQ2IjjrJolkvCxMkJLx0S7SWoffmb1Rl9q3DGpFdY2Ze4vXC8MU0I94AjYJnfgrfKlojb4bHqu55FBOQpqzayHrk8fNsFsN04EgEClAs6a7mI3o0r+3+wS9qCHjzgiwN9COGjAvNJ4OYa1/OznB3wp8CJ/m3LdZDIs+lnlA8B4sbU2EavfNecr6cCoJRQfXCoVz4L795PCwUBi8Ixib5D3aGCAvTJAx5gtnb/mAWqP3BDL23IFccEvYIvWXoOGCyVCaAkRBML8mv75jtL5h52QECgYEA5OHi0vb1qw93HJTaaVUGULahsZ+ThWhRkm30C27tqbHgK/9v1y1Rme1hqQL5D/wLrUYRutf8OpyTy68Ko3qlmG7MhjvBiCT/1dehPQQeE9cBrAV7lnjM2Y9H5cKgxaDwgdZajUCGvS1CeMv82CdaIKGf013TsD65fcZwaLo1U4ECgYEAueoUh7Hj24Q1cOPXVJvFpsTCyU00cAd57IKnPk2zbVVZ1k1deEzVzZsKCmLK8nSivgdG3H5r1f3hXtKV6Qgg76HNA2gpsnTUaE+aC4112Yi6OQfLt1lHOVyyEctcTICc3Ek7fmzUSmau/+wzWIsDbl/SDYpG1o7232lR91LKn/kCgYAFM0ClDGlefpZ1NsiTlhPzp4Ka94YxhMI9snPWAqoxrdHrDf2rhOMSvTr3zRJ9k3tsb2gdt6SKbF5LRWnXdwTmJrZM4nSuNDD/2ctXCI3qY3stPl6ld48n7kJy7O4cL2DngkhCZ1HoYGvfNLtE6ff5P9LSyZHysioiumx5ZKmrgQKBgAKlj3dDFBBa+HvL8pPUx+KQ6Ij/HWD+6kBwt7rOVmCXF3lSjqMYzO5pZ6IwN91txadY6SxYbGaWf6/e5Z10rStcLVQAFp/gw+lcQMArWmMnbCdFsiPBFJ9/b1WLhJveNN15+WvaVsdcYN9p2G1JRVo76PyiXFHPpToXAMC5oALZAoGBALZ23fbQObcytkflxQO0fCvlk0Bn7mibRQqKRDQszIue168ftFf7I9JKBOboycaf2AE58eWKIN7LORhTCMYmRx7zZXVw4xfhDmoO6vysVc+bxebCCQRPvBGh59/CfJjM0N+6QdPp9fVDqI4o3gj2rHfBYdS/vjIJescNygzkc5w2";

    public static PublicKey getPublicKey(String base64PublicKey) {
        PublicKey publicKey = null;
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKey.getBytes()));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(keySpec);
            return publicKey;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return publicKey;
    }

    public static PrivateKey getPrivateKey(String base64PrivateKey) {
        PrivateKey privateKey = null;
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64PrivateKey.getBytes()));
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return privateKey;
    }

    public static byte[] encrypt(String data, String publicKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(publicKey));
        return cipher.doFinal(data.getBytes());
    }

    public static String decrypt(byte[] data, PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(data));
    }

    public static String decrypt(String data, String base64PrivateKey) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        return decrypt(Base64.getDecoder().decode(data.getBytes()), getPrivateKey(base64PrivateKey));
    }

//    public static void main(String[] args) throws IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, BadPaddingException {
//        try {
//            String encryptedString = Base64.getEncoder().encodeToString(encrypt("Dhiraj is the author nguyen dinh lich dai hoc bach khoa ha noi ta quang buu bach khoa hai ba trung ha noi nguyen dinh lih nguyen dinh lich ", publicKey));
//            System.out.println(encryptedString);
//            //String decryptedString = RSAUtil.decrypt(encryptedString, privateKey);
//            //System.out.println(decryptedString);
//        } catch (NoSuchAlgorithmException e) {
//            System.err.println(e.getMessage());
//        }
//
//    }
}
