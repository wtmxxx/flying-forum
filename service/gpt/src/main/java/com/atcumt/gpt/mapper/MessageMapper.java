package com.atcumt.gpt.mapper;

import com.atcumt.model.gpt.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    @Select("select conversation_id, update_time from gpt.message where id = #{id}")
    Message getConversationIdAndUpdateTime(String id);
}
