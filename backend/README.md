# HYMusic Backend

HYMusic 后端 API 服务。

## 技术栈

- Node.js + Express
- PostgreSQL + Prisma
- JWT 认证

## 本地开发

```bash
cd backend
npm install
cp .env.example .env  # 编辑 .env 配置数据库
npm run prisma:generate
npm run prisma:push
npm run dev
```

## API 接口

### 认证
- `POST /api/auth/register` - 注册
- `POST /api/auth/login` - 登录
- `GET /api/auth/me` - 获取当前用户

### 数据同步
- `GET /api/sync/all` - 获取所有同步数据
- `POST /api/sync/favorites` - 同步收藏
- `POST /api/sync/playlists` - 同步播放列表
- `POST /api/sync/history` - 同步历史
- `POST /api/sync/settings` - 同步设置

### 管理员
- `GET /api/admin/users` - 用户列表
- `GET /api/admin/stats` - 统计数据
- `POST /api/admin/users/:id/ban` - 封禁用户

## Zeabur 部署

1. 连接 GitHub 仓库
2. 选择 `backend` 目录
3. 添加 PostgreSQL 数据库
4. 设置环境变量:
   - `DATABASE_URL` (自动生成)
   - `JWT_SECRET` (自定义)
