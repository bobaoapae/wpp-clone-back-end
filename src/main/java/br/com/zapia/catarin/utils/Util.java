package br.com.zapia.catarin.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;

public class Util {

    public static String encodePassword(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(password);
    }

    public static String generate(int keyLen) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(keyLen);
            SecretKey secretKey = keyGen.generateKey();
            byte[] encoded = secretKey.getEncoded();
            return DatatypeConverter.printHexBinary(encoded).toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }
}
