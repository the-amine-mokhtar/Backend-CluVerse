package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.MemberPayment;
import com.hexaweb.backendcluverse.services.MemberPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/member-payments")
@RequiredArgsConstructor
public class MemberPaymentController {

    private final MemberPaymentService memberPaymentService;

    @GetMapping
    public List<MemberPayment> getByClub(@RequestParam Long clubId) {
        return memberPaymentService.findByClub(clubId);
    }

    @GetMapping("/{id}")
    public MemberPayment getById(@PathVariable Long id) {
        return memberPaymentService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public MemberPayment create(@RequestParam Long membershipId,
                                @RequestParam Long clubId,
                                @RequestBody MemberPayment payment) {
        return memberPaymentService.createPayment(membershipId, clubId, payment);
    }

    @PutMapping("/{id}")
    public MemberPayment update(@PathVariable Long id, @RequestBody MemberPayment payment) {
        payment.setId(id);
        return memberPaymentService.save(payment);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        memberPaymentService.deleteById(id);
    }

    @PostMapping("/remind")
    public int sendReminders(@RequestParam Long clubId) {
        return memberPaymentService.sendReminders(clubId);
    }
}
