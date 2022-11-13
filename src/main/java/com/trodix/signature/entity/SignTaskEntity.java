package com.trodix.signature.entity;

import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import org.hibernate.annotations.CreationTimestamp;
import lombok.Data;

@Entity
@Data
public class SignTaskEntity {

    @Id
    @GeneratedValue
    private Long id;

    private UUID documentId;

    private String originalFileName;

    private String senderEmail;

    private String recipientEmail;

    private LocalDate dueDate;

    @CreationTimestamp
    private LocalDate createdAt;



}
