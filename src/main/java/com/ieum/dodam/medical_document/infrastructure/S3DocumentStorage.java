package com.ieum.dodam.medical_document.infrastructure;

import com.ieum.dodam.medical_document.domain.DocumentStorageException;
import com.ieum.dodam.medical_document.domain.DocumentStoragePort;
import com.ieum.dodam.medical_document.domain.OriginalDocumentStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(S3DocumentStorage.class);
    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    @org.springframework.beans.factory.annotation.Autowired
    public S3DocumentStorage(@Value("${dodam.storage.s3.bucket:}") String bucket,
                             @Value("${dodam.storage.s3.region:}") String region) {
        this(requireSetting(bucket, "DODAM_STORAGE_S3_BUCKET"),
                Region.of(requireSetting(region, "DODAM_STORAGE_S3_REGION")));
    }

    private S3DocumentStorage(String bucket, Region region) {
        this(bucket, S3Client.builder().region(region)
                        .httpClientBuilder(UrlConnectionHttpClient.builder()).build(),
                S3Presigner.builder().region(region).build());
    }

    S3DocumentStorage(String bucket, S3Client client, S3Presigner presigner) {
        this.bucket = requireSetting(bucket, "DODAM_STORAGE_S3_BUCKET");
        this.client = client;
        this.presigner = presigner;
    }

    private static String requireSetting(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required when DODAM_STORAGE_PROVIDER=s3");
        }
        return value.strip();
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
        catch (RuntimeException e) {
            if (e instanceof S3Exception s3 && isRegionError(s3)) {
                log.warn("S3 원본 조회 실패. DODAM_STORAGE_S3_REGION이 버킷 리전과 일치하는지 확인해 주세요 (HTTP {}).",
                        s3.statusCode());
            }
            throw new DocumentStorageException("원본 파일을 읽지 못했습니다.", e);
        }
    }

    private static boolean isRegionError(S3Exception error) {
        if (error.statusCode() == 301 || error.statusCode() == 307) {
            return true;
        }
        String code = error.awsErrorDetails() == null ? null : error.awsErrorDetails().errorCode();
        return "AuthorizationHeaderMalformed".equals(code) || "IncorrectEndpoint".equals(code);
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
