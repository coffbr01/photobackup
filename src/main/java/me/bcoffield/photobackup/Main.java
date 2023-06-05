package me.bcoffield.photobackup;

import lombok.extern.slf4j.Slf4j;
import me.bcoffield.photobackup.dto.AlbumDTO;
import me.bcoffield.photobackup.service.PhotoprismService;
import me.bcoffield.photobackup.service.S3Service;
import me.bcoffield.photobackup.util.KeyUtil;
import software.amazon.awssdk.services.s3.model.StorageClass;

import java.util.List;

@Slf4j
public class Main {
    public static void main(String[] args) {
        new Main().start();
    }

    public void start() {
        String host = getRequiredEnvVar("PBKP_HOST");
        String userName = getRequiredEnvVar("PBKP_USER");
        String password = getRequiredEnvVar("PBKP_PASS");
        String bucket = getRequiredEnvVar("PBKP_BUCKET");
        String storageClass = getRequiredEnvVar("PBKP_STORAGE_CLASS");

        PhotoprismService photoprismService = new PhotoprismService(host, userName, password);
        List<AlbumDTO> albums = photoprismService.listAlbumsByMonth();
        S3Service s3Service = new S3Service(bucket, StorageClass.fromValue(storageClass), photoprismService);
        List<String> keys = s3Service.getKeys();
        albums.forEach(album -> {
            String key = KeyUtil.getKey(album);
            if (keys.contains(key)) {
                log.info("{} already exists, skipping", key);
            } else {
                s3Service.uploadAlbum(album);
            }
        });
        log.info("Done!");
    }

    private String getRequiredEnvVar(String envVar) {
        String result = System.getenv(envVar);
        if (result == null || "".equals(result)) {
            log.error("Required env var {} was not set", envVar);
            System.exit(1);
        }
        return result;
    }

}