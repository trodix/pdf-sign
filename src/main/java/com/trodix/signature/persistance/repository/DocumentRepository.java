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
import com.trodix.signature.domain.entity.DocumentEntity;
import lombok.AllArgsConstructor;

@Repository
@Transactional
@AllArgsConstructor
public class DocumentRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;


    public static final String TABLE_NAME = "document";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_UID = "uid";
    public static final String COLUMN_DOWNLOADED = "downloaded";
    public static final String COLUMN_ORIGINAL_FILE_NAME = "original_file_name";

    static class DocumentEntityRowMapper implements RowMapper<DocumentEntity> {

        @Override
        @Nullable
        public DocumentEntity mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final DocumentEntity entity = new DocumentEntity();
            entity.setId(rs.getLong(COLUMN_ID));
            entity.setDocumentId(UUID.fromString(rs.getString(COLUMN_UID)));
            entity.setDownloaded(rs.getBoolean(COLUMN_DOWNLOADED));
            entity.setOriginalFileName(rs.getString(COLUMN_ORIGINAL_FILE_NAME));

            return entity;
        }

    }

    public Optional<DocumentEntity> findByDocumentId(final UUID documentId) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("uid", documentId, Types.VARCHAR);

        final String query = "SELECT * FROM document d WHERE d.uid = :uid";

        return Optional.ofNullable(jdbcTemplate.queryForObject(query, params, new DocumentEntityRowMapper()));
    }

    public Optional<DocumentEntity> findById(final Long id) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);

        final String query = "SELECT * FROM document d WHERE d.id = :id";

        return Optional.ofNullable(jdbcTemplate.queryForObject(query, params, new DocumentEntityRowMapper()));
    }

    public List<DocumentEntity> findByTaskId(final UUID taskId) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("taskId", taskId, Types.VARCHAR);

        final String query = """
                SELECT d.* FROM document d
                    INNER JOIN task t on t.id = d.task_id
                    WHERE
                        t.uid = :taskId
                """;

        return jdbcTemplate.query(query, params, new DocumentEntityRowMapper());
    }

    public List<DocumentEntity> findAll() {

        final String query = "SELECT * FROM document d";

        return jdbcTemplate.query(query, new MapSqlParameterSource(), new DocumentEntityRowMapper());
    }

    public List<DocumentEntity> findByDownloaded(final boolean downloaded) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("downloaded", downloaded);

        final String query = "SELECT * FROM document d WHERE d.downloaded = :downloaded";

        return jdbcTemplate.query(query, params, new DocumentEntityRowMapper());
    }

    public void deleteById(final Long id) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);

        final String query = "DELETE FROM document d WHERE d.id = :id";

        jdbcTemplate.query(query, params, new DocumentEntityRowMapper());
    }

    public void deleteByDocumentId(final UUID documentId) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("uid", documentId, Types.VARCHAR);

        final String query = "DELETE FROM document d WHERE d.uid = :uid";

        jdbcTemplate.query(query, params, new DocumentEntityRowMapper());
    }

}
