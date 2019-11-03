package br.com.zapia.catarin;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.HashMap;

@EnableAsync
@SpringBootApplication
public class CatarinApplication {

    private static final int MIN_PORT = 1100; // to by set according to your
    private static final int MAX_PORT = 8000; // needs or uploaded from

    public static void main(String[] args) {
        HashMap<String, Object> props = new HashMap<>();
        props.put("server.port", findAvailablePort(MIN_PORT, MAX_PORT));
        SpringApplicationBuilder builder = new SpringApplicationBuilder(CatarinApplication.class);
        builder.headless(false).properties(props).run(args);
    }

    @Bean("threadPoolTaskExecutor")
    public TaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(80);
        executor.setMaxPoolSize(1000);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setThreadNamePrefix("Catarin-Async-");
        return executor;
    }

    public static int findAvailablePort(int minPort, int maxPort) {
        int availablePort;
        for (availablePort = minPort; availablePort < maxPort; availablePort++) {
            if (available(availablePort)) {
                break;
            }
        }
        if (availablePort == minPort && !available(availablePort)) {
            throw new IllegalArgumentException("Cant start container for port: " + availablePort);
        }
        return availablePort;
    }

    public static boolean available(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException ignored) {
        } finally {
            if (ds != null) {
                ds.close();
            }
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException ignored) {
                }
            }
        }
        return false;
    }

}
