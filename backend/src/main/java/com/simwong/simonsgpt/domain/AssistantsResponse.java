package com.simwong.simonsgpt.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.simhuang.simonsgpt.model.Assistant;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class AssistantsResponse {
    @JsonProperty("object")
    private String object;
    @JsonProperty("data")
    private List<Assistant> data;
    @JsonProperty("first_id")
    private String firstId;
    @JsonProperty("last_id")
    private String lastId;
    @JsonProperty("has_more")
    private Boolean hasMore;
}
