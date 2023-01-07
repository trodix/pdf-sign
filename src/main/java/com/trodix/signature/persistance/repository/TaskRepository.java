package com.trodix.signature.persistance.repository;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.trodix.signature.domain.entity.DocumentEntity;
import com.trodix.signature.domain.entity.SignatureHistoryEntryEntity;
import com.trodix.signature.domain.entity.TaskEntity;
import com.trodix.signature.domain.entity.UserEntity;
import com.trodix.signature.persistance.extractor.TaskEntityListResultSetExtractor;
import com.trodix.signature.persistance.extractor.TaskRecipientRelationResultSetExtractor;
import lombok.AllArgsConstructor;

@Repository
@Transactional
@AllArgsConstructor
public class TaskRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final UserRepository userRepository;

    private final DocumentRepository documentRepository;

    private final SignatureHistoryRepository historyRepository;


    public static final String TABLE_NAME = "task";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_UID = "uid";
    public static final String COLUMN_DUE_DATE = "due_date";
    public static final String COLUMN_TASK_STATUS = "task_status";
    public static final String COLUMN_CREATED_AT = "created_at";

    public static final String COLUMN_FK_USER_ID = "user_id";
    public static final String COLUMN_USER_EMAIL = "email";

    public static final String COLUMN_FK_TASK_ID = "task_id";

    protected static final String TASK_QUERY =
            """
                    SELECT
                        t.id as t_id,
                        t.uid as t_uid,
                        t.user_id as t_user_id,
                        t.due_date as t_due_date,
                        t.task_status as t_task_status,
                        t.created_at as t_created_at,
                        initiator.id as initiator_id,
                        initiator.email as initiator_email
                    FROM task t
                    INNER JOIN user_ initiator ON initiator.id = t.user_id
                    """;

    public void persist(final TaskEntity entity) {

        // persist user relation
        if (entity.getInitiator() != null) {
            final UserEntity initiator = userRepository.findByEmail(entity.getInitiator().getEmail()).orElse(null);
            if (initiator == null) {
                userRepository.persist(entity.getInitiator());
                final UserEntity createdInitiator = userRepository.findByEmail(entity.getInitiator().getEmail()).orElseThrow();
                entity.setInitiator(createdInitiator);
            } else {
                entity.setInitiator(initiator);
            }
        }

        // persist root object task
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(COLUMN_UID, entity.getTaskId());
        params.addValue(COLUMN_TASK_STATUS, entity.getSignTaskStatus().toString(), Types.VARCHAR);
        params.addValue(COLUMN_FK_USER_ID, entity.getInitiator().getId());
        params.addValue(COLUMN_DUE_DATE, entity.getDueDate());
        params.addValue(COLUMN_TASK_STATUS, entity.getSignTaskStatus());
        params.addValue(COLUMN_CREATED_AT, ObjectUtils.defaultIfNull(entity.getCreatedAt(), LocalDateTime.now()));

        if (entity.getId() == null) {
            final String query =
                    "INSERT INTO task (uid, user_id, due_date, task_status, created_at) VALUES (:uid, :user_id, :due_date, :task_status, :created_at)";
            jdbcTemplate.update(query, params);
        } else {
            params.addValue(COLUMN_ID, entity.getId());
            final String query = """
                    UPDATE task SET uid = :uid, user_id = :user_id, due_date = :due_date, task_status = :task_status, created_at = :created_at
                        WHERE id = :id
                    """;
            jdbcTemplate.update(query, params);
        }


        final TaskEntity persistedEntity = findByTaskId(entity.getTaskId()).orElseThrow();
        entity.setId(persistedEntity.getId());

        // persist relation taskRecipientList
        persistRecipientList(entity);

        // persist relation documentList
        persistDocumentList(entity);

        // persist relation signatureHistory
        if (!CollectionUtils.isEmpty(entity.getSignatureHistory())) {
            for (final SignatureHistoryEntryEntity historyEntry : entity.getSignatureHistory()) {
                if (historyEntry.getSignedBy().getId() == null) {
                    final UserEntity signedBy = userRepository.findByEmail(historyEntry.getSignedBy().getEmail()).orElseThrow();
                    historyEntry.setSignedBy(signedBy);
                }
            }
            persistSignatureHistoryEntryList(entity);
        }

    }

    public Optional<TaskEntity> findByDocumentId(final UUID documentId) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("documentId", documentId.toString());

        final String query = TASK_QUERY + """
                INNER JOIN document d ON d.task_id = t.id
                WHERE
                    d.uid = :documentId
                """;

        TaskEntity task = DaoUtils.findOne((jdbcTemplate.query(query, params, new TaskEntityListResultSetExtractor()))).orElse(null);

        if (task == null) {
            return Optional.ofNullable(null);
        }

        // add relations
        task = fetchToManyRelationsForTask(task);

        return Optional.of(task);
    }

    public Optional<TaskEntity> findById(final Long id) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);

        final String query = TASK_QUERY + """
                WHERE
                    t.id = :id
                """;

        return DaoUtils.findOne((jdbcTemplate.query(query, params, new TaskEntityListResultSetExtractor())));
    }

    public Optional<TaskEntity> findByTaskId(final UUID taskId) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("task_id", taskId.toString());

        final String query = TASK_QUERY + """
                WHERE
                    t.uid = :task_id
                """;

        TaskEntity task = DaoUtils.findOne(jdbcTemplate.query(query, params, new TaskEntityListResultSetExtractor())).orElse(null);
        if (task == null) {
            return Optional.ofNullable(null);
        }

        // add relations
        task = fetchToManyRelationsForTask(task);

        return Optional.of(task);
    }

    public List<TaskEntity> findAll() {

        final String query = TASK_QUERY;

        return jdbcTemplate.query(query, new TaskEntityListResultSetExtractor());
    }

    public List<TaskEntity> findByUserEmail(final String email) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("email", email);

        final String query = TASK_QUERY + """
                INNER JOIN task_recipient tr ON tr.task_id = t.id
                INNER JOIN user_ r  ON r.id = tr.user_id
                WHERE
                    initiator.email = :email
                AND
                    t.task_status IN ('P', 'S', 'R')
                OR
                    r.email = :email
                AND
                    t.task_status IN ('P')
                """;

        final List<TaskEntity> entities = jdbcTemplate.query(query, params, new TaskEntityListResultSetExtractor());

        if (entities != null) {
            return entities.stream().map(this::fetchToManyRelationsForTask).toList();
        }

        return Collections.emptyList();
    }

    public void deleteById(final Long id) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("task_id", id);

        // delete association task_history
        final String queryDeleteAssociationTask_History = "DELETE FROM task_history WHERE task_id = :task_id";
        jdbcTemplate.update(queryDeleteAssociationTask_History, params);

        // delete association document
        final String queryDeleteAssociationDocument = "DELETE FROM document WHERE task_id = :task_id";
        jdbcTemplate.update(queryDeleteAssociationDocument, params);

        // delete association task_recipient
        final String queryDeleteAssociationTask_Recipient = "DELETE FROM task_recipient WHERE task_id = :task_id";
        jdbcTemplate.update(queryDeleteAssociationTask_Recipient, params);

        // delete task
        final String queryDeleteTask = "DELETE FROM task WHERE id = :task_id";
        jdbcTemplate.update(queryDeleteTask, params);
    }

    public void deleteByDocumentId(final UUID documentId) {
        final TaskEntity taskEntity = findByDocumentId(documentId).orElseThrow();
        deleteById(taskEntity.getId());
    }

    /**
     * Persist recipientList in join table for task_user relation
     * 
     * @See https://javabydeveloper.com/spring-jdbctemplate-batch-update-with-maxperformance/#1-2-namedparameterjdbctemplate-batch-update-example
     * 
     * @param task The task containing the relation with its recipients
     */
    protected void persistRecipientList(final TaskEntity taskEntity) {

        deleteOldRecipientListRelations(taskEntity);

        // Add id for each recipient with no id for insert into task_recipient query
        final List<UserEntity> recipientsWithIdsPartialList = taskEntity.getTaskRecipientList().stream().filter(r -> r.getId() == null).map(recipient -> {
            UserEntity recipientInDB = userRepository.findByEmail(recipient.getEmail()).orElse(null);

            if (recipientInDB == null) {
                userRepository.persist(recipient);
                recipientInDB = userRepository.findByEmail(recipient.getEmail()).orElseThrow();
            }

            return recipientInDB;
        }).toList();

        // remove recipients with no id
        taskEntity.getTaskRecipientList().removeIf(r0 -> recipientsWithIdsPartialList.stream().map(UserEntity::getEmail).toList().contains(r0.getEmail()));

        // replace removed recipients with recipients containing an id (from database)
        taskEntity.getTaskRecipientList().addAll(recipientsWithIdsPartialList);

        final List<MapSqlParameterSource> params = new ArrayList<>();

        for (final UserEntity userEntity : taskEntity.getTaskRecipientList()) {

            final MapSqlParameterSource source = new MapSqlParameterSource();
            source.addValue(COLUMN_FK_TASK_ID, taskEntity.getId());
            source.addValue(COLUMN_FK_USER_ID, userEntity.getId());

            final String queryRelationTable = "SELECT * FROM task_recipient WHERE (task_id = :task_id AND user_id = :user_id)";
            final Pair<Long, Long> mapping = jdbcTemplate.query(queryRelationTable, source, new TaskRecipientRelationResultSetExtractor());

            if (mapping == null) {
                params.add(source);
            }            
        }

        final String query = "INSERT INTO task_recipient (task_id, user_id) values (:task_id, :user_id)";

        jdbcTemplate.batchUpdate(query, params.toArray(MapSqlParameterSource[]::new));
    }

    /**
     * Remove relations not in entity anymore from database (update entity relations)
     * 
     * @param taskEntity
     */
    protected void deleteOldRecipientListRelations(final TaskEntity taskEntity) {

        final MapSqlParameterSource deleteUserRelationQueryParams = new MapSqlParameterSource();
        deleteUserRelationQueryParams.addValue("task_id", taskEntity.getId());
        deleteUserRelationQueryParams.addValue("user_ids_keep", taskEntity.getTaskRecipientList().stream().map(UserEntity::getId).toList());
        final String deleteUserRelationQuery = """
                DELETE FROM task_recipient r
                WHERE r.task_id = :task_id
                AND
                r.user_id NOT IN (:user_ids_keep)
                """;
        jdbcTemplate.update(deleteUserRelationQuery, deleteUserRelationQueryParams);
    }

    protected void persistDocumentList(final TaskEntity taskEntity) {

        deleteOldTaskDocumentRelations(taskEntity);

        final List<MapSqlParameterSource> paramsForInsert = new ArrayList<>();
        final List<MapSqlParameterSource> paramsForUpdate = new ArrayList<>();

        for (final DocumentEntity documentEntity : taskEntity.getDocumentList()) {
            final MapSqlParameterSource source = new MapSqlParameterSource();
            source.addValue("task_id", taskEntity.getId());
            source.addValue("uid", documentEntity.getDocumentId());
            source.addValue("downloaded", documentEntity.isDownloaded());
            source.addValue("original_file_name", documentEntity.getOriginalFileName());

            if (documentEntity.getId() == null) {
                paramsForInsert.add(source);
            } else {
                source.addValue("id", documentEntity.getId());
                paramsForUpdate.add(source);
            }            
        }

        final String queryInsert = """
                INSERT INTO document (task_id, uid, downloaded, original_file_name)
                values (:task_id, :uid, :downloaded, :original_file_name)
                """;

        jdbcTemplate.batchUpdate(queryInsert, paramsForInsert.toArray(MapSqlParameterSource[]::new));

        final String queryUpdate = """
                UPDATE document SET task_id = :task_id, uid = :uid, downloaded = :downloaded, original_file_name = :original_file_name 
                    WHERE id = :id
                """;

        jdbcTemplate.batchUpdate(queryUpdate, paramsForUpdate.toArray(MapSqlParameterSource[]::new));
    }

    /**
     * Cascade remove document not anymore in task document list
     * 
     * @param taskEntity
     */
    protected void deleteOldTaskDocumentRelations(final TaskEntity taskEntity) {

        final MapSqlParameterSource deleteTaskDocumentRelationQueryParams = new MapSqlParameterSource();
        deleteTaskDocumentRelationQueryParams.addValue("task_id", taskEntity.getId());
        deleteTaskDocumentRelationQueryParams.addValue("document_ids_keep", taskEntity.getDocumentList().stream().map(DocumentEntity::getId).toList());
        final String deleteTaskDocuentRelationQuery = """
                DELETE FROM document d
                WHERE d.task_id = :task_id
                AND
                d.id NOT IN (:document_ids_keep)
                """;
        jdbcTemplate.update(deleteTaskDocuentRelationQuery, deleteTaskDocumentRelationQueryParams);
    }

    protected void persistSignatureHistoryEntryList(final TaskEntity taskEntity) {

        if (taskEntity.getSignatureHistory() == null) {
            // FIXME Should we find all historySignatureEntry for this task_id and delete them?
            return;
        }

        deleteOldSignatureHistoryEntryRelations(taskEntity);

        final List<MapSqlParameterSource> paramsInsert = new ArrayList<>();
        final List<MapSqlParameterSource> paramsUpdate = new ArrayList<>();

        for (final SignatureHistoryEntryEntity historyEntryEntity : taskEntity.getSignatureHistory()) {

            final MapSqlParameterSource source = new MapSqlParameterSource();
            source.addValue("task_id", taskEntity.getId());
            source.addValue("signed_by", historyEntryEntity.getSignedBy().getId());
            source.addValue("signed_at", historyEntryEntity.getSignedAt());

            if (historyEntryEntity.getId() == null) {
                paramsInsert.add(source);
            } else {
                paramsUpdate.add(source);
            }
        }

        final String insertQuery = "INSERT INTO task_history (task_id, signed_by, signed_at) values (:task_id, :signed_by, :signed_at)";
        jdbcTemplate.batchUpdate(insertQuery, paramsInsert.toArray(MapSqlParameterSource[]::new));

        final String updateQuery = "UPDATE task_history SET task_id = :task_id, signed_by = :signed_by, signed_at = :signed_at WHERE task_id = :task_id";
        jdbcTemplate.batchUpdate(updateQuery, paramsUpdate.toArray(MapSqlParameterSource[]::new));
    }

    /**
     * Cascade remove signature history entry not anymore in task signature history list
     * 
     * @param taskEntity
     */
    protected void deleteOldSignatureHistoryEntryRelations(final TaskEntity taskEntity) {

        if (CollectionUtils.isEmpty(taskEntity.getSignatureHistory())
                || taskEntity.getSignatureHistory().stream().filter(i -> i.getId() != null).toList().isEmpty()) {
            return;
        }

        final MapSqlParameterSource deleteTSHEntryRelationQueryParams = new MapSqlParameterSource();
        deleteTSHEntryRelationQueryParams.addValue("task_id", taskEntity.getId());

        final List<Long> historyIdsKeep =
                taskEntity.getSignatureHistory().stream().filter(i -> i.getId() != null).map(SignatureHistoryEntryEntity::getId).toList();
        deleteTSHEntryRelationQueryParams.addValue("history_ids_keep", historyIdsKeep);

        final String deleteTSHEntryRelationQuery = """
                DELETE FROM task_history h
                WHERE h.task_id = :task_id
                AND
                h.id NOT IN (:history_ids_keep)
                """;
        jdbcTemplate.update(deleteTSHEntryRelationQuery, deleteTSHEntryRelationQueryParams);
    }

    protected TaskEntity fetchToManyRelationsForTask(final TaskEntity task) {

        final List<DocumentEntity> documentList = documentRepository.findByTaskId(task.getTaskId());
        task.setDocumentList(documentList);

        final List<UserEntity> recipientList = userRepository.findAllRecipientByTaskId(task.getTaskId());
        task.setTaskRecipientList(recipientList);

        final List<SignatureHistoryEntryEntity> historyEntryList = historyRepository.findByTaskId(task.getTaskId());
        task.setSignatureHistory(historyEntryList);

        return task;
    }

}
