package com.scit.soragodong.controller;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;

@Controller
public class FinanceController {
    @GetMapping("/finance")
    public String financePage(Model model) {
        model.addAttribute("currentUri", "/finance");
        return "common";
    }

}
