package com.atcumt.gpt.service;

import com.atcumt.model.gpt.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;

public interface MessageService extends IService<Message> {
    void deleteMessages(String messageId);
}
