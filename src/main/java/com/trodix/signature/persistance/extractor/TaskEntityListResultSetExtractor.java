package com.trodix.signature.persistance.extractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.lang.Nullable;
import com.trodix.signature.domain.entity.SignTaskStatus;
import com.trodix.signature.domain.entity.TaskEntity;
import com.trodix.signature.domain.entity.UserEntity;

public class TaskEntityListResultSetExtractor implements ResultSetExtractor<List<TaskEntity>> {

    @Override
    @Nullable
    public List<TaskEntity> extractData(final ResultSet rs) throws SQLException, DataAccessException {

        // Use multiple queries for *-to-many relationships :
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

            taskList.add(entity);
        }

        return taskList;
    }

}
