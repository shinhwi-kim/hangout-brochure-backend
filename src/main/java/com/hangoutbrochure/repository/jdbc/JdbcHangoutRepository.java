package com.hangoutbrochure.repository.jdbc;

import com.hangoutbrochure.domain.Hangout;
import com.hangoutbrochure.domain.HangoutListCriteria;
import com.hangoutbrochure.repository.HangoutRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcHangoutRepository implements HangoutRepository {

    private static final RowMapper<Hangout> HANGOUT_ROW_MAPPER = JdbcHangoutRepository::mapHangout;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcHangoutRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Hangout> findHangouts(HangoutListCriteria criteria) {
        StringBuilder sql = new StringBuilder("""
                SELECT h.id,
                       h.title,
                       h.starts_at,
                       h.ends_at,
                       h.location_name,
                       h.address,
                       h.latitude,
                       h.longitude,
                       COALESCE((
                           SELECT array_agg(category.code ORDER BY category.code)
                           FROM hangout_category_assignments assignment
                           JOIN hangout_categories category ON category.id = assignment.category_id
                           WHERE assignment.hangout_id = h.id
                       ), ARRAY[]::varchar[]) AS categories
                FROM hangouts h
                WHERE TRUE
                """);

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("limit", criteria.limit());

        if (!criteria.categories().isEmpty()) {
            sql.append("""
                    AND EXISTS (
                        SELECT 1
                        FROM hangout_category_assignments assignment
                        JOIN hangout_categories category ON category.id = assignment.category_id
                        WHERE assignment.hangout_id = h.id
                          AND category.code IN (:categories)
                    )
                    """);
            parameters.addValue("categories", criteria.categories());
        }

        sql.append(" AND h.ends_at >= :from AND h.starts_at <= :to");
        parameters.addValue("from", criteria.from());
        parameters.addValue("to", criteria.to());

        if (criteria.cursorId() != null) {
            sql.append(" AND h.id > :cursorId");
            parameters.addValue("cursorId", criteria.cursorId());
        }

        sql.append(" ORDER BY h.id ASC LIMIT :limit");
        return jdbcTemplate.query(sql.toString(), parameters, HANGOUT_ROW_MAPPER);
    }

    private static Hangout mapHangout(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Hangout(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("title"),
                readCategories(resultSet.getArray("categories")),
                resultSet.getObject("starts_at", OffsetDateTime.class),
                resultSet.getObject("ends_at", OffsetDateTime.class),
                resultSet.getString("location_name"),
                resultSet.getString("address"),
                resultSet.getDouble("latitude"),
                resultSet.getDouble("longitude")
        );
    }

    private static List<String> readCategories(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return List.of();
        }

        try {
            Object[] values = (Object[]) sqlArray.getArray();
            return Arrays.stream(values).map(String::valueOf).toList();
        } finally {
            sqlArray.free();
        }
    }
}
