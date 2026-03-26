package org.hiten.blogspherebackend.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.config.FileStorageConfig;
import org.hiten.blogspherebackend.exception.BadRequestException;
import org.hiten.blogspherebackend.exception.FileStorageException;
import org.hiten.blogspherebackend.exception.ResourceNotFoundException;
import org.hiten.blogspherebackend.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final FileStorageConfig fileStorageConfig;

    // Resolved absolute path to the upload root, set once at startup
    private Path uploadRootPath;

    @PostConstruct
    public void init() {
        uploadRootPath = Paths.get(fileStorageConfig.getUploadDir())
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(uploadRootPath);
            log.info("File storage root initialized at: {}", uploadRootPath);
        } catch (IOException e) {
            throw new FileStorageException("Cannot create upload directory: " + uploadRootPath, e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String directory) {
        // 1. Validation
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File must not b empty");
        }

        if(!isValidImage(file)) {
            throw new BadRequestException("Invalid file type. Allowed types: " +
                        String.join(", ", fileStorageConfig.getAllowedTypes())
                    );
        }

        if(file.getSize() > fileStorageConfig.getMaxSize()) {
            long maxMb = fileStorageConfig.getMaxSize() / (1024 * 1024);
            throw new BadRequestException(
                    "File size exceeds the maximum allowed size of " + maxMb + " MB"
            );
        }

        // 2. Generate unique filename
        String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());

        // 3. Resolve target directory, guarding against path traversal
        String safeDirectory = sanitizeDirectory(directory);
        Path targetDirectory = uploadRootPath.resolve(safeDirectory).normalize();

        // Ensure the resolved path is still with the upload root
        if(!targetDirectory.startsWith(uploadRootPath)) {
            throw new BadRequestException("Invalid storage directory: " + directory);
        }

        try {
            Files.createDirectories(targetDirectory);
        } catch (IOException e) {
            throw new FileStorageException("Cannot create storage directory: " + targetDirectory, e);
        }

        // 4. Write to disk
        Path targetPath = targetDirectory.resolve(uniqueFileName);
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {} ({} bytes)", targetPath, file.getSize());
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file " + uniqueFileName, e);
        }

        // 5. Return relative path (e.g., "posts/550e8400-...jpg")
        return safeDirectory + "/" + uniqueFileName;
    }

    @Override
    public void deleteFile(String filePath) {
        if(!StringUtils.hasText(filePath)) {
            return;
        }

        Path target = resolveAndValidatePath(filePath);

        try {
            boolean deleted = Files.deleteIfExists(target);
            if(deleted) {
                log.info("Deleted file: {}", target);
            } else {
                log.warn("File not found for deletion (already gone?): {}", target);
            }
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file: " + filePath, e);
        }
    }

    @Override
    public Resource loadFileAsResource(String filePath) {
        Path target = resolveAndValidatePath(filePath);

        try {
            Resource resource = new UrlResource(target.toUri());
            if(resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ResourceNotFoundException("File", "path", filePath);
        } catch (MalformedURLException e) {
            throw new FileStorageException("Cannot load file: " + filePath, e);
        }
    }

    @Override
    public boolean isValidImage(MultipartFile file) {
        if(file == null || file.isEmpty()) {
            return false;
        }

        // Check declared MIME type
        String contentType = file.getContentType();
        if (contentType == null || !fileStorageConfig.getAllowedTypes().contains(contentType.toLowerCase())) {
            return false;
        }

        // Cross-check the file extension
        String originalName = file.getOriginalFilename();
        if(!StringUtils.hasText(originalName)) {
            return false;
        }

        String extension = getExtension(originalName).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> contentType.equals("image/jpeg");
            case "png" -> contentType.equals("image/png");
            case "gif" -> contentType.equals("image/gif");
            case "webp" -> contentType.equals("image/webp");
            default -> false;
        };
    }

    @Override
    public String generateUniqueFileName(String originalFileName) {
        String extension = "";
        if(StringUtils.hasText(originalFileName)) {
            extension = getExtension(originalFileName);
            if(!extension.isEmpty()) {
                extension = "." + extension.toLowerCase();
            }
        }
        return UUID.randomUUID() + extension;
    }

    // ----------------------------------------------
    // Private Helpers
    // ----------------------------------------------

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex >= 0) ? fileName.substring(dotIndex + 1) : "";
    }

    private String sanitizeDirectory(String directory) {
        if(!StringUtils.hasText(directory)) {
            return "misc";
        }
        // Remove any characters that could construct a relative path attack
        return directory.replaceAll("[./\\\\]", "").trim();
    }

    private Path resolveAndValidatePath(String filePath) {
        Path resolved = uploadRootPath.resolve(filePath).normalize();
        if(!resolved.startsWith(uploadRootPath)) {
            throw new BadRequestException("Invalid file path: " + filePath);
        }
        return resolved;
    }
}
