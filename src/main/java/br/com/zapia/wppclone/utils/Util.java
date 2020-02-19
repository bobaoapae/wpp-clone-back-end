package br.com.zapia.wppclone.utils;

import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Util {

    public static String encodePassword(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(password);
    }

    public static String gerarSenha(int length, boolean specialChars) {
        PasswordGenerator gen = new PasswordGenerator();
        CharacterData lowerCaseChars = EnglishCharacterData.LowerCase;
        CharacterRule lowerCaseRule = new CharacterRule(lowerCaseChars);
        lowerCaseRule.setNumberOfCharacters(2);

        CharacterData upperCaseChars = EnglishCharacterData.UpperCase;
        CharacterRule upperCaseRule = new CharacterRule(upperCaseChars);
        upperCaseRule.setNumberOfCharacters(2);

        CharacterData digitChars = EnglishCharacterData.Digit;
        CharacterRule digitRule = new CharacterRule(digitChars);
        digitRule.setNumberOfCharacters(2);
        CharacterRule splCharRule = new CharacterRule(EnglishCharacterData.Special);
        splCharRule.setNumberOfCharacters(2);

        if (specialChars) {
            return gen.generatePassword(length, splCharRule, lowerCaseRule,
                    upperCaseRule, digitRule);
        } else {
            return gen.generatePassword(length, lowerCaseRule,
                    upperCaseRule, digitRule);
        }
    }
}
