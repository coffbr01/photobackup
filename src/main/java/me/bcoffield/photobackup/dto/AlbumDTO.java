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
public class AlbumDTO {
    @JsonAlias("UID")
    private String uid;
    @JsonAlias("Title")
    private String title;
    @JsonAlias("Year")
    private Integer year;
    @JsonAlias("Month")
    private Integer month;
}
