package com.artguard.gateway.web;

import com.artguard.gateway.alert.AlertSocketHandler;
import com.artguard.gateway.camera.CameraIngestService;
import com.artguard.gateway.incident.Incident;
import com.artguard.gateway.incident.IncidentService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** REST surface for the dashboard: cameras, incidents, and pipeline stats. */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private final CameraIngestService cameras;
    private final IncidentService incidents;
    private final AlertSocketHandler alerts;

    public ApiController(CameraIngestService cameras, IncidentService incidents, AlertSocketHandler alerts) {
        this.cameras = cameras;
        this.incidents = incidents;
        this.alerts = alerts;
    }

    @GetMapping("/cameras")
    public List<Map<String, Object>> cameras() {
        return cameras.stats();
    }

    @GetMapping("/incidents")
    public List<Incident> incidents() {
        return incidents.recent();
    }

    @GetMapping("/incidents/open")
    public List<Incident> openIncidents() {
        return incidents.open();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        var cams = cameras.stats();
        long published = cams.stream().mapToLong(c -> ((Number) c.get("published")).longValue()).sum();
        long dropped   = cams.stream().mapToLong(c -> ((Number) c.get("dropped")).longValue()).sum();
        return Map.of(
            "cameras", cams.size(),
            "framesPublished", published,
            "framesDropped", dropped,
            "openIncidents", incidents.open().size(),
            "dashboardClients", alerts.connectedClients());
    }
}
