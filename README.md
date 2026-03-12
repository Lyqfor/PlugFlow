# PlugFlow

**通用插件组件开发**：设计并实现**插件化通用组件依赖包**，构建支持**定制化、可扩展、高灵活、可插拔**的插件化组件：

1. 基于**泛型与注解**在应用启动时自动发现并加载本地插件；
2. 在主流程的扩展点以**异步并行**的方式执行满足业务身份的所有插件；
3. 支持**灰度、降级、顺序**等企业级属性，可与 Spring/Spring Boot 无缝集成。

---

## 功能概览

| 能力 | 说明 |
|------|------|
| **插件模版** | 使用 `@PluginTemplate` 在接口上声明插件模版，约定执行入口（继承 `PluginExecutor`） |
| **插件实现** | 使用 `@Plugin(template=...)` 在实现类上声明所属模版，可配置 order、enabled、grayRatio、degrade |
| **自动注册** | 启动时扫描指定包，将「模版 → 实现列表」放入注册表 `PluginRegistry` |
| **泛型调用** | `PluginInvoker.invoke(模板Class, context)` 按模版加载插件并执行，支持异步并行 |
| **灰度 / 降级** | 支持按比例灰度、运行时降级开关，以及自定义 `GrayDecider` |

---

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.plugflow</groupId>
    <artifactId>plugflow-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 定义插件模版（接口）

```java
import com.plugflow.core.PluginContext;
import com.plugflow.core.PluginExecutor;
import com.plugflow.core.annotation.PluginTemplate;

@PluginTemplate(name = "订单扩展", description = "下单流程扩展点")
public interface OrderExtPlugin extends PluginExecutor<PluginContext, Void> {

    @Override
    Void execute(PluginContext context);
}
```

### 3. 实现插件并标注

```java
import com.plugflow.core.PluginContext;
import com.plugflow.core.annotation.Plugin;

@Plugin(
    template = OrderExtPlugin.class,
    name = "赠品插件",
    order = 10,
    enabled = true,
    grayRatio = 100,
    degrade = false
)
public class GiftOrderPlugin implements OrderExtPlugin {

    @Override
    public Void execute(PluginContext context) {
        // 业务逻辑
        return null;
    }
}
```

### 4. 注册与调用

**方式一：纯 Java（无 Spring）**

```java
PluginRegistry registry = new PluginRegistry();
PluginScanner scanner = new PluginScanner(registry);
scanner.scan("com.example.myapp");  // 扫描包

PluginInstanceProvider provider = new DefaultPluginInstanceProvider();
PluginInvoker invoker = new PluginInvoker(registry, provider);

PluginContext ctx = new PluginContext();
ctx.setAttribute("orderId", "ORDER_001");
List<Void> results = invoker.invoke(OrderExtPlugin.class, ctx);
```

**方式二：Spring Boot**

在 `application.yml` 中配置扫描包：

```yaml
plugflow:
  scan-packages: com.example.myapp
```

注入并调用：

```java
@Autowired
private PluginInvoker pluginInvoker;

public void onOrderCreated(Order order) {
    PluginContext ctx = new PluginContext();
    ctx.setAttribute("orderId", order.getId());
    pluginInvoker.invoke(OrderExtPlugin.class, ctx);
}
```

---

## 异步并行执行

```java
CompletableFuture<List<Result>> future = pluginInvoker.invokeAsync(
    MyPluginTemplate.class, context);
future.thenAccept(results -> { ... });
```

---

## 灰度与降级

- **灰度**：`@Plugin(grayRatio = 20)` 表示仅约 20% 流量执行该插件；默认 100 表示全量。可自定义 `GrayDecider` 实现基于用户/租户的一致性灰度。
- **降级**：`@Plugin(degrade = true)` 或运行时调用 `registry.setDegrade(templateClass, pluginClass, true)` 可快速跳过该插件，用于故障止血。

---

## 打包与使用

执行 `mvn clean package` 得到 `plugflow-core-1.0.0-SNAPSHOT.jar`。在其他项目中引入该 JAR（或安装到本地仓库后通过 Maven 依赖引用），即可使用上述注解、注册表与调用器；无需 Spring 时仅依赖 `reflections`，可选引入 `spring-context` / `spring-boot-autoconfigure` 以启用自动配置与从容器获取插件实例。

---

## 模块与类说明

| 类 / 接口 | 说明 |
|-----------|------|
| `@PluginTemplate` | 插件模版注解，标注在接口上 |
| `@Plugin` | 插件注解，标注在实现类上，指定 template、order、enabled、grayRatio、degrade |
| `PluginExecutor<C,R>` | 执行器根接口，模版接口继承它以约定 `execute(context)` |
| `PluginContext` | 执行上下文，可扩展或使用 attributes 传递参数 |
| `PluginRegistry` | 注册表：模版 → 插件定义列表，支持运行时调降级/灰度 |
| `PluginScanner` | 扫描器，按包扫描并注册到 Registry |
| `PluginInvoker` | 调用器：按模版类型加载插件并执行（同步/异步并行） |
| `PlugFlow` | 门面：非 Spring 场景下一行创建 Registry + Scanner + Invoker |

---

## 更推荐的企业级用法（建议）

当你把 PlugFlow 当作“主流程扩展点执行器”来用时，通常还会需要这些能力（本项目已预留扩展点，且可以逐步增强）：

- **失败策略**：某个插件失败是否影响主流程（继续/快速失败/收敛异常）
- **超时控制**：单个插件最大执行时间，避免拖垮主链路
- **结果封装**：每个插件的执行结果、耗时、异常原因等可观测信息
- **一致性灰度**：基于 userId/tenantId 等做稳定命中，而不是纯随机
- **插件选择器**：按业务身份/场景（如渠道、租户、版本）过滤插件集合

后续如果你希望我把这些能力也做成开箱即用（含 API 与测试），我可以在当前代码基础上继续完善。
