package com.ivhanfc.scannerjs.app_java;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // permite cualquier origen
public class JsonController {
     @PostMapping("/id")
    public String recibirId(@RequestBody Map<String, Object> body) {
        System.out.println("ID recibido: " + body.get("id"));
        return "ID recibido correctamente: " + body.get("id");
    }
}
