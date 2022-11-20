package com.trodix.signature.dto.request;

import org.springframework.web.multipart.MultipartFile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignRequest {

    private String reason;

    private String location;

    private String p12Password;

    private MultipartFile document;

    private MultipartFile cert;

    private Integer signPageNumber;

    private Float signXPos;

    private Float signYPos;

}
