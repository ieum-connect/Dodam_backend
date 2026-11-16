package com.ieum.dodam.medical_document.infrastructure;

import com.ieum.dodam.medical_document.domain.DocumentStorageException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class S3DocumentStorageTest {
    @Test void storesEncryptedObjectAndReadsSameKey() {
        S3Client client = mock(S3Client.class);
        S3DocumentStorage storage = new S3DocumentStorage("private-docs", client, mock(S3Presigner.class));
        byte[] bytes = {1, 2, 3};
        storage.storeOriginal("docs/original.pdf", bytes, "application/pdf");
        verify(client).putObject(argThat((PutObjectRequest r) -> r.bucket().equals("private-docs")
            && r.key().equals("docs/original.pdf") && r.serverSideEncryption() == ServerSideEncryption.AES256
            && r.acl() == null && r.contentType().equals("application/pdf")), any(RequestBody.class));
        when(client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes));
        assertArrayEquals(bytes, storage.readOriginal("docs/original.pdf"));
        verify(client).getObjectAsBytes(argThat((GetObjectRequest r) -> r.bucket().equals("private-docs") && r.key().equals("docs/original.pdf")));
    }

    @Test void signsBoundedGetUrlWithoutNetwork() {
        try (S3Presigner signer = S3Presigner.builder().region(Region.AP_SOUTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))).build()) {
            S3DocumentStorage storage = new S3DocumentStorage("private-docs", mock(S3Client.class), signer);
            String url = storage.createSignedUrl("rendered/page.png", Duration.ofMinutes(5));
            assertTrue(url.startsWith("https://private-docs.s3.ap-southeast-2.amazonaws.com/rendered/page.png?"));
            assertTrue(url.contains("X-Amz-Expires=300"));
            assertThrows(DocumentStorageException.class, () -> storage.createSignedUrl("a", Duration.ofDays(1)));
        }
    }

    @Test void invalidKeysNeverReachS3() {
        S3Client client = mock(S3Client.class);
        S3DocumentStorage storage = new S3DocumentStorage("private-docs", client, mock(S3Presigner.class));
        for (String key : new String[]{"", "/a", "../a", "a/../b", "a//b", "a\\b", "a\0b"})
            assertThrows(DocumentStorageException.class, () -> storage.readOriginal(key));
        verifyNoInteractions(client);
    }

    @Test void storageErrorsAreWrapped() {
        S3Client client = mock(S3Client.class);
        S3DocumentStorage storage = new S3DocumentStorage("private-docs", client, mock(S3Presigner.class));
        when(client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());
        assertThrows(DocumentStorageException.class, () -> storage.readOriginal("missing.pdf"));
    }
}
