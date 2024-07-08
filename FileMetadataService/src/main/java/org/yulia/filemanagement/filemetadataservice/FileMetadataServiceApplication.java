package org.yulia.filemanagement.filemetadataservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class FileMetadataServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileMetadataServiceApplication.class, args);
	}
}