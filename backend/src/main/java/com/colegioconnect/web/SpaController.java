package com.colegioconnect.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the Vite entry point for browser routes; API and actuator paths are never mapped here. */
@Controller
public class SpaController {

    @GetMapping({"/", "/login", "/register", "/verify", "/dashboard", "/forbidden",
            "/student", "/student/**", "/institution", "/institution/**",
            "/company", "/company/**", "/moderation", "/moderation/**"})
    public String index() {
        return "forward:/index.html";
    }
}
