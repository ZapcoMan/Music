# 代码覆盖率快速指南

## 🚀 快速开始

### 1. 运行测试并生成报告

```bash
cd Music-backend
mvn clean test
```

### 2. 查看 HTML 报告

```bash
# Windows
start target/site/jacoco/index.html

# Mac
open target/site/jacoco/index.html

# Linux
xdg-open target/site/jacoco/index.html
```

---

## 📊 当前覆盖率要求

| 指标 | 最低要求 | 状态 |
|------|---------|------|
| 类覆盖率 | ≥ 60% | ⏳ 待检查 |
| 方法覆盖率 | ≥ 50% | ⏳ 待检查 |
| 行覆盖率 | ≥ 40% | ⏳ 待检查 |
| 分支覆盖率 | ≥ 30% | ⏳ 待检查 |

---

## 📁 报告位置

```
target/site/jacoco/
├── index.html          ← 打开这个文件
├── jacoco-resources/
├── com.shanzhu.music/
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   └── ...
└── jacoco.xml
```

---

## 🎨 报告说明

- 🟩 **绿色**: 已覆盖的代码
- 🟥 **红色**: 未覆盖的代码
- 🟨 **黄色**: 部分覆盖的分支

---

## 💡 常用命令

```bash
# 只运行测试,不检查覆盖率
mvn test -Djacoco.skip=true

# 只生成报告,不运行测试
mvn jacoco:report

# 查看覆盖率检查是否通过
mvn verify

# 跳过覆盖率检查
mvn test -DskipChecks=true
```

---

## 🔧 调整要求

编辑 `pom.xml` 中的 `<minimum>` 值:

```xml
<limit>
    <counter>LINE</counter>
    <value>COVEREDRATIO</value>
    <minimum>0.80</minimum>  <!-- 改为 80% -->
</limit>
```

---

## ❓ 常见问题

**Q: 构建失败怎么办?**  
A: 查看错误信息,补充测试用例或降低覆盖率要求

**Q: 如何排除某些类?**  
A: 在 pom.xml 中添加 `<excludes>` 配置

**Q: 覆盖率在哪里看?**  
A: 打开 `target/site/jacoco/index.html`

---

📖 详细文档: [CODE_COVERAGE.md](CODE_COVERAGE.md)
