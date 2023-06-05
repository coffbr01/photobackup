package me.bcoffield.photobackup.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.bcoffield.photobackup.dto.AlbumDTO;
import me.bcoffield.photobackup.util.KeyUtil;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@RequiredArgsConstructor
public class S3Service {
    private static final int UPLOAD_PART_SIZE = 50_000_000;

    private final String bucket;
    private final StorageClass storageClass;
    private final PhotoprismService photoprismService;

    public void uploadAlbum(AlbumDTO albumDTO) {
        try (S3AsyncClient s3Client = S3AsyncClient.builder().region(Region.US_EAST_1).build()) {
            String key = KeyUtil.getKey(albumDTO);
            log.info("Uploading {}", key);
            String uploadId;
            try {
                uploadId = s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder().bucket(bucket).key(key).storageClass(storageClass).build()).get().uploadId();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            HttpGet get = photoprismService.createDownloadHttpGet(albumDTO);
            Map<Integer, String> eTagsByPartNumber = new HashMap<>();
            try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
                httpclient.execute(get, response -> {
                    log.debug("fetch archive status: {}", response.getCode());
                    InputStream responseStream = response.getEntity().getContent();
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    CompletableFuture<UploadPartResponse> partResponseFuture = null;
                    int partNumber = 1;
                    int read = 0;
                    try {
                        while (read != -1) {
                            read = readToBuffer(responseStream, buffer);
                            if (partResponseFuture != null) {
                                eTagsByPartNumber.put(partNumber - 1, partResponseFuture.get().eTag());
                            }
                            partResponseFuture = uploadPartAsync(s3Client, key, uploadId, partNumber, buffer);
                            buffer = new ByteArrayOutputStream();
                            partNumber++;
                        }
                        eTagsByPartNumber.put(partNumber - 1, partResponseFuture.get().eTag());
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                CompletedMultipartUpload cmu = CompletedMultipartUpload.builder().parts(
                        eTagsByPartNumber.entrySet().stream().map(entry -> CompletedPart.builder().partNumber(entry.getKey()).eTag(entry.getValue()).build()).toList()
                ).build();
                CompleteMultipartUploadRequest cmur = CompleteMultipartUploadRequest.builder().bucket(bucket).key(key).uploadId(uploadId).multipartUpload(cmu).build();
                s3Client.completeMultipartUpload(cmur).get();
                log.info("Uploaded {} in {} parts", key, eTagsByPartNumber.keySet().size());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public List<String> getKeys() {
        try (S3Client s3Client = S3Client.builder().region(Region.US_EAST_1).build()) {
            ListObjectsResponse listObjectsResponse = s3Client.listObjects(ListObjectsRequest.builder().bucket(bucket).build());
            return listObjectsResponse.contents().stream().map(S3Object::key).filter(key -> key.endsWith(".zip")).toList();
        }
    }

    private CompletableFuture<UploadPartResponse> uploadPartAsync(S3AsyncClient s3Client, String key, String uploadId, int partNumber, ByteArrayOutputStream baos) {
        log.debug("Uploading part number {} size {}", partNumber, baos.size());
        UploadPartRequest upr = UploadPartRequest.builder().bucket(bucket).key(key).partNumber(partNumber).uploadId(uploadId).build();
        return s3Client.uploadPart(upr, AsyncRequestBody.fromBytes(baos.toByteArray()));
    }

    private int readToBuffer(InputStream is, ByteArrayOutputStream baos) {
        int read;
        try {
            while ((read = is.read()) != -1) {
                baos.write(read);
                if (baos.size() >= UPLOAD_PART_SIZE) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return read;
    }

}
