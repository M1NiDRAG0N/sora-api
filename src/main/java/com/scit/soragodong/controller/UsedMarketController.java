package com.scit.soragodong.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@RequestMapping("/used-market")
@RequiredArgsConstructor
public class UsedMarketController {
    
    @GetMapping("")
    public String usedMarketPage(Model model) {
        model.addAttribute("currentUri", "/used-market");
        return "common";
    }
    

}
