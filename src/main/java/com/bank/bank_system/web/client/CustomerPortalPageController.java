package com.bank.bank_system.web.client;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/client")
public class CustomerPortalPageController {
    @GetMapping({"", "/dashboard", "/accounts", "/cards", "/card-tiers", "/transfers", "/transactions", "/analytics", "/loans", "/support", "/profile"})
    public String customerPortal() { return "client/index"; }
}
