package com.bank.bank_system.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginPageController {

    @GetMapping({"/login", "/login/employee"})
    public String login() {
        return "forward:/login.html";
    }
}
