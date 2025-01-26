package com.atcumt.post.service.admin;

import com.atcumt.model.post.dto.QuestionUpdateDTO;
import com.atcumt.model.post.vo.QuestionPostVO;

public interface AdminQuestionService {
    QuestionPostVO updateQuestion(QuestionUpdateDTO questionUpdateDTO);

    void deleteQuestion(Long questionId);

    void deleteQuestionComplete(Long questionId);
}
