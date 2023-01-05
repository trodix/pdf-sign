package com.trodix.signature.persistance.repository;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.trodix.signature.domain.entity.DocumentEntity;
import com.trodix.signature.domain.entity.SignatureHistoryEntryEntity;
import com.trodix.signature.domain.entity.TaskEntity;
import com.trodix.signature.domain.entity.UserEntity;
import com.trodix.signature.persistance.extractor.TaskEntityListResultSetExtractor;
import lombok.AllArgsConstructor;

@Repository
@Transactional
@AllArgsConstructor
public class TaskRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final UserRepository userRepository;


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
                        initiator.email as initiator_email,
                        d.id as d_id,
                        d.task_id as d_task_id,
                        d.uid as d_uid,
                        d.downloaded as d_downloaded,
                        d.original_file_name as d_original_file_name,
                        th.id as th_id,
                        th.task_id as th_task_id,
                        th.signed_by as th_signed_by,
                        th.signed_at as th_signed_at,
                        tr.user_id as tr_user_id,
                        tr.task_id as tr_task_id,
                        recipient.id as recipient_id,
                        recipient.email as recipient_email,
                        signee.id as signee_id,
                        signee.email as signee_email
                    FROM task t
                    LEFT JOIN user_ initiator ON initiator.id = t.user_id
                    LEFT JOIN document d ON d.task_id = t.id
                    LEFT JOIN task_history th on th.task_id = t.id
                    LEFT JOIN task_recipient tr on tr.task_id  = t.id
                    LEFT JOIN user_ recipient on recipient.id = tr.user_id
                    LEFT JOIN user_ signee on signee.id = th.signed_by
                    """;

    public void persist(final TaskEntity entity) {

        // persist user relation
        if (entity.getInitiator() != null) {
            final UserEntity initiator = userRepository.findByEmail(entity.getInitiator().getEmail()).orElse(null);
            if (initiator == null) {
                userRepository.persist(entity.getInitiator());
                final UserEntity createdInitiator = userRepository.findByEmail(entity.getInitiator().getEmail()).orElseThrow();
                entity.setInitiator(createdInitiator);
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

        final String query = "INSERT INTO task (uid, user_id, due_date, task_status, created_at) VALUES (:uid, :user_id, :due_date, :task_status, :created_at)";
        jdbcTemplate.update(query, params);

        final TaskEntity persistedEntity = findByTaskId(entity.getTaskId()).orElseThrow();
        entity.setId(persistedEntity.getId());

        // persist relation taskRecipientList
        persistRecipientList(entity);

        // persist relation documentList
        persistDocumentList(entity);

        // persist relation signatureHistory
        persistSignatureHistoryEntryList(entity);

    }

    public Optional<TaskEntity> findByDocumentId(final UUID documentId) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("documentId", documentId.toString());

        final String query = TASK_QUERY + """
                WHERE
                    d.uid = :documentId
                """;

        return DaoUtils.findOne((jdbcTemplate.query(query, params, new TaskEntityListResultSetExtractor())));
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

        return DaoUtils.findOne(jdbcTemplate.query(query, params, new TaskEntityListResultSetExtractor()));
    }

    public List<TaskEntity> findAll() {

        final String query = TASK_QUERY;

        return jdbcTemplate.query(query, new TaskEntityListResultSetExtractor());
    }

    public List<TaskEntity> findByUserEmail(final String email) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("email", email);

        final String query = TASK_QUERY + """
                WHERE
                    u.email = :email
                AND
                    t.task_status IN ('IN_PROGRESS', 'SIGNED', 'REJECTED')
                """;

        return jdbcTemplate.query(query, params, new TaskEntityListResultSetExtractor());
    }

    public void deleteById(final Long id) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);

        final String query = "DELETE FROM task t WHERE t.id = :id";

        jdbcTemplate.update(query, params);
    }

    public void deleteByDocumentId(final UUID documentId) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("uid", documentId);

        final String query = """
                DELETE FROM task t
                INNER JOIN document d ON d.task_id = t.id
                WHERE d.uid = :uid
                """;

        jdbcTemplate.update(query, params);
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

            params.add(source);
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

        final List<MapSqlParameterSource> params = new ArrayList<>();

        for (final DocumentEntity documentEntity : taskEntity.getDocumentList()) {
            final MapSqlParameterSource source = new MapSqlParameterSource();
            source.addValue("task_id", taskEntity.getId());
            source.addValue("uid", documentEntity.getDocumentId());
            source.addValue("downloaded", documentEntity.isDownloaded());
            source.addValue("original_file_name", documentEntity.getOriginalFileName());

            params.add(source);
        }

        final String query = """
                INSERT INTO document (task_id, uid, downloaded, original_file_name)
                values (:task_id, :uid, :downloaded, :original_file_name)
                """;

        jdbcTemplate.batchUpdate(query, params.toArray(MapSqlParameterSource[]::new));
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

        final List<MapSqlParameterSource> params = new ArrayList<>();

        for (final SignatureHistoryEntryEntity historyEntryEntity : taskEntity.getSignatureHistory()) {
            final MapSqlParameterSource source = new MapSqlParameterSource();
            source.addValue("id", historyEntryEntity.getId());
            source.addValue("task_id", taskEntity.getId());
            source.addValue("signedBy", historyEntryEntity.getSignedBy());
            source.addValue("signedAt", historyEntryEntity.getSignedAt());

            params.add(source);
        }

        final String query =
                "INSERT INTO task_history (id, task_id, signedBy, signedAt) values (:id, :task_id, :signedBy, :signedAt)";

        jdbcTemplate.batchUpdate(query, params.toArray(MapSqlParameterSource[]::new));
    }

    /**
     * Cascade remove signature history entry not anymore in task signature history list
     * 
     * @param taskEntity
     */
    protected void deleteOldSignatureHistoryEntryRelations(final TaskEntity taskEntity) {

        if (CollectionUtils.isEmpty(taskEntity.getSignatureHistory())) {
            return;
        }

        final MapSqlParameterSource deleteTSHEntryRelationQueryParams = new MapSqlParameterSource();
        deleteTSHEntryRelationQueryParams.addValue("task_id", taskEntity.getId());
        deleteTSHEntryRelationQueryParams.addValue("history_ids_keep",
                taskEntity.getSignatureHistory().stream().map(SignatureHistoryEntryEntity::getId).toList());
        final String deleteTSHEntryRelationQuery = """
                DELETE FROM task_history h
                WHERE h.task_id = :task_id
                AND
                h.id NOT IN (:history_ids_keep)
                """;
        jdbcTemplate.update(deleteTSHEntryRelationQuery, deleteTSHEntryRelationQueryParams);
    }

}
