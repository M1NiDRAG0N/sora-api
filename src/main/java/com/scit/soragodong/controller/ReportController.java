package com.scit.soragodong.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class ReportController {
	
	@GetMapping("/report")
	public String report(Model model){
		model.addAttribute("currentUri", "/report");
		
		return "common";
	}
}
