package com.trodix.signature.model;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignTaskModel {

    private String originalFileName;

    private UUID documentId;

    private MultipartFile tmpDocument;

    private String senderEmail;

    private String recipientEmail;

    private LocalDateTime dueDate;

    private SignTaskStatus signTaskStatus;

    private LocalDateTime createdAt;

}
