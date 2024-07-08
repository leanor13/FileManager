package org.yulia.filemanagement.fileuploadservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class StaticResourceController {

    @RequestMapping("/health")
    @ResponseBody
    public String health() {
        // Handle requests to the "/health" URL and return a standard response for health checks.
        return "OK";
    }

    // Handle all other requests to static resources to avoid unnecessary 404 errors
    @RequestMapping("/**")
    @ResponseBody
    public ResponseEntity<Void> handleNotFound() {
        // Return 404 for any other request not matched by other handlers
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
