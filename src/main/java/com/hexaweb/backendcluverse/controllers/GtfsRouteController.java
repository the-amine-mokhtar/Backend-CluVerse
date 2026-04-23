package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.RouteResponse;
import com.hexaweb.backendcluverse.services.GtfsRouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gtfs")
@CrossOrigin(origins = "*")
public class GtfsRouteController {

    @Autowired
    private GtfsRouteService gtfsRouteService;

    @GetMapping(value = "/route", 
                produces = "application/json;charset=UTF-8")
    public ResponseEntity<RouteResponse> getRoute(
            @RequestParam String departure,
            @RequestParam String arrival) {
        
        if (departure == null || departure.trim().isEmpty() ||
            arrival == null || arrival.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        RouteResponse response = gtfsRouteService
            .findRoute(departure.trim(), arrival.trim());
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/stops/search",
                produces = "application/json;charset=UTF-8")
    public ResponseEntity<List<String>> searchStops(
            @RequestParam String q) {
        // useful for frontend autocomplete later
        List<String> names = gtfsRouteService.searchStopNames(q);
        return ResponseEntity.ok(names);
    }

    @GetMapping(value = "/debug",
                produces = "application/json;charset=UTF-8")
    public ResponseEntity<Map<String, Object>> debug() {
        Map<String, Object> info = new HashMap<>();
        info.put("stopsCount", gtfsRouteService.getStopsCount());
        info.put("stopTimesCount", gtfsRouteService.getStopTimesCount());
        info.put("firstStop", gtfsRouteService.getFirstStop());
        return ResponseEntity.ok(info);
    }
}
