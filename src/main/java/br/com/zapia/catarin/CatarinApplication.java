package br.com.zapia.catarin;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.HashMap;

@SpringBootApplication
public class CatarinApplication {

    private static final int MIN_PORT = 1100; // to by set according to your
    private static final int MAX_PORT = 8000; // needs or uploaded from

    public static void main(String[] args) {
        int availablePort;
        for (availablePort = MIN_PORT; availablePort < MAX_PORT; availablePort++) {
            if (available(availablePort)) {
                break;
            }
        }
        if (availablePort == MIN_PORT && !available(availablePort)) {
            throw new IllegalArgumentException("Cant start container for port: " + availablePort);
        }
        System.out.println(availablePort);
        HashMap<String, Object> props = new HashMap<>();
        props.put("server.port", availablePort);
        SpringApplicationBuilder builder = new SpringApplicationBuilder(CatarinApplication.class);
        builder.headless(false).properties(props).run(args);
    }

    public static boolean available(int port) {
        System.out.println("TRY PORT " + port);
        // if you have some range for denied ports you can also check it
        // here just add proper checking and return
        // false if port checked within that range
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }

}
