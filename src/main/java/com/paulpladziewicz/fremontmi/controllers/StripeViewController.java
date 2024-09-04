package com.paulpladziewicz.fremontmi.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StripeViewController {

    @GetMapping("/stripe/register")
    public String register() {
        return "stripe/register";
    }

    @GetMapping("/stripe/prices")
    public String prices() {
        return "stripe/prices";
    }

    @GetMapping("/stripe/subscribe")
    public String subscribe() {
        return "stripe/subscribe";
    }

    @GetMapping("/stripe/account")
    public String account() {
        return "stripe/account";
    }

    @GetMapping("/stripe/cancel")
    public String cancel() {
        return "stripe/cancel";
    }
}
