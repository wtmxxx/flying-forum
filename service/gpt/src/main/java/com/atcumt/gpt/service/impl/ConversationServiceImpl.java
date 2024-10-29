package com.atcumt.gpt.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import com.atcumt.gpt.mapper.ConversationMapper;
import com.atcumt.gpt.mapper.MessageMapper;
import com.atcumt.gpt.service.ConversationService;
import com.atcumt.model.gpt.dto.ConversationDTO;
import com.atcumt.model.gpt.dto.ConversationGptDTO;
import com.atcumt.model.gpt.entity.Conversation;
import com.atcumt.model.gpt.entity.Message;
import com.atcumt.model.gpt.vo.ConversationVO;
import com.atcumt.model.gpt.vo.MessagePageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements ConversationService {
    private final RestTemplate restTemplate;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    @Value("${gpt.http.uri}:${gpt.http.port}")
    private String uri;

    @Override
    public ConversationVO newChat(ConversationDTO conversationDTO) {
        Conversation conversation = Conversation
                .builder()
                .userId(conversationDTO.getUserId())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        conversationMapper.insert(conversation);

        return BeanUtil.copyProperties(conversation, ConversationVO.class);
    }

    @Override
    public String getTitle(String conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        ConversationGptDTO conversationGptDTO = BeanUtil.toBean(conversation, ConversationGptDTO.class);
        String title = conversation.getTitle();

        if (title == null || title.isEmpty()) {
            LambdaQueryWrapper<Message> messageQueryWrapper = new LambdaQueryWrapper<>();
            messageQueryWrapper
                    .eq(Message::getConversationId, conversationId)
                    .orderByAsc(Message::getUpdateTime);
            List<Message> messages = messageMapper.selectList(messageQueryWrapper);
            conversationGptDTO.setMessages(messages);

            ResponseEntity<JSONObject> responseTitle = null;
            try {
                responseTitle = restTemplate
                        .postForEntity(uri + "/gpt/get_title", conversationGptDTO, JSONObject.class);
                // 检查GPT响应的状态码是否成功
                if (responseTitle.getStatusCode().is2xxSuccessful()) {
                    title = responseTitle.getBody().get("title", String.class);

                    LambdaUpdateWrapper<Conversation> conversationUpdateWrapper = new LambdaUpdateWrapper<>();
                    conversationMapper.update(
                            conversationUpdateWrapper
                                    .eq(Conversation::getId, conversationId)
                                    .set(Conversation::getTitle, title)
                    );
                }
            } catch (RestClientException e) {
                throw new RuntimeException("GPT服务调用失败(Title)，状态码：" + responseTitle.getStatusCode());
            }
        }

        return title;
    }

    @Override
    public void setTitle(String conversationId, String title) {
        // 定义正则表达式
        String regex = "[\\u4e00-\\u9fa5a-zA-Z\\s：:-—]+";
        // 编译正则表达式
        Pattern pattern = Pattern.compile(regex);
        // 创建 Matcher 对象
        Matcher matcher = pattern.matcher(title);

        // 判断是否匹配
        if (title.length() > 20 || !matcher.matches()) {
            throw new IllegalArgumentException("标题内容非法");
        }

        LambdaUpdateWrapper<Conversation> conversationUpdateWrapper = new LambdaUpdateWrapper<>();
        conversationMapper.update(
                conversationUpdateWrapper
                        .eq(Conversation::getId, conversationId)
                        .set(Conversation::getTitle, title)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(String conversationId) {
        conversationMapper.deleteById(conversationId);
        messageMapper.delete(new LambdaUpdateWrapper<Message>()
                .eq(Message::getConversationId, conversationId));
    }

    @Override
    public ConversationVO getConversation(String conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        List<Message> messages = messageMapper.selectList(
                Wrappers.<Message>lambdaQuery()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getUpdateTime)
        );
        List<MessagePageVO> messagePageVOs = BeanUtil.copyToList(messages, MessagePageVO.class);

        ConversationVO conversationVO = BeanUtil.toBean(conversation, ConversationVO.class);
        conversationVO.setMessages(messagePageVOs);

        return conversationVO;
    }

}


//    public List<Message> strMessageToList(String strMessage) {
//        Type messageListType = new TypeToken<List<Message>>() {}.getType();
//        return gson.fromJson(strMessage, messageListType);
//    }
