package com.trodix.signature.dto.request;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignRequestTaskRequest {

    @RestForm
    private String reason;

    @RestForm
    private String location;

    @RestForm
    private String p12Password;

    @RestForm
    private FileUpload cert;

    @RestForm
    private Integer signPageNumber;

    @RestForm
    private Float signXPos;

    @RestForm
    private Float signYPos;

}
