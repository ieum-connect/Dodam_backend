package com.ieum.dodam.medical_document.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentStorageConfigurationTest {
    private final ApplicationContextRunner storage = new ApplicationContextRunner()
            .withUserConfiguration(S3DocumentStorage.class)
            .withPropertyValues("dodam.storage.provider=s3");

    @Test
    void s3RequiresBucketOnStartup() {
        storage.withPropertyValues("dodam.storage.s3.region=ap-northeast-2")
                .run(context -> assertThat(context.getStartupFailure()).hasRootCauseMessage(
                        "DODAM_STORAGE_S3_BUCKET is required when DODAM_STORAGE_PROVIDER=s3"));
    }

    @Test
    void s3RequiresAnExplicitRegionOnStartup() {
        storage.withPropertyValues("dodam.storage.s3.bucket=private-docs")
                .run(context -> assertThat(context.getStartupFailure()).hasRootCauseMessage(
                        "DODAM_STORAGE_S3_REGION is required when DODAM_STORAGE_PROVIDER=s3"));
    }

    @Test
    void s3AcceptsExplicitBucketAndRegion() {
        storage.withPropertyValues("dodam.storage.s3.bucket=private-docs", "dodam.storage.s3.region=ap-northeast-2")
                .run(context -> assertThat(context).hasSingleBean(S3DocumentStorage.class));
    }

    @Test
    void localFilesAreServedOnlyInLocalMode(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("sample.txt"), "local document");
        WebApplicationContextRunner web = new WebApplicationContextRunner()
                .withUserConfiguration(Mvc.class, LocalDocumentResourceConfig.class)
                .withPropertyValues("dodam.storage.local-root=" + root);

        web.run(context -> MockMvcBuilders.webAppContextSetup(context).build()
                .perform(get("/local-documents/sample.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("local document")));
        web.withPropertyValues("dodam.storage.provider=s3")
                .run(context -> MockMvcBuilders.webAppContextSetup(context).build()
                        .perform(get("/local-documents/sample.txt"))
                        .andExpect(status().isNotFound()));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    static class Mvc {}
}
