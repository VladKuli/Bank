package com.bank.bank_system.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TermsPageController {
    @GetMapping("/terms")
    public String terms() {
        return "auth/terms";
    }
}
