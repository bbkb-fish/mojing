# 墨境小说后端

Java 21 + Spring Boot 3.4.4，实现根据光标前上下文生成下一段小说正文。

## 启动

未配置模型密钥时会使用内置演示响应：

```powershell
mvn spring-boot:run
```

接入 OpenAI 兼容模型：

```powershell
$env:AI_API_KEY = "你的密钥"
$env:AI_BASE_URL = "https://api.openai.com/v1"
$env:AI_MODEL = "gpt-4.1-mini"
mvn spring-boot:run
```

模型请求默认强制直连，不继承 IDE、JVM 或 Windows 系统代理。如确需使用系统代理，请在启动前设置 `$env:AI_USE_SYSTEM_PROXY = "true"`。配置变更后需要重启后端。

服务地址：`http://127.0.0.1:8081`

## 接口

`POST /api/ai/completion`

```json
{
  "cursorContext": "小说正文结尾",
  "maxLength": 120
}
```

```json
{
  "completion": "续写内容",
  "source": "demo"
}
```
