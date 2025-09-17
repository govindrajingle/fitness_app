package com.fitness.userservice.controller;

import com.fitness.userservice.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadPdf(@RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> resp = new HashMap<>();
        if (file == null || file.isEmpty()) {
            resp.put("ok", "No file provided");
            resp.put("message", "No file provided");
            return ResponseEntity.badRequest().body(resp); //400
        }
//        1) Quick MIME-type hint check (not sufficient)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            resp.put("mimeTypeWarning", "Uploaded content-type is not application/pdf (was: " + contentType + ")");
        }
//        2) Read file bytes
        byte[] bytes = file.getBytes();
//        3) Verify PDF magic reader
        String header = new String(bytes, 0, Math.min(bytes.length, 5), StandardCharsets.US_ASCII);
        if (!header.startsWith("%PDF-")) {
            resp.put("ok", false);
            resp.put("message", "File does nt have PDF magic header, not a PDF.");
            return ResponseEntity.badRequest().body(resp);//400
        }
//        4) Scan pdf
        List<String> findings = fileService.scanPdf(bytes);
//        5) Result compose
        boolean suspicious = !findings.isEmpty();
        resp.put("ok", true);
        resp.put("suspicious", suspicious);
        resp.put("findingCount", findings.size());
        resp.put("findings", findings);
//        Showing in console
        if (suspicious) {
            System.out.println("PDF scan findings for file '" + file.getOriginalFilename() + "'.");
            findings.forEach(f -> System.out.println("  - " + f));
        } else {
            System.out.println("PDF appears clean (no suspicious JS/actions found) for file '" + file.getOriginalFilename() + "'.");
        }
        return ResponseEntity.ok(resp); //200
    }
}
