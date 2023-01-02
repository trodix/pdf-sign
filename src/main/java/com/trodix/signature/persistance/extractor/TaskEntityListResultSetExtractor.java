package com.trodix.signature.persistance.extractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.lang.Nullable;
import com.trodix.signature.domain.entity.DocumentEntity;
import com.trodix.signature.domain.entity.SignatureHistoryEntryEntity;
import com.trodix.signature.domain.entity.TaskEntity;
import com.trodix.signature.domain.entity.UserEntity;
import com.trodix.signature.domain.model.SignTaskStatus;

public class TaskEntityListResultSetExtractor implements ResultSetExtractor<List<TaskEntity>> {

    @Override
    @Nullable
    public List<TaskEntity> extractData(final ResultSet rs) throws SQLException, DataAccessException {

        // FIXME Use multiple queries for *-to-many relationships :
        // // @See https://stackoverflow.com/a/54115466/10315605 and
        // // @See https://medium.com/@benmorel/to-join-or-not-to-join-bba9c1377c10

        final List<TaskEntity> taskList = new ArrayList<>();

        while (rs.next()) {
            final UserEntity initiator = new UserEntity();
            initiator.setId(rs.getLong("initiator_id"));
            initiator.setEmail(rs.getString("initiator_email"));


            final TaskEntity entity = new TaskEntity();
            entity.setId(rs.getLong("t_id"));
            entity.setTaskId(UUID.fromString(rs.getString("t_uid")));
            entity.setInitiator(initiator);
            entity.setDueDate(rs.getTimestamp("t_due_date").toLocalDateTime());
            entity.setSignTaskStatus(SignTaskStatus.fromValue(rs.getString("t_task_status")));
            entity.setCreatedAt(rs.getTimestamp("t_created_at").toLocalDateTime());

            final UserEntity taskRecipient = new UserEntity();
            taskRecipient.setId(rs.getLong("recipient_id"));
            taskRecipient.setEmail(rs.getString("recipient_email"));

            // FIXME d_uid is null in rs for an unknown reason
            DocumentEntity tmpDocument = null;
            if (rs.getString("d_uid") != null) {
                tmpDocument = new DocumentEntity();
                tmpDocument.setId(rs.getLong("d_id"));
                tmpDocument.setDocumentId(UUID.fromString(rs.getString("d_uid")));
                tmpDocument.setOriginalFileName(rs.getString("d_original_file_name"));
                tmpDocument.setDownloaded(rs.getBoolean("d_downloaded"));
            }
            final DocumentEntity document = tmpDocument;

            // 0L is a null value in database (no existing relation)
            SignatureHistoryEntryEntity tmpHistoryEntry = null;
            if (rs.getLong("th_id") > 0L) {
                tmpHistoryEntry = new SignatureHistoryEntryEntity();
                tmpHistoryEntry.setId(rs.getLong("th_id"));
                tmpHistoryEntry.setSignedAt(rs.getTimestamp("th_signed_at").toLocalDateTime());
            }
            final SignatureHistoryEntryEntity historyEntry = tmpHistoryEntry;

            // 0L is a null value in database (no existing relation)
            if (historyEntry != null && rs.getLong("signee_id") > 0L) {
                final UserEntity signedBy = new UserEntity();
                signedBy.setId(rs.getLong("signee_id"));
                signedBy.setEmail(rs.getString("signee_email"));
                historyEntry.setSignedBy(signedBy);
            }

            // Remove duplicate rows as the sql query contains joins with no distinct
            if (!taskList.stream().map(TaskEntity::getId).toList().contains(entity.getId())) {
                taskList.add(entity);
            }

            // Add recipientList items (relation)
            CollectionUtils.transform(taskList, new Transformer<TaskEntity, TaskEntity>() {

                @Override
                public TaskEntity transform(final TaskEntity input) {
                    if (input.getId().equals(entity.getId())) {

                        // Populate recipient relation in taskEntity
                        if (input.getTaskRecipientList() == null) {
                            input.setTaskRecipientList(new ArrayList<>());
                        }
                        input.getTaskRecipientList().add(taskRecipient);

                        // Populate document relation in taskEntity
                        if (input.getDocumentList() == null) {
                            input.setDocumentList(new ArrayList<>());
                        }
                        input.getDocumentList().add(document);

                        // Populate signatureHistory relation
                        if (input.getSignatureHistory() == null) {
                            input.setSignatureHistory(new ArrayList<>());
                        }
                        if (historyEntry != null) {
                            input.getSignatureHistory().add(historyEntry);
                        }

                    }
                    return input;
                }

            });
        }

        return taskList;
    }

}
