# 代码覆盖率配置（JaCoCo）

## 📋 概述

项目已配置 **JaCoCo (Java Code Coverage)** 插件，用于生成详细的代码覆盖率报告，并在 `verify` 阶段执行门禁。

> ⚠️ **口径更新**：四项阈值已在近期一轮按实测水位上调（留 5–7 个点余量），原先的 60/50/40/30
> 是“挂在那儿永远不会失败的空门禁”——覆盖率再掉 20 个点 CI 也不会响。现状以 `pom.xml` 与本文件为准。

---

## ✅ 已配置内容

### 1. JaCoCo Maven 插件

**版本**: 0.8.12  
**位置**: `pom.xml` `<build><plugins>`

### 2. 三个执行阶段

#### ① prepare-agent（测试前）
- **时机**: 测试执行前
- **作用**: 启动 Java Agent，收集覆盖率数据
- **输出**: `target/jacoco.exec`（二进制数据文件）

#### ② report（测试后）
- **时机**: test 阶段完成后
- **作用**: 生成 HTML/XML/CSV 格式报告
- **输出**: `target/site/jacoco/`（HTML 报告）

#### ③ check（验证）
- **时机**: verify 阶段
- **作用**: 检查覆盖率是否达标
- **失败**: 未达标则构建失败

---

## 📊 覆盖率指标

### 配置的最低要求（现状，以 pom.xml 为准）

| 指标 | 最低要求 | 实测水位 | 说明 |
|------|---------|---------|------|
| **类覆盖率**（CLASS） | ≥ 90% | 97.3% (72/74) | 被测试覆盖的类的比例 |
| **方法覆盖率**（METHOD） | ≥ 80% | 86.2% (355/412) | 被测试覆盖的方法的比例 |
| **行覆盖率**（LINE） | ≥ 70% | 76.7% (1752/2283) | 被测试覆盖的代码行的比例 |
| **分支覆盖率**（BRANCH） | ≥ 50% | 56.5% (543/961) | 被测试覆盖的分支的比例 |

门禁与实测水位均在 `Music-backend/pom.xml` 的 JaCoCo `check` 规则注释中留有出处；
升版时若覆盖率下滑至门禁以下，`mvn -B clean verify`（本地与 CI 同一条命令）会直接失败。
与覆盖率并行的另一道静态门禁是 **SpotBugs 4.8.6.4**（`threshold=High`，报告 0 项；
`spotbugs-exclude.xml` 只压 Lombok `@Data` 的 EI_EXPOSE_REP/REP2 结构性误报，不得用它掩盖真实缺陷）。

### 覆盖率等级参考

| 等级 | 行覆盖率 | 说明 |
|------|---------|------|
| ⭐⭐⭐ 优秀 | ≥ 80% | 高质量测试 |
| ⭐⭐ 良好 | ≥ 60% | 可接受 |
| ⭐ 合格 | ≥ 40% | 基本要求 |
| ❌ 不合格 | < 40% | 需要补充测试 |

---

## 🚀 使用方法

### 1. 运行测试并生成报告

```bash
cd Music-backend

# 运行测试并生成覆盖率报告
mvn clean test

# 或完整生命周期
mvn clean verify
```

### 2. 查看 HTML 报告

```bash
# 在浏览器中打开
# Windows
start target/site/jacoco/index.html

# Mac
open target/site/jacoco/index.html

# Linux
xdg-open target/site/jacoco/index.html
```

### 3. 查看控制台输出

测试完成后会显示覆盖率摘要：

```
[INFO] --- jacoco-maven-plugin:0.8.12:check (check) @ Music-backend ---
[INFO] Loading execution data file /path/to/target/jacoco.exec
[INFO] Analyzed bundle 'Music-backend' with 45 classes
[WARNING] Rule violated for bundle Music-backend: lines covered ratio is 0.35, but expected minimum is 0.40
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
```

---

## 📁 报告文件结构

```
target/
├── jacoco.exec                    # 原始覆盖率数据（二进制）
├── site/
│   └── jacoco/
│       ├── index.html             # 主报告页面
│       ├── jacoco-resources/      # CSS/JS 资源
│       ├── com.shanzhu.music/     # 包级别报告
│       │   ├── index.html
│       │   ├── controller/
│       │   ├── service/
│       │   ├── mapper/
│       │   └── ...
│       └── jacoco.xml             # XML 格式报告
└── surefire-reports/              # 单元测试报告
```

---

## 🎨 HTML 报告说明

### 颜色标记

| 颜色 | 含义 |
|------|------|
| 🟩 绿色 | 已覆盖的代码 |
| 🟥 红色 | 未覆盖的代码 |
| 🟨 黄色 | 部分覆盖的分支 |

### 报告层级

1. **项目级别**: 所有包的总览
2. **包级别**: 每个包的覆盖率
3. **类级别**: 每个类的详细 coverage
4. **方法级别**: 每个方法的 coverage
5. **行级别**: 每行代码的覆盖状态

---

## 🔧 自定义配置

### 1. 调整覆盖率要求

编辑 `pom.xml`：

```xml
<execution>
    <id>check</id>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>  <!-- 提高到 80% -->
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

### 2. 排除某些类/包

```xml
<configuration>
    <excludes>
        <!-- 排除实体类 -->
        <exclude>**/entity/**/*.class</exclude>
        <!-- 排除配置类 -->
        <exclude>**/config/**/*.class</exclude>
        <!-- 排除工具类 -->
        <exclude>**/utils/**/*.class</exclude>
    </excludes>
</configuration>
```

### 3. 只统计特定包

```xml
<configuration>
    <includes>
        <include>**/service/**/*.class</include>
        <include>**/controller/**/*.class</include>
    </includes>
</configuration>
```

---

## 💡 最佳实践

### 1. 合理设置覆盖率目标

```
开发阶段: 40%  （快速迭代）
测试阶段: 60%  （质量保证）
发布阶段: 80%  （生产标准）
```

### 2. 关注关键模块

优先保证以下模块的高覆盖率：
- ✅ Service 层业务逻辑
- ✅ Controller 层接口
- ✅ 工具类核心方法
- ❌ 实体类（getter/setter）
- ❌ 配置类

### 3. 定期审查报告

- 每周查看覆盖率趋势
- 识别未覆盖的关键代码
- 补充缺失的测试用例

### 4. 不要盲目追求 100%

- 某些代码难以测试（异常处理、边界条件）
- 过度测试会降低开发效率
- 质量 > 数量

---

## 🔍 常见问题

### Q1: 覆盖率为什么这么低？

**A**: 可能原因：
1. 缺少单元测试
2. 测试用例不够全面
3. 包含了大量简单类（entity/config）

**解决方案**：
- 补充 Service 层测试
- 增加边界条件测试
- 排除不需要测试的类

### Q2: 如何查看具体哪些代码未覆盖？

**A**: 
1. 打开 HTML 报告
2. 点击红色的类
3. 查看红色标记的行

### Q3: 构建失败怎么办？

**A**: 
```
[ERROR] Rule violated: lines covered ratio is 0.35, but expected minimum is 0.40
```

**解决方案**：
1. 补充测试用例
2. 或临时降低要求（不推荐）
3. 或排除某些类

### Q4: 如何跳过覆盖率检查？

**A**: 
```bash
# 跳过 JaCoCo
mvn clean test -Djacoco.skip=true

# 或跳过 check 阶段
mvn clean test -DskipChecks=true
```

---

## 📊 当前项目覆盖率目标

根据项目实际情况，设定合理的目标：

| 模块 | 目标覆盖率 | 优先级 |
|------|-----------|--------|
| Service 层 | ≥ 70% | 🔴 高 |
| Controller 层 | ≥ 60% | 🟡 中 |
| Utils 工具类 | ≥ 50% | 🟡 中 |
| Entity 实体类 | ≥ 20% | 🟢 低 |
| Config 配置类 | 可选 | 🟢 低 |

---

## 🎯 下一步优化

### 1. 提高覆盖率
- 补充 Service 层测试
- 增加异常场景测试
- 添加边界条件测试

### 2. 集成 SonarQube
- 更详细的代码质量分析
- 历史趋势追踪
- 团队协作改进

### 3. 自动化监控
- CI/CD 集成
- 覆盖率下降告警
- 定期报告生成

---

## 📖 参考文档

- [JaCoCo 官方文档](https://www.jacoco.org/jacoco/trunk/doc/)
- [Maven JaCoCo Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [Code Coverage Best Practices](https://testing.googleblog.com/2016/03/code-coverage-best-practices.html)

---

**配置完成时间**: 2026-05-07（阈值于 2026-11 按实测水位上调）  
**JaCoCo 版本**: 0.8.12  
**覆盖率要求**: 类 90% / 方法 80% / 行 70% / 分支 50%
