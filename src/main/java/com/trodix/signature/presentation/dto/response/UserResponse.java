package com.trodix.signature.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.trodix.signature.domain.entity.SignTaskStatus;
import com.trodix.signature.domain.model.Document;
import com.trodix.signature.domain.model.SignatureHistoryEntry;
import com.trodix.signature.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String email;

}
