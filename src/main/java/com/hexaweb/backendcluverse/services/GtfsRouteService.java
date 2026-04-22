package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.RouteResponse;
import com.hexaweb.backendcluverse.dto.RouteStep;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GtfsRouteService {

    private List<Map<String, String>> stops = new ArrayList<>();
    private List<Map<String, String>> stopTimes = new ArrayList<>();
    private List<Map<String, String>> trips = new ArrayList<>();
    private List<Map<String, String>> routes = new ArrayList<>();

    @PostConstruct
    public void loadGtfsData() {
        try {
            stops = loadCsv("/gtfs/stops.txt");
            stopTimes = loadCsv("/gtfs/stop_times.txt");
            trips = loadCsv("/gtfs/trips.txt");
            routes = loadCsv("/gtfs/routes.txt");
            System.out.println("✅ GTFS data loaded: " 
                + stops.size() + " stops, " 
                + stopTimes.size() + " stop_times");
        } catch (Exception e) {
            System.err.println("Failed to load GTFS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Map<String, String>> loadCsv(String resourcePath) 
            throws Exception {
        List<Map<String, String>> records = new ArrayList<>();
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            System.err.println("File not found: " + resourcePath);
            return records;
        }
        
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
        CSVFormat format = CSVFormat.DEFAULT
            .withFirstRecordAsHeader()
            .withIgnoreHeaderCase()
            .withTrim()
            .withIgnoreEmptyLines();
        
        try (CSVParser parser = new CSVParser(reader, format)) {
            for (CSVRecord record : parser) {
                Map<String, String> map = new LinkedHashMap<>();
                parser.getHeaderNames().forEach(header -> 
                    map.put(header, record.get(header))
                );
                records.add(map);
            }
        }
        return records;
    }

    public RouteResponse findRoute(String depName, String arrName) {
        // Step 1: find matching stops by name (partial, ignore case)
        List<String> depStopIds = stops.stream()
            .filter(s -> s.getOrDefault("stop_name","")
                .toLowerCase().contains(depName.toLowerCase()))
            .map(s -> s.get("stop_id"))
            .collect(Collectors.toList());

        List<String> arrStopIds = stops.stream()
            .filter(s -> s.getOrDefault("stop_name","")
                .toLowerCase().contains(arrName.toLowerCase()))
            .map(s -> s.get("stop_id"))
            .collect(Collectors.toList());

        if (depStopIds.isEmpty() || arrStopIds.isEmpty()) {
            return buildNotFoundResponse(depName, arrName);
        }

        // Step 2: group stop_times by trip_id
        Map<String, List<Map<String,String>>> byTrip = stopTimes.stream()
            .collect(Collectors.groupingBy(
                st -> st.getOrDefault("trip_id", "")));

        // Step 3: find a trip containing both dep and arr stops in order
        for (Map.Entry<String, List<Map<String,String>>> entry 
                : byTrip.entrySet()) {
            
            List<Map<String,String>> tripStops = entry.getValue().stream()
                .sorted(Comparator.comparingInt(st -> {
                    try { 
                        return Integer.parseInt(
                            st.getOrDefault("stop_sequence","0")); 
                    } catch(Exception e) { return 0; }
                }))
                .collect(Collectors.toList());

            int depIdx = -1, arrIdx = -1;
            for (int i = 0; i < tripStops.size(); i++) {
                String sid = tripStops.get(i).get("stop_id");
                if (depIdx == -1 && depStopIds.contains(sid)) {
                    depIdx = i;
                }
                if (depIdx != -1 && arrIdx == -1 
                    && arrStopIds.contains(sid)) {
                    arrIdx = i;
                }
            }

            if (depIdx != -1 && arrIdx != -1 && arrIdx > depIdx) {
                // Found a valid trip — build response
                List<Map<String,String>> segment = 
                    tripStops.subList(depIdx, arrIdx + 1);
                
                List<RouteStep> steps = new ArrayList<>();
                for (int i = 0; i < segment.size(); i++) {
                    Map<String,String> st = segment.get(i);
                    String sid = st.get("stop_id");
                    
                    // find stop details
                    Map<String,String> stopInfo = stops.stream()
                        .filter(s -> sid.equals(s.get("stop_id")))
                        .findFirst().orElse(new HashMap<>());
                    
                    RouteStep step = new RouteStep();
                    step.setSequence(i + 1);
                    step.setStopId(sid);
                    step.setStopName(stopInfo.getOrDefault(
                        "stop_name", sid));
                    try {
                        step.setLatitude(Double.parseDouble(
                            stopInfo.getOrDefault("stop_lat","0")));
                        step.setLongitude(Double.parseDouble(
                            stopInfo.getOrDefault("stop_lon","0")));
                    } catch(Exception e) {}
                    step.setArrivalTime(st.getOrDefault(
                        "arrival_time",""));
                    step.setDepartureTime(st.getOrDefault(
                        "departure_time",""));
                    steps.add(step);
                }

                // find route name
                String tripId = entry.getKey();
                String routeId = trips.stream()
                    .filter(t -> tripId.equals(t.get("trip_id")))
                    .map(t -> t.get("route_id"))
                    .findFirst().orElse("");
                String routeName = routes.stream()
                    .filter(r -> routeId.equals(r.get("route_id")))
                    .map(r -> r.getOrDefault("route_long_name",
                               r.getOrDefault("route_short_name","")))
                    .findFirst().orElse("Ligne inconnue");

                // calculate duration
                String depTime = steps.get(0).getDepartureTime();
                String arrTime = steps.get(steps.size()-1)
                    .getArrivalTime();
                int durationMin = calculateDuration(depTime, arrTime);

                RouteResponse response = new RouteResponse();
                response.setTripId(tripId);
                response.setRouteName(routeName);
                response.setSteps(steps);
                response.setTotalStops(steps.size());
                response.setTotalDuration(durationMin + " min");
                return response;
            }
        }
        return buildNotFoundResponse(depName, arrName);
    }

    private int calculateDuration(String start, String end) {
        try {
            String[] s = start.split(":");
            String[] e = end.split(":");
            int startMin = Integer.parseInt(s[0])*60 
                         + Integer.parseInt(s[1]);
            int endMin = Integer.parseInt(e[0])*60 
                       + Integer.parseInt(e[1]);
            return Math.max(0, endMin - startMin);
        } catch(Exception ex) { return 0; }
    }

    private RouteResponse buildNotFoundResponse(
            String dep, String arr) {
        RouteResponse r = new RouteResponse();
        r.setRouteName("Aucun itinéraire trouvé entre " 
            + dep + " et " + arr);
        r.setSteps(new ArrayList<>());
        r.setTotalStops(0);
        r.setTotalDuration("0 min");
        return r;
    }

    public List<String> searchStopNames(String query) {
        return stops.stream()
            .map(s -> s.getOrDefault("stop_name", ""))
            .filter(name -> name.toLowerCase()
                .contains(query.toLowerCase()))
            .distinct()
            .sorted()
            .limit(10)
            .collect(Collectors.toList());
    }

    // Debug methods
    public int getStopsCount() {
        return stops.size();
    }

    public int getStopTimesCount() {
        return stopTimes.size();
    }

    public Map<String, String> getFirstStop() {
        return stops.isEmpty() ? new HashMap<>() : stops.get(0);
    }
}
