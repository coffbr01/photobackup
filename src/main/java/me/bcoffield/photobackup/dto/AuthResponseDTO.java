package me.bcoffield.photobackup.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthResponseDTO {
    @JsonAlias("id")
    private String sessionId;

    private AuthResponseConfigDTO config;

}
