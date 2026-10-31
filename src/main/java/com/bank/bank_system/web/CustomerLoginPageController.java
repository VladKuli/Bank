package com.bank.bank_system.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CustomerLoginPageController {
    @GetMapping("/login/client")
    public String customerLogin() { return "auth/client-login"; }
}
