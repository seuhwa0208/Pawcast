package com.project.pawcast.cart.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CartMergeRequestRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean insertIfAbsent(Long memberId, UUID mergeId) {
        int insertedRows = jdbcTemplate.update(
                """
                INSERT INTO cart_merge_requests (member_id, merge_id)
                VALUES (?, ?)
                ON CONFLICT (member_id, merge_id) DO NOTHING
                """,
                memberId,
                mergeId
        );

        return insertedRows == 1;
    }
}