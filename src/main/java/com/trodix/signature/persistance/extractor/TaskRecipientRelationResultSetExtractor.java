package com.trodix.signature.persistance.extractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.lang.Nullable;

/**
 * Return the relation for task_recipient table.
 * 
 * <p>Pair&lt;task_id, user_id&gt; or <i>null</i> if none found</p>
 */
public class TaskRecipientRelationResultSetExtractor implements ResultSetExtractor<Pair<Long, Long>> {

    @Override
    @Nullable
    public Pair<Long, Long> extractData(ResultSet rs) throws SQLException, DataAccessException {
        if (rs.next()) {
            return new ImmutablePair<>(rs.getLong("task_id"), rs.getLong("user_id"));
        }

        return null;
    }
    
}
