package com.ieum.dodam.medical_document.infrastructure;

import com.ieum.dodam.medical_document.domain.DocumentStorageException;
import com.ieum.dodam.medical_document.domain.DocumentStoragePort;
import com.ieum.dodam.medical_document.domain.OriginalDocumentStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import jakarta.annotation.PreDestroy;
import java.time.Duration;

/** Private objects; credentials come from the EC2 instance role, never source code. */
@Component
@ConditionalOnProperty(name = "dodam.storage.provider", havingValue = "s3")
public class S3DocumentStorage implements DocumentStoragePort, OriginalDocumentStoragePort {
    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    @org.springframework.beans.factory.annotation.Autowired
    public S3DocumentStorage(@Value("${dodam.storage.s3.bucket}") String bucket,
                             @Value("${dodam.storage.s3.region:ap-southeast-2}") String region) {
        this(bucket, S3Client.builder().region(Region.of(region))
                        .httpClientBuilder(UrlConnectionHttpClient.builder()).build(),
                S3Presigner.builder().region(Region.of(region)).build());
    }

    S3DocumentStorage(String bucket, S3Client client, S3Presigner presigner) {
        if (bucket == null || bucket.isBlank()) throw new IllegalArgumentException("S3 bucket is required");
        this.bucket = bucket;
        this.client = client;
        this.presigner = presigner;
    }

    static String key(String key) {
        if (key == null || key.isBlank() || key.startsWith("/") || key.contains("\\") || key.contains("\0"))
            throw new DocumentStorageException("storageKey 형식이 올바르지 않습니다.");
        for (String segment : key.split("/", -1))
            if (segment.isEmpty() || segment.equals(".") || segment.equals(".."))
                throw new DocumentStorageException("storageKey 형식이 올바르지 않습니다.");
        return key;
    }

    @Override public void storeOriginal(String storageKey, byte[] content, String mimeType) {
        String key = key(storageKey);
        try {
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(mimeType)
                    .serverSideEncryption(ServerSideEncryption.AES256).build(), RequestBody.fromBytes(content));
        } catch (RuntimeException e) { throw new DocumentStorageException("원본 파일을 저장하지 못했습니다.", e); }
    }

    @Override public byte[] readOriginal(String storageKey) {
        String key = key(storageKey);
        try { return client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray(); }
        catch (RuntimeException e) { throw new DocumentStorageException("원본 파일을 읽지 못했습니다.", e); }
    }

    @Override public void deleteOriginal(String storageKey) {
        String key = key(storageKey);
        try { client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build()); }
        catch (RuntimeException e) { throw new DocumentStorageException("원본 파일을 삭제하지 못했습니다.", e); }
    }

    @Override public String createSignedUrl(String storageKey, Duration ttl) {
        String key = key(storageKey);
        if (ttl == null || ttl.isNegative() || ttl.isZero() || ttl.compareTo(Duration.ofHours(1)) > 0)
            throw new DocumentStorageException("서명 URL 유효기간이 올바르지 않습니다.");
        try {
            return presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(ttl)
                    .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build()).build()).url().toString();
        } catch (RuntimeException e) { throw new DocumentStorageException("문서 URL을 발급하지 못했습니다.", e); }
    }

    @PreDestroy public void close() { presigner.close(); client.close(); }
}
