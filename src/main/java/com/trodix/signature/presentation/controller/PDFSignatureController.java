package com.trodix.signature.presentation.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import com.trodix.signature.domain.model.Document;
import com.trodix.signature.domain.model.SignRequestOptions;
import com.trodix.signature.domain.model.SignTaskStatus;
import com.trodix.signature.domain.model.Task;
import com.trodix.signature.domain.model.User;
import com.trodix.signature.domain.service.DocumentService;
import com.trodix.signature.domain.service.SignatureService;
import com.trodix.signature.domain.service.UserService;
import com.trodix.signature.mapper.TaskMapper;
import com.trodix.signature.presentation.dto.request.CreateTaskRequest;
import com.trodix.signature.presentation.dto.request.SignRequestTaskRequest;
import com.trodix.signature.presentation.dto.response.DocumentResponse;
import com.trodix.signature.presentation.dto.response.TaskResponse;
import com.trodix.signature.presentation.service.TaskService;
import com.trodix.signature.security.utils.Claims;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Scope(WebApplicationContext.SCOPE_REQUEST)
@RequestMapping("/api/sign")
@RolesAllowed({"market-customer"})
@Slf4j
@AllArgsConstructor
public class PDFSignatureController {

    private final DocumentService documentService;

    private final TaskService taskService;

    private final UserService userService;

    private final TaskMapper documentMapper;

    private final TaskMapper taskMapper;

    private final Jwt jwt;


    // @PostMapping
    // public DocumentResponse signPDF(@ModelAttribute final SignRequest signRequest) throws IOException
    // {
    //
    // final UUID documentId = UUID.randomUUID();
    //
    // final SignTaskModel signTaskModel = new SignTaskModel();
    // signTaskModel.setTmpDocument(signRequest.getDocument());
    // signTaskModel.setOriginalFileName(signRequest.getDocument().getOriginalFilename());
    // signTaskModel.setInitiator(userService.getOrCreateUser(jwt.getClaim(Claims.EMAIL.value)));
    // signTaskModel.setSignTaskRecipientList(Arrays.asList(jwt.getClaim(Claims.EMAIL.value)));
    // signTaskModel.setDueDate(null);
    // signTaskModel.setCreatedAt(LocalDateTime.now());
    // signTaskModel.setDocumentId(documentId);
    //
    // this.signService.createSignTask(signTaskModel);
    //
    // final SignRequestOptions signRequestOptions = new SignRequestOptions();
    //
    // final BouncyCastleProvider provider = new BouncyCastleProvider();
    // Security.addProvider(provider);
    // final String providerName = provider.getName();
    //
    // try {
    // final KeyStore keystore =
    // SignatureService.loadKeystore(SignatureService.multipartFileToFile(signRequest.getCert()).getAbsolutePath(),
    // signRequest.getP12Password());
    // final PrivateKey pk = SignatureService.getPrivateKey(keystore, signRequest.getP12Password(),
    // signRequestOptions.getKeyAlias());
    // final Certificate[] chain = SignatureService.getChainCertificates(keystore,
    // signRequestOptions.getKeyAlias());
    //
    // signRequestOptions.setOriginalFile(SignatureService.multipartFileToFile(signRequest.getDocument()));
    // signRequestOptions.setOriginalFileName(signRequest.getDocument().getOriginalFilename());
    // signRequestOptions.setProvider(providerName);
    // signRequestOptions.setChain(chain);
    // signRequestOptions.setPk(pk);
    // signRequestOptions.setReason(signRequest.getReason());
    // signRequestOptions.setLocation(signRequest.getLocation());
    // signRequestOptions.setSenderEmail(jwt.getClaim(Claims.EMAIL.value));
    // signRequestOptions.setSignPageNumber(signRequest.getSignPageNumber());
    // signRequestOptions.setSignXPos(signRequest.getSignXPos());
    // signRequestOptions.setSignYPos(signRequest.getSignYPos());
    // signRequestOptions.setDocumentId(documentId);
    //
    // final DocumentModel signedDocumentModel = this.signService.signPdf(signRequestOptions);
    //
    // return documentMapper.signedDocumentModelToSignedDocumentResponse(signedDocumentModel);
    // } catch (GeneralSecurityException | IOException e) {
    // e.printStackTrace();
    // }
    //
    // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to sign the document with the
    // provided data");
    // }

    @PostMapping("/{taskId}")
    public TaskResponse signPDFTask(@PathVariable(value = "taskId") final UUID taskId,
            @Valid @ModelAttribute final SignRequestTaskRequest signRequest)
            throws IOException {

        final Task signTaskModel = this.taskService.getTaskModelByTaskId(taskId);

        if (signTaskModel == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sign task not found with id: " + taskId);
        }

        if (this.taskService.isTaskSignedByRecipient(taskId, jwt.getClaim(Claims.EMAIL.value))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    jwt.getClaim(Claims.EMAIL.value) + " has already signed the task with id: " + taskId);
        }

        if (!signTaskModel.getTaskRecipientList().stream().map(User::getEmail).collect(Collectors.toList())
                .contains(jwt.getClaim(Claims.EMAIL.value))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    jwt.getClaim(Claims.EMAIL.value) + " don't match the task recipient for task with id: " + taskId);
        }

        final SignRequestOptions signRequestTaskOptions = new SignRequestOptions();

        final BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        final String providerName = provider.getName();

        try {
            final KeyStore keystore =
                    SignatureService.loadKeystore(DocumentService.multipartFileToFile(signRequest.getCert()).getAbsolutePath(), signRequest.getP12Password());
            final PrivateKey pk = SignatureService.getPrivateKey(keystore, signRequest.getP12Password(), signRequestTaskOptions.getKeyAlias());
            final Certificate[] chain = SignatureService.getChainCertificates(keystore, signRequestTaskOptions.getKeyAlias());

            signRequestTaskOptions.setTaskId(taskId);
            signRequestTaskOptions.setProvider(providerName);
            signRequestTaskOptions.setChain(chain);
            signRequestTaskOptions.setPk(pk);
            signRequestTaskOptions.setReason(signRequest.getReason());
            signRequestTaskOptions.setLocation(signRequest.getLocation());
            signRequestTaskOptions.setSignPageNumber(signRequest.getSignPageNumber());
            signRequestTaskOptions.setSignXPos(signRequest.getSignXPos());
            signRequestTaskOptions.setSignYPos(signRequest.getSignYPos());
            signRequestTaskOptions.setSenderEmail(jwt.getClaim(Claims.EMAIL.value));

            // Sign all documents associated with the task
            this.taskService.signPdfDocumentsForTask(taskId, signRequestTaskOptions);

            final Task task = taskService.getTaskModelByTaskId(taskId);

            return taskMapper.taskToTaskResponse(task);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to sign the document with the provided data");
    }

    @GetMapping(value = "/{documentId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadSignedDocument(@PathVariable(value = "documentId") final UUID documentId)
            throws IOException {

        final Document signedDocument = this.taskService.getDocumentModel(documentId);

        final Task task = this.taskService.getTaskModelByDocumentId(signedDocument.getDocumentId());

        if (SignTaskStatus.SIGNED.equals(task.getSignTaskStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Signature workflow is not completed. All recipient must sign the file.");
        }

        if (!new EqualsBuilder().append(signedDocument.getTask().getInitiator().getEmail(), jwt.getClaim(Claims.EMAIL.value)).isEquals()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    jwt.getClaim(Claims.EMAIL.value) + " don't match the task sender for documentId " + documentId);
        }

        try {
            final File document = this.documentService.getSignedDocument(signedDocument);
            final Path path = Paths.get(document.getAbsolutePath());
            final ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

            final HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + document);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } finally {
            this.taskService.markTaskDocumentsAsDownloaded(task.getTaskId());
        }

    }

    @GetMapping("/list")
    public List<DocumentResponse> getSignedDocuments() {

        final List<Document> result = this.taskService.getSignedDocuments().stream()
                .filter(i -> i.getTask().getInitiator().getEmail().equals(jwt.getClaim(Claims.EMAIL.value))).toList();
        return documentMapper.documentModelListToDocumentResponseList(result);
    }

    @GetMapping(value = "/preview/{documentId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> previewSignedDocument(@PathVariable(value = "documentId") final UUID documentId)
            throws IOException {

        final Document signedDocument = this.taskService.getDocumentModel(documentId);

        if (!new EqualsBuilder().append(signedDocument.getTask().getInitiator().getEmail(), jwt.getClaim(Claims.EMAIL.value)).isEquals()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    jwt.getClaim(Claims.EMAIL.value) + " don't match the task sender for documentId " + documentId);
        }

        final File document = this.documentService.getSignedDocument(signedDocument);
        final Path path = Paths.get(document.getAbsolutePath());
        final ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=" + document);

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @PostMapping("/task")
    public TaskResponse createSignTask(@Valid @ModelAttribute final CreateTaskRequest taskRequest) throws IOException {
        final User initiator = userService.getOrCreateUser(jwt.getClaim(Claims.EMAIL.value));
        final Task result = this.taskService.createSignTask(taskRequest, initiator);

        return this.taskMapper.taskToTaskResponse(result);
    }

    @GetMapping("/task/{taskId}")
    public TaskResponse getSignTask(@PathVariable(value = "taskId") final UUID taskId) {

        final Task task = this.taskService.getTaskModelByTaskId(taskId);

        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sign task not found for taskId " + taskId);
        }

        if (!hasAccesToTask(task, jwt)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    jwt.getClaim(Claims.EMAIL.value) + " don't match the task recipient for taskId " + taskId);
        }

        return this.taskMapper.taskToTaskResponse(task);
    }

    @GetMapping("/task/list")
    public List<TaskResponse> getTaskDocuments() {

        final List<Task> result = this.taskService.getTaskDocumentsForUser(jwt.getClaim(Claims.EMAIL.value));
        return taskMapper.taskListToTaskResponseList(result);
    }

    @GetMapping(value = "/task/preview/{documentId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> previewSignTaskDocument(@PathVariable(value = "documentId") final UUID documentId) throws IOException {

        final Task task = this.taskService.getTaskModelByDocumentId(documentId);

        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sign task not found for documentId " + documentId);
        }

        if (!hasAccesToTask(task, jwt)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    jwt.getClaim(Claims.EMAIL.value) + " don't match the task recipient for documentId " + documentId);
        }

        final Document documentModel = this.taskService.getDocumentModel(documentId);

        final File document;

        if (task.getSignTaskStatus().equals(SignTaskStatus.SIGNED)) {
            document = this.documentService.getSignedDocument(documentModel);
        } else {
            document = this.documentService.getDocument(documentModel);
        }

        final Path path = Paths.get(document.getAbsolutePath());
        final ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=" + document);
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    private boolean hasAccesToTask(final Task task, final Jwt jwt) {
        if (task.getTaskRecipientList().contains(jwt.getClaim(Claims.EMAIL.value))) {
            return true;
        } else if (new EqualsBuilder().append(task.getInitiator().getEmail(), jwt.getClaim(Claims.EMAIL.value)).isEquals()) {
            return true;
        }

        return false;
    }

}
