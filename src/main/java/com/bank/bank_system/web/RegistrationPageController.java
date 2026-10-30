package com.bank.bank_system.web;

import com.bank.bank_system.service.impl.CustomerRegistrationService;
import com.bank.bank_system.web.client.CustomerRegistrationRequest;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistrationPageController {
    private final CustomerRegistrationService registrationService;
    private final AuthenticationManager customerAuthenticationManager;
    private final SecurityContextRepository customerSecurityContextRepository;

    public RegistrationPageController(
            CustomerRegistrationService registrationService,
            @Qualifier("customerAuthenticationManager") AuthenticationManager customerAuthenticationManager,
            @Qualifier("customerSecurityContextRepository") SecurityContextRepository customerSecurityContextRepository) {
        this.registrationService = registrationService;
        this.customerAuthenticationManager = customerAuthenticationManager;
        this.customerSecurityContextRepository = customerSecurityContextRepository;
    }

    @GetMapping("/register")
    public String registerPage() { return "auth/register"; }

    @PostMapping("/register")
    public String register(@Valid CustomerRegistrationRequest request, BindingResult binding, Model model,
                           HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        if (binding.hasErrors()) {
            model.addAttribute("registrationError", binding.getAllErrors().getFirst().getDefaultMessage());
            model.addAttribute("form", request);
            return "auth/register";
        }
        try {
            registrationService.register(request.firstName(), request.lastName(), request.email(),
                    request.phone(), request.password(), request.confirmPassword());
            Authentication authentication = customerAuthenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim(), request.password()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            new ChangeSessionIdAuthenticationStrategy().onAuthentication(authentication, servletRequest, servletResponse);
            SecurityContextHolder.setContext(context);
            customerSecurityContextRepository.saveContext(context, servletRequest, servletResponse);
            return "redirect:/client/dashboard";
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            model.addAttribute("registrationError", exception.getReason());
            model.addAttribute("form", request);
            return "auth/register";
        } catch (AuthenticationException exception) {
            return "redirect:/login/client?registered";
        }
    }
}
