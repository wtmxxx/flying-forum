package com.atcumt.gpt.api.client.fallback;

import com.atcumt.gpt.api.client.UserClient;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.gpt.vo.ConversationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class UserClientFallback implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public Result<ConversationVO> getConversation(String conversationId) {
                // 获取对话失败
                log.error("远程调用UserClient#getConversation方法出现异常，参数：{}", conversationId, cause);
                throw new RuntimeException(cause);
            }

        };
    }
}
