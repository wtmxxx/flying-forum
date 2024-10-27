# GPT & WebSocket流式输出JSON格式规范

## 1. 客户端请求规范

```json
{
  "conversationId": "UUID",
  "content": "your questions."
}
```

## 2. 客户端接收规范

### 两种类型

若返回类型（**type**）为**content**，如下

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "type": "content",
    "data": {
      "conversationId": "UUID",
      "lastMessageId": "UUID",
      "role": "ai",
      "content": "",
      "createTime": 1730001005000,
      "updateTime": 1730001005000
    }
  }
}
```

若返回类型（**type**）为**citations**，如下

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "type": "citations",
    "messageId": "UUID",
    "data": [
      {
        "url": "https://www.wotemo.com",
        "title": "Wotemo"
      },
      {
        "url": "https://www.cumt.edu.cn",
        "title": "CUMT"
      }
    ]
  }
}
```

## 3. Python（GPT）接收规范

```json
{
  "id": "UUID",
  "userId": "UUID",
  "messages": [
    {
      "id": "UUID",
      "conversationId": "UUID",
      "role": "human",
      "content": "Who is Wotemo?",
      "createTime": 1730001005000,
      "updateTime": 1730001005000
    }
  ],
  "createTime": 1730001005000,
  "updateTime": 1730001005000
}
```

## 4. Python（GPT）发送规范

### 两种类型

若发送类型（**type**）为**content**，如下

```json
{
  "type": "content",
  "content": "Wotemo is a well-known backend developer."
}
```

若发送类型（**type**）为**citations**，如下

```json
{
  "type": "citations",
  "citations": [
    {
      "url": "https://www.wotemo.com",
      "title": "Wotemo"
    },
    {
      "url": "https://www.cumt.edu.cn",
      "title": "CUMT"
    }
  ]
}
```