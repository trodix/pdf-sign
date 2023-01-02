package com.trodix.signature.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignatureHistoryEntry {

    private User signedBy;

    private LocalDateTime signedAt;

}
