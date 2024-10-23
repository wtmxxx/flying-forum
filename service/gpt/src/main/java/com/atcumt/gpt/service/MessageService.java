package com.atcumt.gpt.service;

import com.atcumt.model.gpt.dto.MessageDTO;
import com.atcumt.model.gpt.entity.Message;
import com.atcumt.model.gpt.vo.MessageVO;
import com.baomidou.mybatisplus.extension.service.IService;
import reactor.core.publisher.Flux;

public interface MessageService extends IService<Message> {
    MessageVO processChatStuff(MessageDTO messageDTO);

    Flux<MessageVO> processChatStreamFlux(MessageDTO messageDTO);
}
