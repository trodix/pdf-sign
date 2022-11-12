package com.trodix.signature.dto.request;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignRequest {

    //@RestForm
    private String reason;

    //@RestForm
    private String location;

    //@RestForm
    private String p12Password;

    // @RestForm
    // private FileUpload document;

    // @RestForm
    // private FileUpload cert;

}
