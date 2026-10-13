package com.hangoutbrochure;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HangoutBrochureController {

    @GetMapping("/api/hello")
    public String hello() {
        return "HangoutBrochure server is running";
    }
}