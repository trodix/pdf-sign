package com.trodix.signature.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import com.trodix.signature.domain.model.SignTaskStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TaskEntity {


    private Long id;

    @NotNull
    private UUID taskId;

    private UserEntity initiator;

    private LocalDateTime dueDate;

    @NotNull
    private SignTaskStatus signTaskStatus;

    private LocalDateTime createdAt;

    private List<UserEntity> taskRecipientList;

    private List<DocumentEntity> documentList;
    
    private List<SignatureHistoryEntryEntity> signatureHistory;


    public List<SignatureHistoryEntryEntity> addHistoryElement(final SignatureHistoryEntryEntity signatureHistory) {
        if (this.signatureHistory == null) {
            this.signatureHistory = new ArrayList<>();
        }
        this.signatureHistory.add(signatureHistory);
        return this.getSignatureHistory();
    }

}
