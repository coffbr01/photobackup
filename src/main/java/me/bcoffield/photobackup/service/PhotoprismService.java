package me.bcoffield.photobackup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.bcoffield.photobackup.dto.AlbumDTO;
import me.bcoffield.photobackup.dto.AuthResponseDTO;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class PhotoprismService {
    private static final String API = "https://%1$s/api/v1";
    private static final int NUM_ALBUMS = 12 * 100; // 100 years
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String host;
    private final String userName;
    private final String password;

    private AuthResponseDTO authResponseDTO;

    private void authenticate() {
        if (authResponseDTO != null) {
            return;
        }
        HttpPost httpPost = new HttpPost(getApiUrl() + "/session");
        String jsonBody = "{\"username\":\"" + userName + "\",\"password\":\"" + password + "\"}";
        httpPost.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            httpclient.execute(httpPost, response -> {
                authResponseDTO = MAPPER.readValue(EntityUtils.toString(response.getEntity()), AuthResponseDTO.class);
                return null;
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AlbumDTO> listAlbumsByMonth() {
        authenticate();
        HttpGet monthAlbumsGet = new HttpGet(getApiUrl() + "/albums?count=" + NUM_ALBUMS + "&offset=0&order=oldest&type=month");
        monthAlbumsGet.addHeader("X-Session-id", authResponseDTO.getSessionId());
        LocalDate now = LocalDate.now();
        List<AlbumDTO> albums = Collections.emptyList();
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            albums = httpclient.execute(monthAlbumsGet, response -> {
                String body = EntityUtils.toString(response.getEntity());
                return Arrays.stream(MAPPER.readValue(body, AlbumDTO[].class)).filter(album -> {
                    LocalDate albumDate = LocalDate.of(album.getYear(), album.getMonth(), 1);
                    Period age = albumDate.until(now);
                    int monthAge = age.getYears() * 12 + age.getMonths();
                    return monthAge >= 2;
                }).toList();
            });
            log.info("Matched {} albums", albums.size());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return albums;
    }

    public HttpGet createDownloadHttpGet(AlbumDTO albumDTO) {
        authenticate();
        HttpGet get = new HttpGet(getApiUrl() + "/albums/" + albumDTO.getUid() + "/dl?t=" + authResponseDTO.getConfig().getDownloadToken());
        get.addHeader("X-Session-id", authResponseDTO.getSessionId());
        get.addHeader("x-download-token", authResponseDTO.getConfig().getDownloadToken());
        return get;
    }

    private String getApiUrl() {
        return String.format(API, host);
    }
}
