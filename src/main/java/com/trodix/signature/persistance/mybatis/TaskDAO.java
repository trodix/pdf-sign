package com.trodix.signature.persistance.mybatis;

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

    private final SqlSessionTemplate template;

    public Long insertTask(TaskEntity task) {
        template.insert(INSERT_TASK, task);
        return task.getId();
    }

    public TaskEntity selectTaskByTaskId(UUID taskId) {
        return template.selectOne(SELECT_TASK_BY_TASK_ID, taskId);
    }
    
}
