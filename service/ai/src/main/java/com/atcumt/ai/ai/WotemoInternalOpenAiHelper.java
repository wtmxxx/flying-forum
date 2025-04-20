package com.atcumt.ai.ai;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.*;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.UnsupportedFeatureException;
import dev.langchain4j.internal.Exceptions;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElementHelper;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.InternalOpenAiHelper;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiTokenUsage;
import dev.langchain4j.model.openai.internal.chat.*;
import dev.langchain4j.model.openai.internal.chat.Content;
import dev.langchain4j.model.openai.internal.chat.ContentType;
import dev.langchain4j.model.openai.internal.shared.CompletionTokensDetails;
import dev.langchain4j.model.openai.internal.shared.PromptTokensDetails;
import dev.langchain4j.model.openai.internal.shared.Usage;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;

import java.util.*;
import java.util.stream.Collectors;

public class WotemoInternalOpenAiHelper {
    static final String DEFAULT_OPENAI_URL = "https://api.openai.com/v1";
    static final String DEFAULT_USER_AGENT = "langchain4j-openai";

    public WotemoInternalOpenAiHelper() {
    }

    public static List<Message> toOpenAiMessages(List<ChatMessage> messages) {
        return (List)messages.stream().map(InternalOpenAiHelper::toOpenAiMessage).collect(Collectors.toList());
    }

    public static Message toOpenAiMessage(ChatMessage message) {
        if (message instanceof SystemMessage) {
            return dev.langchain4j.model.openai.internal.chat.SystemMessage.from(((SystemMessage)message).text());
        } else if (message instanceof UserMessage) {
            UserMessage userMessage = (UserMessage)message;
            return userMessage.hasSingleText() ? dev.langchain4j.model.openai.internal.chat.UserMessage.builder().content(userMessage.singleText()).name(userMessage.name()).build() : dev.langchain4j.model.openai.internal.chat.UserMessage.builder().content((List)userMessage.contents().stream().map(WotemoInternalOpenAiHelper::toOpenAiContent).collect(Collectors.toList())).name(userMessage.name()).build();
        } else if (message instanceof AiMessage) {
            AiMessage aiMessage = (AiMessage)message;
            if (!aiMessage.hasToolExecutionRequests()) {
                return AssistantMessage.from(aiMessage.text());
            } else {
                ToolExecutionRequest toolExecutionRequest = (ToolExecutionRequest)aiMessage.toolExecutionRequests().get(0);
                if (toolExecutionRequest.id() == null) {
                    FunctionCall functionCall = FunctionCall.builder().name(toolExecutionRequest.name()).arguments(toolExecutionRequest.arguments()).build();
                    return AssistantMessage.builder().functionCall(functionCall).build();
                } else {
                    List<ToolCall> toolCalls = (List)aiMessage.toolExecutionRequests().stream().map((it) -> ToolCall.builder().id(it.id()).type(ToolType.FUNCTION).function(FunctionCall.builder().name(it.name()).arguments(it.arguments()).build()).build()).collect(Collectors.toList());
                    return AssistantMessage.builder().content(aiMessage.text()).toolCalls(toolCalls).build();
                }
            }
        } else if (message instanceof ToolExecutionResultMessage) {
            ToolExecutionResultMessage toolExecutionResultMessage = (ToolExecutionResultMessage)message;
            return (Message)(toolExecutionResultMessage.id() == null ? FunctionMessage.from(toolExecutionResultMessage.toolName(), toolExecutionResultMessage.text()) : ToolMessage.from(toolExecutionResultMessage.id(), toolExecutionResultMessage.text()));
        } else {
            throw Exceptions.illegalArgument("Unknown message type: " + String.valueOf(message.type()), new Object[0]);
        }
    }

    private static Content toOpenAiContent(dev.langchain4j.data.message.Content content) {
        if (content instanceof TextContent) {
            return toOpenAiContent((TextContent)content);
        } else if (content instanceof ImageContent) {
            return toOpenAiContent((ImageContent)content);
        } else if (content instanceof AudioContent) {
            AudioContent audioContent = (AudioContent)content;
            return toOpenAiContent(audioContent);
        } else {
            throw Exceptions.illegalArgument("Unknown content type: " + String.valueOf(content), new Object[0]);
        }
    }

    private static Content toOpenAiContent(TextContent content) {
        return Content.builder().type(ContentType.TEXT).text(content.text()).build();
    }

    private static Content toOpenAiContent(ImageContent content) {
        return Content.builder().type(ContentType.IMAGE_URL).imageUrl(ImageUrl.builder().url(toUrl(content.image())).detail(toDetail(content.detailLevel())).build()).build();
    }

    private static Content toOpenAiContent(AudioContent audioContent) {
        return Content.builder().type(ContentType.AUDIO).inputAudio(InputAudio.builder().data(ValidationUtils.ensureNotBlank(audioContent.audio().base64Data(), "audio.base64Data")).format(extractSubtype(ValidationUtils.ensureNotBlank(audioContent.audio().mimeType(), "audio.mimeType"))).build()).build();
    }

    private static String extractSubtype(String mimetype) {
        return mimetype.split("/")[1];
    }

    private static String toUrl(Image image) {
        return image.url() != null ? image.url().toString() : String.format("data:%s;base64,%s", image.mimeType(), image.base64Data());
    }

    private static ImageDetail toDetail(ImageContent.DetailLevel detailLevel) {
        return detailLevel == null ? null : ImageDetail.valueOf(detailLevel.name());
    }

    public static List<Tool> toTools(Collection<ToolSpecification> toolSpecifications, boolean strict) {
        return toolSpecifications == null ? null : (List)toolSpecifications.stream().map((toolSpecification) -> toTool(toolSpecification, strict)).collect(Collectors.toList());
    }

    private static Tool toTool(ToolSpecification toolSpecification, boolean strict) {
        Function.Builder functionBuilder = Function.builder().name(toolSpecification.name()).description(toolSpecification.description()).parameters(toOpenAiParameters(toolSpecification.parameters(), strict));
        if (strict) {
            functionBuilder.strict(true);
        }

        Function function = functionBuilder.build();
        return Tool.from(function);
    }

    /** @deprecated */
    @Deprecated
    public static List<Function> toFunctions(Collection<ToolSpecification> toolSpecifications) {
        return (List)toolSpecifications.stream().map(WotemoInternalOpenAiHelper::toFunction).collect(Collectors.toList());
    }

    /** @deprecated */
    @Deprecated
    private static Function toFunction(ToolSpecification toolSpecification) {
        return Function.builder().name(toolSpecification.name()).description(toolSpecification.description()).parameters(toOpenAiParameters(toolSpecification.parameters(), false)).build();
    }

    private static Map<String, Object> toOpenAiParameters(JsonObjectSchema parameters, boolean strict) {
        if (parameters != null) {
            return JsonSchemaElementHelper.toMap(parameters, strict);
        } else {
            Map<String, Object> map = new LinkedHashMap();
            map.put("type", "object");
            map.put("properties", new HashMap());
            map.put("required", new ArrayList());
            if (strict) {
                map.put("additionalProperties", false);
            }

            return map;
        }
    }

    public static AiMessage aiMessageFrom(ChatCompletionResponse response) {
        AssistantMessage assistantMessage = ((ChatCompletionChoice)response.choices().get(0)).message();
        String text = assistantMessage.content();
        List<ToolCall> toolCalls = assistantMessage.toolCalls();
        if (!Utils.isNullOrEmpty(toolCalls)) {
            List<ToolExecutionRequest> toolExecutionRequests = (List)toolCalls.stream().filter((toolCall) -> toolCall.type() == ToolType.FUNCTION).map(WotemoInternalOpenAiHelper::toToolExecutionRequest).collect(Collectors.toList());
            return Utils.isNullOrBlank(text) ? AiMessage.from(toolExecutionRequests) : AiMessage.from(text, toolExecutionRequests);
        } else {
            FunctionCall functionCall = assistantMessage.functionCall();
            if (functionCall != null) {
                ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder().name(functionCall.name()).arguments(functionCall.arguments()).build();
                return Utils.isNullOrBlank(text) ? AiMessage.from(new ToolExecutionRequest[]{toolExecutionRequest}) : AiMessage.from(text, Collections.singletonList(toolExecutionRequest));
            } else {
                return AiMessage.from(text);
            }
        }
    }

    private static ToolExecutionRequest toToolExecutionRequest(ToolCall toolCall) {
        FunctionCall functionCall = toolCall.function();
        return ToolExecutionRequest.builder().id(toolCall.id()).name(functionCall.name()).arguments(functionCall.arguments()).build();
    }

    public static OpenAiTokenUsage tokenUsageFrom(Usage openAiUsage) {
        if (openAiUsage == null) {
            return null;
        } else {
            PromptTokensDetails promptTokensDetails = openAiUsage.promptTokensDetails();
            OpenAiTokenUsage.InputTokensDetails inputTokensDetails = null;
            if (promptTokensDetails != null) {
                inputTokensDetails = new OpenAiTokenUsage.InputTokensDetails(promptTokensDetails.cachedTokens());
            }

            CompletionTokensDetails completionTokensDetails = openAiUsage.completionTokensDetails();
            OpenAiTokenUsage.OutputTokensDetails outputTokensDetails = null;
            if (completionTokensDetails != null) {
                outputTokensDetails = new OpenAiTokenUsage.OutputTokensDetails(completionTokensDetails.reasoningTokens());
            }

            return OpenAiTokenUsage.builder().inputTokenCount(openAiUsage.promptTokens()).inputTokensDetails(inputTokensDetails).outputTokenCount(openAiUsage.completionTokens()).outputTokensDetails(outputTokensDetails).totalTokenCount(openAiUsage.totalTokens()).build();
        }
    }

    public static FinishReason finishReasonFrom(String openAiFinishReason) {
        if (openAiFinishReason == null) {
            return null;
        } else {
            switch (openAiFinishReason) {
                case "stop":
                    return FinishReason.STOP;
                case "length":
                    return FinishReason.LENGTH;
                case "tool_calls":
                case "function_call":
                    return FinishReason.TOOL_EXECUTION;
                case "content_filter":
                    return FinishReason.CONTENT_FILTER;
                default:
                    return null;
            }
        }
    }

    static ResponseFormat toOpenAiResponseFormat(dev.langchain4j.model.chat.request.ResponseFormat responseFormat, Boolean strict) {
        if (responseFormat != null && responseFormat.type() != dev.langchain4j.model.chat.request.ResponseFormatType.TEXT) {
            JsonSchema jsonSchema = responseFormat.jsonSchema();
            if (jsonSchema == null) {
                return ResponseFormat.builder().type(dev.langchain4j.model.openai.internal.chat.ResponseFormatType.JSON_OBJECT).build();
            } else if (!(jsonSchema.rootElement() instanceof JsonObjectSchema)) {
                throw new IllegalArgumentException("For OpenAI, the root element of the JSON Schema must be a JsonObjectSchema, but it was: " + String.valueOf(jsonSchema.rootElement().getClass()));
            } else {
                dev.langchain4j.model.openai.internal.chat.JsonSchema openAiJsonSchema = dev.langchain4j.model.openai.internal.chat.JsonSchema.builder().name(jsonSchema.name()).strict(strict).schema(JsonSchemaElementHelper.toMap(jsonSchema.rootElement(), strict)).build();
                return ResponseFormat.builder().type(dev.langchain4j.model.openai.internal.chat.ResponseFormatType.JSON_SCHEMA).jsonSchema(openAiJsonSchema).build();
            }
        } else {
            return null;
        }
    }

    public static ToolChoiceMode toOpenAiToolChoice(dev.langchain4j.model.chat.request.ToolChoice toolChoice) {
        if (toolChoice == null) {
            return null;
        } else {
            ToolChoiceMode var10000;
            switch (toolChoice) {
                case AUTO -> var10000 = ToolChoiceMode.AUTO;
                case REQUIRED -> var10000 = ToolChoiceMode.REQUIRED;
                default -> throw new IncompatibleClassChangeError();
            }

            return var10000;
        }
    }

    public static Response<AiMessage> convertResponse(ChatResponse chatResponse) {
        return Response.from(chatResponse.aiMessage(), chatResponse.metadata().tokenUsage(), chatResponse.metadata().finishReason());
    }

    static void validate(ChatRequestParameters parameters) {
        if (parameters.topK() != null) {
            throw new UnsupportedFeatureException("'topK' parameter is not supported by OpenAI");
        }
    }

    static dev.langchain4j.model.chat.request.ResponseFormat fromOpenAiResponseFormat(String responseFormat) {
        return "json_object".equals(responseFormat) ? dev.langchain4j.model.chat.request.ResponseFormat.JSON : null;
    }

    static ChatCompletionRequest.Builder toOpenAiChatRequest(ChatRequest chatRequest, OpenAiChatRequestParameters parameters, Boolean strictTools, Boolean strictJsonSchema) {
        return ChatCompletionRequest.builder().messages(toOpenAiMessages(chatRequest.messages())).model(parameters.modelName()).temperature(parameters.temperature()).topP(parameters.topP()).frequencyPenalty(parameters.frequencyPenalty()).presencePenalty(parameters.presencePenalty()).maxTokens(parameters.maxOutputTokens()).stop(parameters.stopSequences()).tools(toTools(parameters.toolSpecifications(), strictTools)).toolChoice(toOpenAiToolChoice(parameters.toolChoice())).responseFormat(toOpenAiResponseFormat(parameters.responseFormat(), strictJsonSchema)).maxCompletionTokens(parameters.maxCompletionTokens()).logitBias(parameters.logitBias()).parallelToolCalls(parameters.parallelToolCalls()).seed(parameters.seed()).user(parameters.user()).store(parameters.store()).metadata(parameters.metadata()).serviceTier(parameters.serviceTier()).reasoningEffort(parameters.reasoningEffort());
    }
}
