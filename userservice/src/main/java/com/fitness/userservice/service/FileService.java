package com.fitness.userservice.service;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDDestinationOrAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileService {
    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String extractSnippet(String s, int maxChars) {
        if (s == null) return "";
        s = s.trim();
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "...";
    }

    /**
     * Scan PDF bytes and return a list of findings (empty = clean).
     * This method:
     * - extracts visible text and searches for suspicious JS terms
     * - iterates low-level COS objects/streams to find /JS, /JavaScript, /OpenAction, /AA, /Launch, /EmbeddedFiles, /URI, etc.
     * <p>
     * NOTE: This loads the whole file into memory (byte[]). For very large files switch to streaming approaches.
     */
    public List<String> scanPdf(byte[] pdfBytes) {
        List<String> findings = new ArrayList<>();

        // Quick text-based checks
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {

            // 1) Check PDF-level actions (OpenAction)
            try {
                PDDestinationOrAction openAction = doc.getDocumentCatalog().getOpenAction();
                if (openAction instanceof PDActionJavaScript) {
                    findings.add("OpenAction contains JavaScript (PDF will run JS on open).");
                } else if (openAction != null) {
                    findings.add("OpenAction present (could trigger actions when opening).");
                }
            } catch (Exception ex) {
                // don't fail the whole scan on one check
                findings.add("Could not inspect OpenAction: " + ex.getMessage());
            }

            // 2) Extract text and look for obvious JS signatures
            try {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(doc);
                String lower = (text == null) ? "" : text.toLowerCase();

                // suspicious text patterns
                String[] suspiciousPatterns = new String[]{"<script", "javascript:", "eval(", "document.write(", "app.alert", "this.geturl", "getannots", "submitform", "exportdata", "collab.getnote", "util.printd"};

                for (String p : suspiciousPatterns) {
                    if (lower.contains(p)) {
                        findings.add("Found suspicious pattern in text: \"" + p + "\"");
                    }
                }
            } catch (Exception ex) {
                findings.add("Could not extract text: " + ex.getMessage());
            }

            // 3) Deep scan: iterate COS objects and streams to find JS / actions / embedded files / URIs
            try {
                COSDocument cosDoc = doc.getDocument();
                // iterate low-level objects
                for (COSObject cosObject : cosDoc.getObjects()) {
                    COSBase base = cosObject.getObject();
                    if (base instanceof COSDictionary) {
                        COSDictionary dict = (COSDictionary) base;
                        // check for keys commonly used by malicious PDFs
                        for (COSName key : dict.keySet()) {
                            String name = key.getName();
                            if (name.equalsIgnoreCase("JS") || name.equalsIgnoreCase("JavaScript")) {
                                findings.add("COS dictionary contains JavaScript entry (key: " + name + ").");
                                COSBase val = dict.getItem(key);
                                if (val instanceof COSString) {
                                    findings.add("  -> JavaScript string found: " + truncate(((COSString) val).getString(), 200));
                                }
                            }
                            if (name.equalsIgnoreCase("OpenAction") || name.equalsIgnoreCase("AA") || name.equalsIgnoreCase("Launch") || name.equalsIgnoreCase("EmbeddedFiles") || name.equalsIgnoreCase("URI")) {
                                findings.add("COS dictionary contains suspicious key: " + name);
                            }
                        }
                    }
                    if (base instanceof COSStream) {
                        COSStream stream = (COSStream) base;
                        // read unfiltered stream bytes and search for patterns (may reveal obfuscated JS)
                        try (InputStream is = stream.createInputStream()) {
                            byte[] bytes = is.readAllBytes();
                            String s = new String(bytes, StandardCharsets.UTF_8).toLowerCase();
                            if (s.contains("javascript") || s.contains("/js") || s.contains("app.alert") || s.contains("eval(") || s.contains("this.geturl") || s.contains("document.write")) {
                                findings.add("Found suspicious content inside a stream (possible embedded or obfuscated JS). Snippet: " + truncate(extractSnippet(s, 80), 300));
                            }
                        } catch (IOException e) {
                            // ignore streams we cannot decode, but record it
                            findings.add("Could not read a COSStream (possible compressed/encoded stream) — " + e.getMessage());
                        }
                    }
                }
            } catch (Exception ex) {
                findings.add("Deep COS scan failed: " + ex.getMessage());
            }
        } catch (IOException e) {
            findings.add("Failed to load PDF (corrupted or not a PDF): " + e.getMessage());
        }
        return findings;
    }
}