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
}
