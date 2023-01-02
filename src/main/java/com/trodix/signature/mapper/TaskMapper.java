package com.trodix.signature.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.trodix.signature.domain.entity.DocumentEntity;
import com.trodix.signature.domain.entity.TaskEntity;
import com.trodix.signature.domain.model.Document;
import com.trodix.signature.domain.model.Task;
import com.trodix.signature.domain.model.User;
import com.trodix.signature.presentation.dto.request.TaskRecipientRequest;
import com.trodix.signature.presentation.dto.response.DocumentResponse;
import com.trodix.signature.presentation.dto.response.TaskResponse;

@Mapper(componentModel = "spring", uses = { SignatureHistoryEntryMapper.class })
public interface TaskMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "recipientEmail", target = "email")
    public User taskRecipientRequestToUser(TaskRecipientRequest taskRecipientRequest);

    public List<User> taskRecipientRequestListToUserList(List<TaskRecipientRequest> taskRecipientRequestList);

    @Mapping(target = "id", ignore = true)
    public TaskEntity taskToTaskEntity(Task task);

    public Task taskEntityToTask(TaskEntity taskEntity);

    public TaskResponse taskToTaskResponse(Task task);

    public List<Task> taskEntityListToTaskList(List<TaskEntity> taskEntityList);

    public List<TaskResponse> taskListToTaskResponseList(List<Task> taskList);



    @Mapping(target = "id", ignore = true)
    public DocumentEntity documentToDocumentEntity(Document document);

    public Document documentEntityToDocument(DocumentEntity documentEntity);

    public List<Document> documentEntityListToDocumentList(List<DocumentEntity> signatureDocumentEntities);

    public List<DocumentResponse> documentModelListToDocumentResponseList(List<Document> documentList);

}
