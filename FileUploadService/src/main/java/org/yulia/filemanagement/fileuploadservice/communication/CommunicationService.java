package org.yulia.filemanagement.fileuploadservice.communication;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface CommunicationService {
    ResponseEntity<String> sendFileUrl(String fileUrl);

    void sendDeleteMessage(String originalFilename);

    ResponseEntity<String> getFiles(Map<String, String> queryParams);
}
