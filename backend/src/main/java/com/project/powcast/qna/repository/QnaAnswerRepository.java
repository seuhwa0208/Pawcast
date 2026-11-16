package com.project.powcast.qna.repository;

import com.project.powcast.qna.entity.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long> {
}
