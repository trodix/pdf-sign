package com.trodix.signature.presentation.dto.request;

import org.springframework.web.multipart.MultipartFile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignRequestTaskRequest {

    @NotBlank
    private String reason;

    @NotBlank
    private String location;

    @NotBlank
    private String p12Password;

    @NotNull
    private MultipartFile cert;

    private Integer signPageNumber;

    private Float signXPos;

    private Float signYPos;

}
