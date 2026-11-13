package cl.duoc.horizonte.controller;
import java.security.Principal;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @GetMapping("/me")
    Map<String,Object> me(Authentication auth){ return Map.of("usuario",auth.getName(),"rol",auth.getAuthorities().iterator().next().getAuthority()); }
}
