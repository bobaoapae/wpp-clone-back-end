package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.modelo.WhatsAppObjectWithIdProperty;
import br.com.zapia.wppclone.modelo.dto.DTO;
import br.com.zapia.wppclone.modelo.dto.WhatsAppObjectWithIdPropertyCreateDTO;
import br.com.zapia.wppclone.modelo.dto.WhatsAppObjectWithIdPropertyResponseDTO;
import br.com.zapia.wppclone.modelo.dto.WhatsAppObjectWithIdPropertyUpdateDTO;
import br.com.zapia.wppclone.modelo.enums.WhatsAppObjectWithIdType;
import br.com.zapia.wppclone.servicos.WhatsAppObjectWithPropertyService;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Scope("usuario")
@RestController
@RequestMapping("/api/properties")
public class WhatsAppObjectPropertiesRestController {

    @Autowired
    private WhatsAppObjectWithPropertyService whatsAppObjectWithPropertyService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private WhatsAppClone whatsAppClone;

    @GetMapping("/chat/{chatId}/{key}")
    public ResponseEntity<?> getChatProperty(@PathVariable("chatId") String chatId, @PathVariable("key") String key) {
        WhatsAppObjectWithIdProperty lastUserSendMessageProperty = whatsAppObjectWithPropertyService.buscarPropriedade(WhatsAppObjectWithIdType.CHAT, chatId, key);
        if (lastUserSendMessageProperty != null) {
            return ResponseEntity.ok(lastUserSendMessageProperty);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<?> getChatProperties(@PathVariable("chatId") String chatId) {
        return ResponseEntity.ok(modelMapper.map(whatsAppObjectWithPropertyService.buscarPropriedades(WhatsAppObjectWithIdType.CHAT, chatId), new TypeToken<List<WhatsAppObjectWithIdPropertyResponseDTO>>() {
        }.getType()));
    }

    @PostMapping("/chat")
    public ResponseEntity<?> addChatProperty(@DTO(WhatsAppObjectWithIdPropertyCreateDTO.class) WhatsAppObjectWithIdProperty whatsAppObjectWithIdProperty) {
        whatsAppObjectWithIdProperty.setType(WhatsAppObjectWithIdType.CHAT);
        if (whatsAppObjectWithPropertyService.salvar(whatsAppObjectWithIdProperty)) {
            whatsAppClone.enviarEventoWpp(WhatsAppClone.TypeEventWebSocket.CHANGE_PROPERTY_CHAT, modelMapper.map(whatsAppObjectWithIdProperty, WhatsAppObjectWithIdPropertyResponseDTO.class));
            return ResponseEntity.ok(modelMapper.map(whatsAppObjectWithIdProperty, WhatsAppObjectWithIdPropertyResponseDTO.class));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/chat")
    public ResponseEntity<?> updateChatProperty(@DTO(WhatsAppObjectWithIdPropertyUpdateDTO.class) WhatsAppObjectWithIdProperty whatsAppObjectWithIdProperty) {
        if (whatsAppObjectWithPropertyService.salvar(whatsAppObjectWithIdProperty)) {
            whatsAppClone.enviarEventoWpp(WhatsAppClone.TypeEventWebSocket.CHANGE_PROPERTY_CHAT, modelMapper.map(whatsAppObjectWithIdProperty, WhatsAppObjectWithIdPropertyResponseDTO.class));
            return ResponseEntity.ok(modelMapper.map(whatsAppObjectWithIdProperty, WhatsAppObjectWithIdPropertyResponseDTO.class));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/chat/filter/{key}/{value}")
    public ResponseEntity<?> getAllChatIdWithProperty(@PathVariable("key") String key, @PathVariable("value") String value) {
        return ResponseEntity.ok(whatsAppObjectWithPropertyService.buscarWhatsAppIdsComPropriedade(WhatsAppObjectWithIdType.CHAT, key, value));
    }
}
