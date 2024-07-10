package org.yulia.filemanagement.fileuploadservice.communication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// these are basic tests for the HTTPCommunicationService class, deeper testing done in other classes
@SpringBootTest
public class HTTPCommunicationServiceTests {

    @Mock
    private RestTemplate restTemplate;

    @Autowired
    private HTTPCommunicationService httpCommunicationService;

    @Value("${file.metadata.service.url}")
    private String metadataServiceUrl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        httpCommunicationService = new HTTPCommunicationService(restTemplate, metadataServiceUrl);
    }

    @Test
    public void testSendFileUrl_Successful() {
        String fileUrl = "http://example.com/file";
        String expectedResponse = "{\"message\":\"File registered successfully.\"}";
        ResponseEntity<String> mockResponse = new ResponseEntity<>(expectedResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(any(String.class), any(), any(Class.class))).thenReturn(mockResponse);

        ResponseEntity<String> response = httpCommunicationService.sendFileUrl(fileUrl);

        assert response.getStatusCode() == HttpStatus.OK;
        assert Objects.equals(response.getBody(), expectedResponse);
    }

    @Test
    public void testSendFileUrl_Unsuccessful_5xxError() {
        String fileUrl = "http://example.com/file";
        when(restTemplate.postForEntity(any(String.class), any(), any(Class.class)))
                .thenThrow(new RestClientException("Failed to communicate"));

        ResponseEntity<String> response = httpCommunicationService.sendFileUrl(fileUrl);

        assert response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
        assert Objects.requireNonNull(response.getBody()).contains("Communication error with metadata service");
    }

    @Test
    public void testSendFileUrl_Unsuccessful_4xxError() {
        String fileUrl = "http://example.com/file";
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request");

        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenThrow(exception);

        ResponseEntity<String> response = httpCommunicationService.sendFileUrl(fileUrl);

        // Verify that the response correctly reflects a 4xx client error
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Response status should be BAD_REQUEST");
    }

    @Test
    public void testSendDeleteMessage_Successful() {
        String fileName = "testfile.txt";
        doNothing().when(restTemplate).delete(any(String.class));

        assertDoesNotThrow(() -> httpCommunicationService.sendDeleteMessage(fileName));
    }


    @Test
    public void testGetFiles_Successful() {
        Map<String, String> queryParams = Map.of("type", "image/png");
        String expectedResponse = "[{\"file\":\"file1.png\"}]";
        ResponseEntity<String> mockResponse = new ResponseEntity<>(expectedResponse, HttpStatus.OK);

        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(), any(Class.class))).thenReturn(mockResponse);

        ResponseEntity<String> response = httpCommunicationService.getFiles(queryParams);

        assert response.getStatusCode() == HttpStatus.OK;
        assert Objects.equals(response.getBody(), expectedResponse);
    }

    @Test
    public void testGetFiles_Unsuccessful_5xxError() {
        Map<String, String> queryParams = Map.of("type", "image/png");
        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(), any(Class.class)))
                .thenThrow(new RestClientException("Service unavailable"));

        ResponseEntity<String> response = httpCommunicationService.getFiles(queryParams);

        assert response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
        assert Objects.requireNonNull(response.getBody()).contains("Communication error with metadata service");
    }

    @Test
    void testGetFiles_Unsuccessful_4xxError() {
        Map<String, String> queryParams = Map.of("type", "image/png");
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(exception);

        ResponseEntity<String> response = httpCommunicationService.getFiles(queryParams);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Response status should be BAD_REQUEST");
    }

}
