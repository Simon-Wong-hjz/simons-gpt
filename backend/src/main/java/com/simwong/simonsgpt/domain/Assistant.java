package com.simwong.simonsgpt.domain;//package com.simhuang.simonsgpt.domain;
//
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.util.List;
//
//@NoArgsConstructor
//@Data
//public class Assistant {
//
//    @JsonProperty("id")
//    private String id;
//    @JsonProperty("object")
//    private String object;
//    @JsonProperty("created_at")
//    private Integer createdAt;
//    @JsonProperty("name")
//    private String name;
//    @JsonProperty("description")
//    private Object description;
//    @JsonProperty("model")
//    private String model;
//    @JsonProperty("instructions")
//    private String instructions;
//    @JsonProperty("tools")
//    private List<Tools> tools;
//    @JsonProperty("file_ids")
//    private List<?> fileIds;
//
//    @NoArgsConstructor
//    @Data
//    public static class Tools {
//        @JsonProperty("type")
//        private String type;
//    }
//}
