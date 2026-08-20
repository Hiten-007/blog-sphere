package org.hiten.blogspherebackend.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeFile(MultipartFile file, String directory);

    String storeBytes(
            byte[] data,
            String fileName,
            String contentType,
            String directory
    );

    void deleteFile(String filePath);

    Resource loadFileAsResource(String filePath);

    boolean isValidImage(MultipartFile file);

    String generateUniqueFileName(String originalFileName);
}
