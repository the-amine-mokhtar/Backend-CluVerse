package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.TransportPlannerResponse;
import com.hexaweb.backendcluverse.services.TransportPlannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transport-planner")
@CrossOrigin(origins = "*")
public class TransportPlannerController {

    @Autowired
    private TransportPlannerService plannerService;

    @GetMapping(value = "/plan",
                produces = "application/json;charset=UTF-8")
    public ResponseEntity<TransportPlannerResponse> getPlan(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            if (end.isBefore(start)) {
                return ResponseEntity.badRequest().build();
            }
            if (start.plusDays(31).isBefore(end)) {
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.ok(
                plannerService.generatePlan(start, end));

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
