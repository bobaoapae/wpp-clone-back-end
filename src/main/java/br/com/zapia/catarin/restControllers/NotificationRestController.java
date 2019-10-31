package br.com.zapia.catarin.restControllers;

import br.com.zapia.catarin.payloads.Notification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/notifications")
public class NotificationRestController {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping("/init")
    public SseEmitter getNewNotification() {
        SseEmitter emitter = new SseEmitter();
        this.emitters.add(emitter);

        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            this.emitters.remove(emitter);
        });

        return emitter;
    }

    public void sendNotification(Notification notification) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        this.emitters.forEach(emitter -> {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event();
                event.id(String.valueOf(event.hashCode()));
                event.name(notification.getType());
                event.data(notification.getDado());
                emitter.send(event);
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        });
        this.emitters.remove(deadEmitters);
    }

    public boolean possuiEmitters() {
        return this.emitters.isEmpty();
    }

}
