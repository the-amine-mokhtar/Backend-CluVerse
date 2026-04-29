package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.services.SmsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class SmsTestController {

    private final SmsService smsService;

    public SmsTestController(SmsService smsService) {
        this.smsService = smsService;
    }

    @GetMapping("/sms-test")
    public String testSms() {

        boolean ok = smsService.sendSms(
                "+216XXXXXXXX",
                "🚀 Test SMS OK"
        );

        return ok ? "SMS envoyé" : "Erreur SMS";
    }
}