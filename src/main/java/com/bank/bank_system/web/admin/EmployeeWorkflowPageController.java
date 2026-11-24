package com.bank.bank_system.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/employee")
public class EmployeeWorkflowPageController {
    @GetMapping("/dashboard")
    public RedirectView dashboard() { return new RedirectView("/"); }

    @GetMapping({"/cards", "/memberships", "/loans", "/requests", "/transfers"})
    public String reviews() { return "employee/workflows"; }
}
