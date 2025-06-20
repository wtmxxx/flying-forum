# FlyingForum —— 集 GPT 与 论坛于一体的校园综合服务平台

FlyingForum 是一个融合智能问答与社交互动功能的综合性平台。系统基于高并发分布式架构，借助 GPT 等前沿技术，实现了实时互动、智能问答、热点讨论等功能，致力于为用户提供高效、便捷的知识分享与问题解决体验。

---

## 目录

- [项目概述](#项目概述)
- [核心技术架构](#核心技术架构)
- [数据库设计](#数据库设计)
- [系统优化与高并发处理](#系统优化与高并发处理)
- [GPT 问答机制与未回答处理](#gpt-问答机制与未回答处理)
- [部署与运行指南](#部署与运行指南)
- [贡献与合作](#贡献与合作)
- [项目展望](#项目展望)
- [许可证](#许可证)

---

## 项目概述

FlyingForum 结合了 GPT 智能问答与传统论坛社交功能，支持用户直接发起提问，并自动尝试生成回答；若生成失败，则问题将发布至论坛版块，由社区其他成员协同解答。系统采用高并发分布式架构，并引入多项缓存与监控手段，确保在大规模访问情况下依然保持稳定和高效。

---

## 核心技术架构

### 后端技术栈

- **Spring Boot 3**  
  构建模块化、高效的 RESTful API 服务。

- **Spring Cloud**  
  采用微服务架构，拆分为独立服务（用户、帖子、评论等），支持弹性扩展。

- **Sa-Token + OAuth2.0**  
  实现用户认证与授权，支持第三方登录（微信、QQ、GitHub）。

- **MyBatis-Plus**  
  简化数据库操作，提高开发效率。

- **RAG + LangChain**  
  增强 GPT 的问答能力，通过检索相关文档提升生成回答的准确性与实用性。

- **Redis**  
  缓存热点数据，降低数据库压力，加速响应。

- **Elasticsearch**  
  提供精准、高效的全文检索功能。

- **消息队列（Kafka / RocketMQ）**  
  异步处理任务（如消息通知、回答生成）。

- **Prometheus + Grafana**  
  实时监控系统运行状态，支持数据可视化分析。

- **Spring Cloud Gateway**  
  统一管理 API 请求，支持限流、熔断和负载均衡。

### 系统架构示意图

```
+-----------------+       +-----------------------+       +-----------------+
|    API 网关     | <---> |      微服务系统       | <---> |  数据库 / 缓存  |
| (Spring Cloud   |       | (用户、帖子、评论等)  |       | (MySQL、Redis、 |
|   Gateway)      |       |                       |       | Elasticsearch)  |
+-----------------+       +-----------------------+       +-----------------+
         |                          |
         |                          |
         v                          v
   +-------------+           +-----------------+
   |    监控     |           |  消息队列系统   |
   | (Prometheus |           | (Kafka / MQ)    |
   |   + Grafana)|           +-----------------+
   +-------------+
```

---

## 数据库设计

### 关系型数据库（MySQL / PostgreSQL）

- **鉴权架构（auth）**  
  存储用户认证信息，支持多种登录方式。

- **核心业务数据**  
  存储用户、帖子、评论等数据，保证数据一致性。

### 文档数据库（MongoDB）

- **帖子集合（post）**  
  保存帖子标题、内容及发布相关信息。

- **评论集合（comment）**  
  记录用户评论数据。

- **点赞集合（like）**  
  管理用户点赞操作。

- **关注集合（follow）**  
  记录用户关注关系数据。

- **文件集合（file）**  
  存储图片、视频等文件的元数据与文件地址。

---

## 系统优化与高并发处理

1. **缓存机制**  
   利用 Redis 缓存热门数据，如帖子与问题详情，减少数据库压力。

2. **异步任务处理**  
   通过 Kafka 或 RocketMQ 处理耗时任务（如通知推送、答案生成），提升响应速度。

3. **流量控制与熔断**  
   采用 Sentinel 限流、熔断策略，保障系统在高并发下的稳定性。

4. **CDN 加速**  
   利用 CDN 提供静态资源（图片、视频）的高速加载能力。

5. **分布式任务调度**  
   使用 Elastic Job 定时清理无用数据，重新计算帖子热度，保证数据时效性与准确性。

---

## GPT 问答机制与未回答处理

1. **智能提问与回答**  
   用户发起问题后，系统调用 GPT 接口尝试自动生成回答。

2. **回答失败处理**  
   若 GPT 无法生成有效回答，将问题存储至问题表，并自动发布至论坛相应板块，等待社区用户协同解答。

3. **用户互动**  
   鼓励社区成员对问题进行讨论与补充，提升平台活跃度。

4. **动态状态更新**  
   一旦问题得到有效解决，系统将自动更新状态并通知提问者。

---

## 部署与运行指南

### 环境准备

- 必须安装并配置下列服务：
   - Nacos（配置中心）
   - Sentinel（流量控制）
   - Seata（分布式事务管理）
   - MySQL / PostgreSQL（关系型数据库）
   - MongoDB（文档数据库）
   - Redis（缓存数据库）
   - Elasticsearch（搜索数据库）
   - RocketMQ / Kafka（消息队列）
   - MinIO（对象存储）
   - Monstache（MongoDB -> Elasticsearch 数据同步）

### 启动项目

```bash
# 克隆项目代码
git clone https://github.com/wtmxxx/flying-forum.git
cd flying-forum

# 使用 Maven 构建并启动后端服务
./mvnw spring-boot:run
```

### 配置说明

- 根据不同环境，调整配置文件中的数据库、Redis、ES 等参数。
- 可通过 Nacos 进行配置管理，确保各微服务配置统一。

---

## 贡献与合作

我们欢迎开发者积极参与项目建设。贡献流程如下：

1. Fork 项目并新建功能分支：

   ```bash
   git checkout -b feature/your-feature
   ```

2. 提交代码改动：

   ```bash
   git commit -m '[feature]添加新功能描述'
   ```

3. 推送分支并创建 Pull Request。

请确保遵循项目编码规范和提交规范，以便顺利合并。

---

## 项目展望

1. **智能推荐系统**  
   利用用户行为与问答数据，精准推送相关内容，提升用户体验。

2. **社交功能扩展**  
   增加私信、实时讨论等功能，丰富用户互动形式。

3. **多文档检索优化**  
   深化 RAG 与 LangChain 集成，提高复杂问题回答的准确性和深度。

4. **分布式扩展能力**  
   持续优化系统架构，支持更多并发用户访问，确保高可用性和稳定性。

---

## 许可证

本项目（FlyingForum）采用 [Apache 2.0 许可证](https://www.apache.org/licenses/LICENSE-2.0) 开源，欢迎各位开发者在遵守协议的前提下自由使用、修改和分发代码。
