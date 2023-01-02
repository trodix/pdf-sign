package com.trodix.signature.persistance.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.trodix.signature.domain.entity.SignatureHistoryEntryEntity;
import com.trodix.signature.domain.entity.UserEntity;
import lombok.AllArgsConstructor;

@Repository
@Transactional
@AllArgsConstructor
public class SignatureHistoryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;


    public static final String TABLE_NAME = "task_history";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SIGNED_AT = "signed_at";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_USER_EMAIL = "user_email";

    static class SignatureHistoryEntryEntityRowMapper implements RowMapper<SignatureHistoryEntryEntity> {

        @Override
        @Nullable
        public SignatureHistoryEntryEntity mapRow(final ResultSet rs, final int rowNum) throws SQLException {

            final UserEntity signedBy = new UserEntity();
            signedBy.setId(rs.getLong(COLUMN_USER_ID));
            signedBy.setEmail(rs.getString(COLUMN_USER_EMAIL));

            final SignatureHistoryEntryEntity entity = new SignatureHistoryEntryEntity();
            entity.setId(rs.getLong(COLUMN_ID));
            entity.setSignedAt(rs.getTimestamp(COLUMN_SIGNED_AT).toLocalDateTime());
            entity.setSignedBy(signedBy);

            return entity;
        }

    }

    public List<SignatureHistoryEntryEntity> findByTaskId(final UUID taskId) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("taskId", taskId);

        final String query = """
                SELECT th.*, u.email FROM task_history th
                INNER JOIN task t ON t.id = th.task_id
                INNER JOIN user_ u ON th.user_id = u.id
                WHERE t.uid = :taskId
                """;

        return jdbcTemplate.query(query, params, new SignatureHistoryEntryEntityRowMapper());
    }

}
