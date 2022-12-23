package com.trodix.signature.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import com.trodix.signature.model.SignTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignTaskResponse {

    private String originalFileName;

    private UUID documentId;

    private String senderEmail;

    private String recipientEmail;

    private LocalDateTime dueDate;

    private SignTaskStatus signTaskStatus;

    private LocalDateTime createdAt;

}
