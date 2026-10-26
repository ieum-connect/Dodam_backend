package com.ieum.dodam.medical_document.infrastructure;

import com.ieum.dodam.medical_document.domain.DocumentStorageException;
import com.ieum.dodam.medical_document.domain.DocumentStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * 로컬 파일시스템에 원본을 저장하는 구현입니다. S3 어댑터는 후속 이슈에서 추가합니다.
 */
@Component
public class LocalDocumentStorage implements DocumentStoragePort {

    private static final String DEFAULT_ROOT = "./data/object-storage";

    private final Path root;
    private final String baseUrl;

    public LocalDocumentStorage(
            @Value("${dodam.storage.local-root:" + DEFAULT_ROOT + "}") String localRoot,
            @Value("${dodam.storage.public-base-url:}") String publicBaseUrl,
            @Value("${server.port:8081}") String serverPort) {
        String rootPath = StringUtils.hasText(localRoot) ? localRoot : DEFAULT_ROOT;
        this.root = Path.of(rootPath).toAbsolutePath().normalize();
        // 서버가 실제로 떠 있는 포트를 쓴다. 예전 기본값(8080)이 하드코딩돼 원본 보기가 깨져 있었다.
        String base = StringUtils.hasText(publicBaseUrl)
                ? publicBaseUrl
                : "http://localhost:" + serverPort + "/local-documents/";
        this.baseUrl = base.endsWith("/") ? base : base + "/";
    }

    @Override
    public String createSignedUrl(String storageKey, Duration ttl) {
        return baseUrl + storageKey + "?expiresAt=" + Instant.now().plus(ttl);
    }

    /** 루트 아래 storageKey 경로에 원본 파일을 씁니다. 상위 폴더가 없으면 만들고, 반환값은 없습니다. */
    @Override
    public void storeOriginal(String storageKey, byte[] content, String mimeType) {
        Path target = resolveInsideRoot(storageKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new DocumentStorageException("원본 파일을 저장하지 못했습니다.", e);
        }
    }

    /** 루트 아래 storageKey 파일을 지웁니다. 파일이 없으면 조용히 끝나고, 반환값은 없습니다. */
    @Override
    public void deleteOriginal(String storageKey) {
        try {
            Files.deleteIfExists(resolveInsideRoot(storageKey));
        } catch (IOException e) {
            throw new DocumentStorageException("원본 파일을 삭제하지 못했습니다.", e);
        }
    }

    private Path resolveInsideRoot(String storageKey) {
        Path target = toPath(storageKey);
        if (!target.startsWith(root) || target.equals(root)) {
            throw new DocumentStorageException("storageKey가 저장소 루트 밖을 가리킵니다.");
        }
        return target;
    }

    private Path toPath(String storageKey) {
        try {
            return root.resolve(storageKey).normalize();
        } catch (InvalidPathException e) {
            throw new DocumentStorageException("storageKey 형식이 올바르지 않습니다.", e);
        }
    }
}
