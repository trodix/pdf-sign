package com.trodix.signature.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.trodix.signature.domain.entity.SignTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Task {

    private UUID taskId;

    private User initiator;

    private LocalDateTime dueDate;

    private LocalDateTime createdAt;

    private SignTaskStatus signTaskStatus;

    private List<User> taskRecipientList;

    private List<Document> documentList;

    private List<SignatureHistoryEntry> signatureHistory;


    public List<Document> addDocument(final Document document) {
        if (this.getDocumentList() == null) {
            this.setDocumentList(new ArrayList<>());
        }
        this.getDocumentList().add(document);
        return this.getDocumentList();
    }

}
