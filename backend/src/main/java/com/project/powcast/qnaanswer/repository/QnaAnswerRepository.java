package com.project.powcast.qnaanswer.repository;

import com.project.powcast.qna.entity.Qna;
import com.project.powcast.qnaanswer.entity.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long> {
    Optional<QnaAnswer>findByQna(Qna qna);
}
