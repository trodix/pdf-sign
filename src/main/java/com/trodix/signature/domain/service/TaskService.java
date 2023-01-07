package com.trodix.signature.domain.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.trodix.signature.domain.entity.DocumentEntity;
import com.trodix.signature.domain.entity.SignatureHistoryEntryEntity;
import com.trodix.signature.domain.entity.TaskEntity;
import com.trodix.signature.domain.entity.UserEntity;
import com.trodix.signature.domain.model.Document;
import com.trodix.signature.domain.model.SignRequestOptions;
import com.trodix.signature.domain.model.SignTaskStatus;
import com.trodix.signature.domain.model.SignatureHistoryEntry;
import com.trodix.signature.domain.model.Task;
import com.trodix.signature.domain.model.User;
import com.trodix.signature.mapper.SignatureHistoryEntryMapper;
import com.trodix.signature.mapper.TaskMapper;
import com.trodix.signature.persistance.repository.DocumentRepository;
import com.trodix.signature.persistance.repository.TaskRepository;
import com.trodix.signature.presentation.dto.request.CreateTaskRequest;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TaskService {

    @Value("${app.frontend.baseurl}")
    private String frontendUrl;

    private final DocumentService documentService;

    private final EmailService emailService;

    private final UserService userService;

    private final SignatureService signatureService;

    private final TaskMapper taskMapper;

    private final SignatureHistoryEntryMapper signatureHistoryMapper;

    private final TaskRepository taskRepository;

    private final DocumentRepository documentRepository;


    public TaskService(final DocumentService documentService, final EmailService emailService, final UserService userService,
            final SignatureService signatureService, final TaskMapper taskMapper, final SignatureHistoryEntryMapper signatureHistoryMapper,
            final TaskRepository taskRepository, final DocumentRepository documentRepository) {

        this.documentService = documentService;
        this.emailService = emailService;
        this.userService = userService;
        this.signatureService = signatureService;
        this.taskMapper = taskMapper;
        this.signatureHistoryMapper = signatureHistoryMapper;
        this.taskRepository = taskRepository;
        this.documentRepository = documentRepository;
    }


    public Task createSignTask(final CreateTaskRequest taskRequest, final User user) throws IOException {

        final Task task = new Task();
        task.setTaskId(UUID.randomUUID());
        task.setInitiator(user);
        task.setDueDate(taskRequest.getDueDate());
        task.setSignTaskStatus(SignTaskStatus.IN_PROGRESS);
        task.setTaskRecipientList(taskMapper.taskRecipientRequestListToUserList(taskRequest.getRecipientList()));

        for (final MultipartFile tmpFile : taskRequest.getDocumentList()) {
            final UUID documentId = UUID.randomUUID();
            this.documentService.storeFile(tmpFile, documentId);
            final Document document = new Document();
            document.setDocumentId(documentId);
            document.setOriginalFileName(tmpFile.getOriginalFilename());

            task.addDocument(document);
        }

        final TaskEntity entity = this.taskMapper.taskToTaskEntity(task);
        taskRepository.persist(entity);
        final TaskEntity result = taskRepository.findByTaskId(task.getTaskId()).orElseThrow();

        for (final UserEntity recipient : result.getTaskRecipientList()) {

            final String to = recipient.getEmail();
            final String subject = "[Sign PDF] New Sign request";
            final Map<String, Object> tpl = new HashMap<>();
            tpl.put("initiatorEmail", result.getInitiator().getEmail());
            tpl.put("documentList", result.getDocumentList());

            final String taskUrl = frontendUrl + "/tasks/" + result.getTaskId();
            tpl.put("taskUrl", taskUrl);

            try {
                this.emailService.sendNewSignTaskEmailNotification(to, subject, tpl);
            } catch (IOException | TemplateException | MessagingException e) {
                log.error("Error while sending email notification", e);
            }

        }

        return this.taskMapper.taskEntityToTask(result);
    }

    public Task getTaskModelByTaskId(final UUID taskId) {
        final TaskEntity entity = taskRepository.findByTaskId(taskId).orElseThrow();

        return taskMapper.taskEntityToTask(entity);
    }

    public Task getTaskModelByDocumentId(final UUID documentId) {
        final TaskEntity entity = taskRepository.findByDocumentId(documentId).orElseThrow();

        return taskMapper.taskEntityToTask(entity);
    }

    public Task registerSignedTask(final UUID taskId, final User signedBy) {

        final TaskEntity taskEntity = taskRepository.findByTaskId(taskId).orElseThrow();
        final SignatureHistoryEntry signatureHistoryEntry = new SignatureHistoryEntry(signedBy, LocalDateTime.now());

        taskEntity.addHistoryElement(signatureHistoryMapper.signatureHistoryEntryToSignatureHistoryEntryEntity(signatureHistoryEntry));

        if (isTaskSignedByAllRecipients(taskEntity)) {
            taskEntity.setSignTaskStatus(SignTaskStatus.SIGNED);
        }

        taskRepository.persist(taskEntity);

        final Task task = taskMapper.taskEntityToTask(taskEntity);

        final String to = task.getInitiator().getEmail();
        final String subject = "[Sign PDF] Your files has been signed";
        final Map<String, Object> tpl = new HashMap<>();
        tpl.put("task", task);

        final String downloadUrl = frontendUrl + "/tasks/" + taskId;
        tpl.put("downloadUrl", downloadUrl);

        try {
            this.emailService.sendSignedDocumentEmailNotification(to, subject, tpl);
        } catch (IOException | TemplateException | MessagingException e) {
            log.error("Error while sending email notification", e);
        }

        return task;
    }

    public void markTaskDocumentsAsDownloaded(final UUID taskId) {
        final TaskEntity taskEntity = taskRepository.findByTaskId(taskId).orElseThrow();

        for (final DocumentEntity documentEntity : taskEntity.getDocumentList()) {
            documentEntity.setDownloaded(true);
        }

        taskEntity.setSignTaskStatus(SignTaskStatus.DOWNLOADED);
        taskRepository.persist(taskEntity);

        downloadedSignedDocumentsCleaner();

        for (final UserEntity recipient : taskEntity.getTaskRecipientList()) {

            final String to = recipient.getEmail();
            final String subject = "[Sign PDF] Your files has been downloaded";
            final Map<String, Object> tpl = new HashMap<>();
            tpl.put("task", taskEntity);

            try {
                this.emailService.sendDownloadedDocumentEmailNotification(to, subject, tpl);
            } catch (IOException | TemplateException | MessagingException e) {
                log.error("Error while sending email notification", e);
            }

        }
    }

    public void signPdfDocumentsForTask(final UUID taskId, final SignRequestOptions signRequestOptions) {
        final Task taskModel = getTaskModelByTaskId(taskId);

        for (final Document document : taskModel.getDocumentList()) {
            try {
                this.signatureService.signPdf(signRequestOptions, document);
            } catch (final GeneralSecurityException e) {
                e.printStackTrace();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        registerSignedTask(taskId, userService.getOrCreateUser(signRequestOptions.getSenderEmail()));
    }

    public boolean isTaskSignedByAllRecipients(final TaskEntity task) {

        final List<String> a = task.getSignatureHistory().stream().map(h -> h.getSignedBy().getEmail()).toList();
        final List<String> b = task.getTaskRecipientList().stream().map(UserEntity::getEmail).toList();

        return CollectionUtils.containsAll(a, b);
    }

    public boolean isTaskSignedByRecipient(final UUID taskId, final String recipientEmail) {

        final TaskEntity taskEntity = taskRepository.findByTaskId(taskId).orElseThrow();

        final List<SignatureHistoryEntryEntity> history = taskEntity.getSignatureHistory();

        return history.stream().map(h -> h.getSignedBy().getEmail()).collect(Collectors.toList()).contains(recipientEmail);
    }

    public void deleteTask(final UUID documentId) {
        try {
            final Document document = getDocumentModel(documentId);
            taskRepository.deleteByDocumentId(documentId);
            documentService.deleteOriginalDocument(document);
            documentService.deleteSignedDocument(document);
        } catch (final RuntimeException e) {
            log.error("Error while deleting task {}", documentId, e);
        }
    }

    public List<Document> getSignedDocuments() {
        final List<DocumentEntity> entities = documentRepository.findAll();
        return taskMapper.documentEntityListToDocumentList(entities);
    }

    public List<Task> getTaskDocumentsForUser(final String email) {
        final List<TaskEntity> entities = taskRepository.findByUserEmail(email);
        return taskMapper.taskEntityListToTaskList(entities);
    }

    public Document getDocumentModel(final UUID documentId) {
        final DocumentEntity entity = documentRepository.findByDocumentId(documentId).orElseThrow();

        return taskMapper.documentEntityToDocument(entity);
    }

    protected void downloadedSignedDocumentsCleaner() {
        final List<DocumentEntity> downloadedDocuments = documentRepository.findByDownloaded(true);
        downloadedDocuments.forEach(document -> deleteTask(document.getDocumentId()));
    }


}
