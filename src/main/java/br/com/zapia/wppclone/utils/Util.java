package br.com.zapia.wppclone.utils;

import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Util {

    public static String encodePassword(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(password);
    }

    public static <T> void aguardarFuturesSeremConcluidas(CompletableFuture<T>... futures) {
        aguardarFuturesSeremConcluidas(Arrays.asList(futures));
    }

    public static <T> void aguardarFuturesSeremConcluidas(List<CompletableFuture<T>> futures) {
        while (true) {
            boolean todasCompletas = true;
            for (Future future : futures) {
                if (!future.isDone()) {
                    todasCompletas = false;
                    break;
                }
            }
            if (todasCompletas) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static <T> T pegarResultadoFuture(CompletableFuture<T> future) {
        return pegarResultadosFutures(future).get(0);
    }

    public static <T> List<T> pegarResultadosFutures(CompletableFuture<T>... futures) {
        return pegarResultadosFutures(Arrays.asList(futures));
    }

    public static <T> List<T> pegarResultadosFutures(List<CompletableFuture<T>> futures) {
        Util.aguardarFuturesSeremConcluidas(futures);
        List<T> resultados = new ArrayList<>();
        for (CompletableFuture<T> future : futures) {
            try {
                resultados.add(future.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return resultados;
    }

    public static <K> Collection<List<K>> partition(List<K> lista, int size) {
        final AtomicInteger counter = new AtomicInteger();
        return lista.stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / size)).values();
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
