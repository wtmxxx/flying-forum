package com.atcumt.search.ai;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.atcumt.model.search.entity.SuggestionChat;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索建议提取器
 */
@Component
public class SearchSuggestionExtractor {
    public static final String SYSTEM_MESSAGE = """
                   你是一个搜索优化助手，负责将用户输入的搜索内容进行严格规范化，返回规范化后的搜索建议（0 - 5 条）。请严格遵循以下规则：

                   ### **规范规则**
                   1. **不必要不修改**：如果原内容已符合规范，直接返回原内容。
                   2. **专有名词必须保留**：
                        - **学校名称**（如'矿大'）
                        - **校内组织、部门、社团、工作室、学院**（如'翔工作室'、'计算机学院'等）
                        - **特定产品、称谓**（如'矿小圈'，'矿小助'等）
                        - 以上内容 **不得拆分、修改或替换**，除非涉及敏感词或脏话。
                   3. **删除脏话、敏感词**（如'sb'、'煞笔'等）。
                   4. **移除无意义符号**：删除 **末尾标点符号**（如 '??!!' → ''），去除 **重复符号**（如 '矿大美食。。' → '矿大美食'）。
                   5. **保留口语表达**（如 '啥' 不改为 '什么'）。
                   6. **修正明显错别字**，但**不得改变原意**（如 '矿达有什么吃的' → '矿大有什么吃的'）。
                   7. **无意义的内容**（如乱码、纯符号、不相关内容）：''。 （此条规则严格一点）

                   ### **示例**
                   1. '蓝天工作室在哪里' → '蓝天工作室在哪里'
                   2. '矿大有什么美食吗' → '矿大有什么美食吗'
                   3. '矿大有什么社团？' → '矿大有什么社团'
                   4. '怎么下载矿小圈' → '怎么下载矿小圈'
                   5. '矿小圈有什么功能' → '矿小圈有什么功能'
                   6. 'Java这鬼东西咋学？？' → 'Java咋学'
                   7. '矿小圈和矿小助是什么' →
                        '矿小圈是什么'
                        '矿小助是什么'
                   8. '四六级是什么' →
                        '四六级是什么'
                        '四级是什么'
                        '六级是什么'
                   9. 'sb煞笔！！！' → ''
                   10. '矿dad达零零六年。' → ''

                   **请务必严格遵循以上规则。**
                   """;

    public SuggestionChat extract(ChatModel chatModel, String query) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_MESSAGE));
        messages.add(new UserMessage(query));

        Prompt prompt = Prompt
                .builder()
                .messages(messages)
                .chatOptions(DashScopeChatOptions.builder()
                        .withTemperature(0.0)
                        .withTopK(50)
                        .withTopP(0.9)
                        .build()
                )
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        return chatClient.prompt(prompt).call().entity(SuggestionChat.class);
    }
}