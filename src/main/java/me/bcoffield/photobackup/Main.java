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
        String host = System.getenv("PBKP_HOST");
        String userName = System.getenv("PBKP_USER");
        String password = System.getenv("PBKP_PASS");
        String bucket = System.getenv("PBKP_BUCKET");
        String storageClass = System.getenv("PBKP_STORAGE_CLASS");
        PhotoprismService photoprismService = new PhotoprismService(host, userName, password);
        List<AlbumDTO> albums = photoprismService.listAlbumsByMonth();
        S3Service s3Service = new S3Service(bucket, StorageClass.fromValue(storageClass), photoprismService);
        List<String> keys = s3Service.getKeys();
        albums.forEach(album -> {
            String key = KeyUtil.getKey(album);
            if (!keys.contains(key)) {
                s3Service.uploadAlbum(album);
            } else {
                log.info("{} already exists, skipping", key);
            }
        });
    }

}