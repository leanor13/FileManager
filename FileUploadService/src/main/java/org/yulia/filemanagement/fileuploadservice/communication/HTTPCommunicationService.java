package org.yulia.filemanagement.fileuploadservice.communication;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@Profile("http")
public class HTTPCommunicationService implements CommunicationService {

    private static final Logger logger = LoggerFactory.getLogger(HTTPCommunicationService.class);

    private final RestTemplate restTemplate;
    private final String metadataServiceUrl;

    @Autowired
    public HTTPCommunicationService(RestTemplate restTemplate,
                                    @Value("${file.metadata.service.url}") String metadataServiceUrl) {
        this.restTemplate = restTemplate;
        this.metadataServiceUrl = metadataServiceUrl;
    }

    /**
     * Sends the URL of an uploaded file to the metadata service for registration.
     *
     * @param fileUrl the URL of the uploaded file
     * @return a ResponseEntity containing the response from the metadata service
     */
    @Override
    public ResponseEntity<String> sendFileUrl(String fileUrl) {
        try {
            var url = metadataServiceUrl + "/register";  // Building the URL for the POST request
            logger.info("Sending file URL: {}", fileUrl); // Log the URL being sent
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            var json = new JSONObject();
            json.put("fileUrl", fileUrl);
            var entity = new HttpEntity<>(json.toString(), headers);
            // Using service discovery to find the metadata service
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return handleResponse(response);
        } catch (RestClientException ex) {
            logger.error("Failed to send file URL due to an exception", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Communication error " +
                    "with metadata service.\"}");
        }
    }

    /**
     * Sends a request to the metadata service to delete a file entry.
     *
     * @param fileName the name of the file to be deleted
     */
    @Override
    public void sendDeleteMessage(String fileName) {
        try {
            logger.info("Sending delete message for file: {}", fileName);
            // Building the URL for the DELETE request
            var url = metadataServiceUrl + "/delete?fileName=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);

            // send DELETE request to metadata service
            restTemplate.delete(url);
            logger.info("Delete message successfully sent to metadata service.");
        } catch (RestClientException ex) {
            handleRestClientException(ex);
        }
    }

    /**
     * Retrieves files from the metadata service based on provided query parameters.
     *
     * @param queryParams a map of query parameters for filtering the files
     * @return a ResponseEntity containing the response from the metadata service
     */
    @Override
    public ResponseEntity<String> getFiles(Map<String, String> queryParams) {
        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(metadataServiceUrl + "/files");
            queryParams.forEach(uriBuilder::queryParam);
            logger.info("Sending request to metadata service to retrieve files");

            ResponseEntity<String> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.GET,
                    null,
                    String.class);

            HttpStatus statusCode = (HttpStatus) response.getStatusCode();

            return handleResponse(response);
        } catch (RestClientException ex) {
            logger.error("Failed to retrieve files due to an exception", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Communication error " +
                    "with metadata service.\"}");
        }
    }

    // logs various types of exceptions that can occur during communication
    private void handleRestClientException(RestClientException ex) {
        if (ex instanceof HttpClientErrorException clientError) {
            logger.error("Client error during communication: Status code {}", clientError.getStatusCode());
        } else if (ex instanceof HttpServerErrorException serverError) {
            logger.error("Server error during communication: Status code {}", serverError.getStatusCode());
        } else {
            logger.error("Communication error: ", ex);
        }
    }

    // handles the response from the metadata service
    private ResponseEntity<String> handleResponse(ResponseEntity<String> response) {
        HttpStatus statusCode = (HttpStatus) response.getStatusCode();

        if (statusCode.is2xxSuccessful()) {
            logger.info("Operation successful with status: {}", statusCode);
            return response;
        } else if (statusCode.is4xxClientError()) {
            logger.warn("Client error from metadata service: {}", statusCode);
            return ResponseEntity.status(statusCode).body(response.getBody());
        } else {
            logger.error("Server error from metadata service: {}", statusCode);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Internal server error. Please try again later.\"}");
        }
    }

}
