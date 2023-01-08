package com.trodix.signature.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.trodix.signature.domain.entity.SignTaskStatus;
import com.trodix.signature.domain.model.SignatureHistoryEntry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private UUID taskId;

    private UserResponse initiator;

    private LocalDateTime dueDate;

    private LocalDateTime createdAt;

    private SignTaskStatus signTaskStatus;

    private List<UserResponse> taskRecipientList;

    private List<DocumentResponse> documentList;

    private List<SignatureHistoryEntry> signatureHistory;

}
