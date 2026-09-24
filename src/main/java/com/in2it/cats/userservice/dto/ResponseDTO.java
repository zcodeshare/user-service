package com.in2it.cats.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDTO {

    @JsonProperty("is_success")
    private boolean isSuccess;

    private Object data;

    @JsonProperty("error_info")
    private CustomErrorResponseDTO errorInfo;
}