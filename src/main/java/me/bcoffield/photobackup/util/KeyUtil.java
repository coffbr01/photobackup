package me.bcoffield.photobackup.util;

import me.bcoffield.photobackup.dto.AlbumDTO;

public class KeyUtil {
    public static String getKey(AlbumDTO albumDTO) {
        return albumDTO.getYear() + "/" + albumDTO.getMonth() + "/archive.zip";
    }
}
