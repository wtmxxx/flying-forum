package com.atcumt.post.service;

import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.model.post.dto.QuestionDTO;
import com.atcumt.model.post.dto.QuestionUpdateDTO;
import com.atcumt.model.post.vo.QuestionPostVO;
import com.atcumt.model.post.vo.QuestionVO;

public interface QuestionService {
    QuestionPostVO postQuestion(QuestionDTO questionDTO) throws Exception;

    QuestionPostVO updateQuestion(QuestionUpdateDTO questionUpdateDTO) throws AuthorizationException;

    QuestionPostVO saveQuestionAsDraft(QuestionDTO questionDTO);

    void deleteQuestion(Long questionId) throws AuthorizationException;

    void privateQuestion(Long questionId) throws AuthorizationException;

    QuestionVO getQuestion(Long questionId);

    void pinQuestion(Long questionId);

    void unpinQuestion(Long questionId);
}
