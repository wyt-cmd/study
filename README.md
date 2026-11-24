# 在线考试系统

课程实训项目：题库、组卷、限时考试、自动判分和成绩统计。

## 技术栈

- 后端：Java 17、Spring Boot 3、MyBatis-Plus、MySQL 8、Redis、MinIO
- 前端：Vue 3、Element Plus、Vite
- 接口文档：Knife4j，启动后打开 http://127.0.0.1:8080/doc.html

## 功能

| 模块 | 说明 |
| --- | --- |
| 题目 | 分类、增删改查、Excel 导入；重复题干会拦截 |
| 试卷 | 手工组卷、按规则随机抽题；题目不重复，总分必须与设定一致 |
| 考试 | 开始考试、限时交卷、超时自动处理；同一考生同一试卷不能重复开考 |
| 判分 | 客观题全对得分，主观题按关键词比例给整数分 |
| 成绩 | 按试卷汇总、分页、导出 |
| 账号 | 管理员、教师、考生；退出后令牌失效 |

## 本地运行

先启动 MySQL 8 和 Redis。建库脚本在 `docs/sql/schema.sql`。

后端需要这些环境变量，不要把真实口令写进仓库：

| 变量 | 作用 |
| --- | --- |
| `DB_PASSWORD` | MySQL 密码 |
| `JWT_SECRET` | 至少 32 个字符 |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | 文件服务。不上传图片时填占位值即可 |
| `SEED_PASSWORD` | 首次启动时写入三个演示账号的密码 |

```powershell
cd backend
mvn spring-boot:run
```

```powershell
cd frontend
npm install
npm run dev
```

浏览器打开 http://localhost:5173/ 。

演示账号用户名是 `admin`、`teacher`、`student`，密码都是启动时设置的 `SEED_PASSWORD`。本机当前使用的演示密码是 `exam123456`。

## 目录

- `backend`：接口和业务逻辑，业务写在 service 层
- `frontend`：页面
- `docs/sql/schema.sql`：建表脚本
