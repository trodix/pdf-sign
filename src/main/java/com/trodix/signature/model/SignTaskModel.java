package com.trodix.signature.model;

import java.time.LocalDate;
import java.util.UUID;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignTaskModel {

    private String originalFileName;

    private UUID documentId;

    private FileUpload tmpDocument;

    private String senderEmail;

    private String recipientEmail;

    private LocalDate dueDate;

    private LocalDate createdAt;

}
