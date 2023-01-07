package com.trodix.signature.persistance.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.trodix.signature.domain.entity.UserEntity;
import lombok.AllArgsConstructor;

@Repository
@Transactional
@AllArgsConstructor
public class UserRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;


    public static final String TABLE_NAME = "user_";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_EMAIL = "email";

    static class UserEntityRowMapper implements RowMapper<UserEntity> {

        @Override
        @Nullable
        public UserEntity mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final UserEntity entity = new UserEntity();
            entity.setId(rs.getLong(COLUMN_ID));
            entity.setEmail(rs.getString(COLUMN_EMAIL));

            return entity;
        }

    }

    public void persist(final UserEntity entity) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(COLUMN_EMAIL, entity.getEmail());

        final String query = "INSERT INTO user_ (email) VALUES (:email)";
        jdbcTemplate.update(query, params);
    }

    public Optional<UserEntity> findById(final Long id) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(COLUMN_ID, id);

        final String query = "SELECT * FROM user_ u WHERE d.id = :id";

        return Optional.ofNullable(jdbcTemplate.queryForObject(query, params, new UserEntityRowMapper()));
    }

    public List<UserEntity> findAll() {

        final String query = "SELECT * FROM user_ u";

        return jdbcTemplate.query(query, new MapSqlParameterSource(), new UserEntityRowMapper());
    }

    public List<UserEntity> findAllRecipientByTaskId(final UUID taskId) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("taskId", taskId, Types.VARCHAR);

        final String query = """
            SELECT u.* FROM task_recipient r 
            INNER JOIN task t ON t.id = r.task_id 
            INNER JOIN user_ u ON u.id = r.user_id 
            WHERE 
                t.uid = :taskId
            """;

        return jdbcTemplate.query(query, params, new UserEntityRowMapper());
    }

    public Optional<UserEntity> findByEmail(final String email) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(COLUMN_EMAIL, email);

        final String query = "SELECT * FROM user_ u WHERE u.email = :email";

        List<UserEntity> results = jdbcTemplate.query(query, params, new UserEntityRowMapper());

        return DaoUtils.findOne(results);
    }

    public void deleteById(final Long id) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(COLUMN_ID, id);

        final String query = "DELETE FROM user_ u WHERE d.id = :id";

        jdbcTemplate.query(query, params, new UserEntityRowMapper());
    }

}
