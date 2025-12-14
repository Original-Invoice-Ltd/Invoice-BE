package invoice.controllers;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Controller to serve static resources like logos for email templates
 */
@RestController
@RequestMapping("/api/static")
public class StaticResourceController {
    
    /**
     * Serve the Original Invoice logo as SVG
     */
    @GetMapping(value = "/logo", produces = "image/svg+xml")
    public ResponseEntity<String> getLogo() {
        try {
            // Return the inline SVG for Original Invoice logo
            String logoSvg = """
                <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <rect width="32" height="32" rx="9" fill="url(#paint0_linear_14310_13934)"/>
                    <rect x="0.25" y="0.25" width="31.5" height="31.5" rx="8.75" stroke="white" stroke-opacity="0.27" stroke-width="0.5"/>
                    <path d="M10.7339 8.09381C10.9572 7.70706 11.3699 7.46881 11.8165 7.46881L20.4298 7.46881C20.8764 7.46881 21.2891 7.70706 21.5124 8.09381L25.819 15.5532C26.0423 15.94 26.0423 16.4165 25.819 16.8032L22.5039 22.5452L21.4214 20.6702L24.0148 16.1782L20.069 9.34381L12.1773 9.34381L9.59725 13.8126H7.43219L10.7339 8.09381Z" fill="#EFF8FF"/>
                    <path d="M20.0875 22.9804L21.0825 24.7037C20.8892 24.822 20.6642 24.8876 20.4298 24.8876H11.8165C11.3699 24.8876 10.9572 24.6494 10.7339 24.2626L6.42723 16.8032C6.31181 16.6033 6.25606 16.3794 6.25996 16.1563L15.6132 16.1562C15.9488 16.1562 16.2589 16.3357 16.4261 16.6268L20.0818 22.9904L20.0875 22.9804Z" fill="#EFF8FF"/>
                    <defs>
                        <linearGradient id="paint0_linear_14310_13934" x1="16" y1="0" x2="16" y2="32" gradientUnits="userSpaceOnUse">
                            <stop stop-color="#3B82F6"/>
                            <stop offset="1" stop-color="#1D4ED8"/>
                        </linearGradient>
                    </defs>
                </svg>
                """;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("image/svg+xml"));
            headers.setCacheControl("public, max-age=86400"); // Cache for 1 day
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(logoSvg);
                    
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Serve the Original Invoice logo as base64 data URL for email embedding
     */
    @GetMapping("/logo-base64")
    public ResponseEntity<String> getLogoBase64() {
        try {
            // Convert SVG to base64 data URL
            String logoSvg = """
                <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <rect width="32" height="32" rx="9" fill="url(#paint0_linear_14310_13934)"/>
                    <rect x="0.25" y="0.25" width="31.5" height="31.5" rx="8.75" stroke="white" stroke-opacity="0.27" stroke-width="0.5"/>
                    <path d="M10.7339 8.09381C10.9572 7.70706 11.3699 7.46881 11.8165 7.46881L20.4298 7.46881C20.8764 7.46881 21.2891 7.70706 21.5124 8.09381L25.819 15.5532C26.0423 15.94 26.0423 16.4165 25.819 16.8032L22.5039 22.5452L21.4214 20.6702L24.0148 16.1782L20.069 9.34381L12.1773 9.34381L9.59725 13.8126H7.43219L10.7339 8.09381Z" fill="#EFF8FF"/>
                    <path d="M20.0875 22.9804L21.0825 24.7037C20.8892 24.822 20.6642 24.8876 20.4298 24.8876H11.8165C11.3699 24.8876 10.9572 24.6494 10.7339 24.2626L6.42723 16.8032C6.31181 16.6033 6.25606 16.3794 6.25996 16.1563L15.6132 16.1562C15.9488 16.1562 16.2589 16.3357 16.4261 16.6268L20.0818 22.9904L20.0875 22.9804Z" fill="#EFF8FF"/>
                    <defs>
                        <linearGradient id="paint0_linear_14310_13934" x1="16" y1="0" x2="16" y2="32" gradientUnits="userSpaceOnUse">
                            <stop stop-color="#3B82F6"/>
                            <stop offset="1" stop-color="#1D4ED8"/>
                        </linearGradient>
                    </defs>
                </svg>
                """;
            
            // Convert to base64
            String base64Logo = java.util.Base64.getEncoder().encodeToString(logoSvg.getBytes());
            String dataUrl = "data:image/svg+xml;base64," + base64Logo;
            
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(dataUrl);
                    
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}