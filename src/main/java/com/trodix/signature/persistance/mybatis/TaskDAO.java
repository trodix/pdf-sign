package com.trodix.signature.persistance.mybatis;

import java.util.List;
import java.util.UUID;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;
import com.trodix.signature.domain.entity.TaskEntity;
import lombok.AllArgsConstructor;

@Repository
@AllArgsConstructor
public class TaskDAO {

    private static final String INSERT_TASK = "pdfsign.task.insert.insert_Task";
    private static final String SELECT_TASK_BY_TASK_ID = "pdfsign.task.select_TaskByTaskId";
    private static final String SELECT_TASK_BY_DOCUMENT_ID = "pdfsign.task.select_TaskByDocumentId";
    private static final String SELECT_TASK_BY_USER_EMAIL = "pdfsign.task.select_TaskByUserEmail";

    private final SqlSessionTemplate template;

    public Long insertTask(final TaskEntity task) {
        template.insert(INSERT_TASK, task);
        return task.getId();
    }

    public TaskEntity findByTaskId(final UUID taskId) {
        return template.selectOne(SELECT_TASK_BY_TASK_ID, taskId);
    }

    public TaskEntity findByDocumentId(final UUID documentId) {
        return template.selectOne(SELECT_TASK_BY_DOCUMENT_ID, documentId);
    }

    public List<TaskEntity> findByUserEmail(final String email) {
        return template.selectList(SELECT_TASK_BY_USER_EMAIL, email);
    }

}
