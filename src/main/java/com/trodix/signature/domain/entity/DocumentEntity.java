
package com.trodix.signature.domain.entity;

import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString

public class DocumentEntity {

    private Long id;

    @NotNull
    private UUID documentId;

    @NotNull
    private String originalFileName;

    private boolean downloaded;

    private TaskEntity task;

}
