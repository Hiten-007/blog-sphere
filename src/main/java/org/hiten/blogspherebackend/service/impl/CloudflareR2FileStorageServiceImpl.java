package org.hiten.blogspherebackend.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.config.FileStorageConfig;
import org.hiten.blogspherebackend.config.R2StorageConfig;
import org.hiten.blogspherebackend.exception.BadRequestException;
import org.hiten.blogspherebackend.exception.FileStorageException;
import org.hiten.blogspherebackend.exception.ResourceNotFoundException;
import org.hiten.blogspherebackend.service.FileStorageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudflareR2FileStorageServiceImpl
        implements FileStorageService {

    private final R2StorageConfig r2StorageConfig;
    private final FileStorageConfig fileStorageConfig;

    private S3Client s3Client;

    @PostConstruct
    public void init() {

        if (!StringUtils.hasText(r2StorageConfig.getAccountId())
                || !StringUtils.hasText(r2StorageConfig.getAccessKey())
                || !StringUtils.hasText(r2StorageConfig.getSecretKey())
                || !StringUtils.hasText(r2StorageConfig.getBucket())) {

            throw new IllegalStateException(
                    "R2 configuration is incomplete. " +
                            "Required: R2_ACCOUNT_ID, R2_ACCESS_KEY, " +
                            "R2_SECRET_KEY, R2_BUCKET"
            );
        }

        String endpoint =
                "https://" + r2StorageConfig.getAccountId()
                        + ".r2.cloudflarestorage.com";

        log.info(
                "R2 Account ID = {}",
                r2StorageConfig.getAccountId()
        );

        log.info(
                "R2 Endpoint = {}",
                endpoint
        );

        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(
                        r2StorageConfig.getAccessKey(),
                        r2StorageConfig.getSecretKey()
                );

        S3Configuration s3Configuration =
                S3Configuration.builder()
                        .chunkedEncodingEnabled(false)
                        .build();

        s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(
                        StaticCredentialsProvider.create(credentials)
                )
                .serviceConfiguration(s3Configuration)
                .build();

        log.info(
                "Cloudflare R2 storage initialized. Bucket={}",
                r2StorageConfig.getBucket()
        );

        if (StringUtils.hasText(r2StorageConfig.getPublicUrl())) {
            log.info(
                    "R2 Public URL = {}",
                    r2StorageConfig.getPublicUrl()
            );
        } else {
            log.warn(
                    "R2 public URL is not configured. " +
                            "Uploaded files will only have object keys."
            );
        }
    }

    @Override
    public String storeFile(
            MultipartFile file,
            String directory
    ) {

        // 1. Validate file

        if (file == null || file.isEmpty()) {
            throw new BadRequestException(
                    "File must not be empty"
            );
        }

        if (!isValidImage(file)) {
            throw new BadRequestException(
                    "Invalid file type. Allowed types: " +
                            String.join(
                                    ", ",
                                    fileStorageConfig.getAllowedTypes()
                            )
            );
        }

        if (file.getSize() > fileStorageConfig.getMaxSize()) {

            long maxMb =
                    fileStorageConfig.getMaxSize()
                            / (1024 * 1024);

            throw new BadRequestException(
                    "File size exceeds the maximum allowed size of "
                            + maxMb + " MB"
            );
        }

        // 2. Generate unique filename

        String uniqueFileName =
                generateUniqueFileName(
                        file.getOriginalFilename()
                );

        // 3. Build R2 object key

        String safeDirectory =
                sanitizeDirectory(directory);

        String objectKey =
                safeDirectory + "/" + uniqueFileName;

        // 4. Upload to R2

        try {

            byte[] fileBytes = file.getBytes();

            PutObjectRequest request =
                    PutObjectRequest.builder()
                            .bucket(
                                    r2StorageConfig.getBucket()
                            )
                            .key(objectKey)
                            .contentType(
                                    file.getContentType()
                            )
                            .contentLength(
                                    (long) fileBytes.length
                            )
                            .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(fileBytes)
            );

            log.info(
                    "Stored file in R2: {} ({} bytes)",
                    objectKey,
                    fileBytes.length
            );

            return buildPublicUrl(objectKey);

        } catch (Exception e) {

            log.error(
                    "R2 upload failed for object: {}",
                    objectKey,
                    e
            );

            throw new FileStorageException(
                    "Failed to store file in R2: "
                            + objectKey,
                    e
            );
        }
    }

    @Override
    public String storeBytes(
            byte[] data,
            String fileName,
            String contentType,
            String directory
    ) {

        if (data == null || data.length == 0) {
            throw new BadRequestException(
                    "File data must not be empty"
            );
        }

        String safeDirectory =
                sanitizeDirectory(directory);

        String safeFileName =
                generateUniqueFileName(fileName);

        String objectKey =
                safeDirectory + "/" + safeFileName;

        try {

            PutObjectRequest request =
                    PutObjectRequest.builder()
                            .bucket(
                                    r2StorageConfig.getBucket()
                            )
                            .key(objectKey)
                            .contentType(contentType)
                            .contentLength(
                                    (long) data.length
                            )
                            .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(data)
            );

            log.info(
                    "Stored generated file in R2: {} ({} bytes)",
                    objectKey,
                    data.length
            );

            return buildPublicUrl(objectKey);

        } catch (Exception e) {

            log.error(
                    "R2 upload failed for generated object: {}",
                    objectKey,
                    e
            );

            throw new FileStorageException(
                    "Failed to store bytes in R2: "
                            + objectKey,
                    e
            );
        }
    }

    @Override
    public void deleteFile(String filePath) {

        if (!StringUtils.hasText(filePath)) {
            return;
        }

        String objectKey = extractObjectKey(filePath);

        try {

            DeleteObjectRequest request =
                    DeleteObjectRequest.builder()
                            .bucket(
                                    r2StorageConfig.getBucket()
                            )
                            .key(objectKey)
                            .build();

            s3Client.deleteObject(request);

            log.info(
                    "Deleted R2 object: {}",
                    objectKey
            );

        } catch (Exception e) {

            log.error(
                    "R2 delete failed for object: {}",
                    objectKey,
                    e
            );

            throw new FileStorageException(
                    "Failed to delete R2 object: "
                            + objectKey,
                    e
            );
        }
    }

    @Override
    public Resource loadFileAsResource(
            String filePath
    ) {

        if (!StringUtils.hasText(filePath)) {
            throw new ResourceNotFoundException(
                    "File",
                    "path",
                    filePath
            );
        }

        String objectKey = extractObjectKey(filePath);

        try {

            GetObjectRequest request =
                    GetObjectRequest.builder()
                            .bucket(
                                    r2StorageConfig.getBucket()
                            )
                            .key(objectKey)
                            .build();

            ResponseBytes<GetObjectResponse> response =
                    s3Client.getObjectAsBytes(request);

            return new ByteArrayResource(
                    response.asByteArray()
            );

        } catch (NoSuchKeyException e) {

            throw new ResourceNotFoundException(
                    "File",
                    "path",
                    objectKey
            );

        } catch (Exception e) {

            log.error(
                    "R2 load failed for object: {}",
                    objectKey,
                    e
            );

            throw new FileStorageException(
                    "Failed to load R2 object: "
                            + objectKey,
                    e
            );
        }
    }

    @Override
    public boolean isValidImage(
            MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType =
                file.getContentType();

        if (contentType == null
                || !fileStorageConfig
                .getAllowedTypes()
                .contains(
                        contentType.toLowerCase()
                )) {

            return false;
        }

        String originalName =
                file.getOriginalFilename();

        if (!StringUtils.hasText(originalName)) {
            return false;
        }

        String extension =
                getExtension(originalName)
                        .toLowerCase();

        return switch (extension) {

            case "jpg", "jpeg" ->
                    contentType.equals("image/jpeg");

            case "png" ->
                    contentType.equals("image/png");

            case "gif" ->
                    contentType.equals("image/gif");

            case "webp" ->
                    contentType.equals("image/webp");

            default ->
                    false;
        };
    }

    @Override
    public String generateUniqueFileName(
            String originalFileName
    ) {

        String extension = "";

        if (StringUtils.hasText(originalFileName)) {

            extension =
                    getExtension(originalFileName);

            if (!extension.isEmpty()) {
                extension =
                        "." + extension.toLowerCase();
            }
        }

        return UUID.randomUUID() + extension;
    }

    private String getExtension(
            String fileName
    ) {

        int dotIndex =
                fileName.lastIndexOf('.');

        return dotIndex >= 0
                ? fileName.substring(dotIndex + 1)
                : "";
    }

    private String sanitizeDirectory(
            String directory
    ) {

        if (!StringUtils.hasText(directory)) {
            return "misc";
        }

        String sanitized =
                directory
                        .replaceAll("[./\\\\]", "")
                        .trim();

        return StringUtils.hasText(sanitized)
                ? sanitized
                : "misc";
    }

    private String buildPublicUrl(String objectKey) {

        String publicUrl =
                r2StorageConfig.getPublicUrl();

        if (!StringUtils.hasText(publicUrl)) {

            throw new FileStorageException(
                    "R2 public URL is not configured"
            );
        }

        return publicUrl.replaceAll("/$", "")
                + "/"
                + objectKey;
    }


    private String extractObjectKey(String filePath) {

        if (!StringUtils.hasText(filePath)) {
            return filePath;
        }

        String publicUrl =
                r2StorageConfig.getPublicUrl();

        if (StringUtils.hasText(publicUrl)) {

            String normalizedPublicUrl =
                    publicUrl.replaceAll("/$", "");

            if (filePath.startsWith(
                    normalizedPublicUrl + "/"
            )) {

                return filePath.substring(
                        normalizedPublicUrl.length() + 1
                );
            }
        }

        return filePath;
    }
}