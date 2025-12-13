const express = require('express');
const { PrismaClient } = require('@prisma/client');
const { authMiddleware, adminMiddleware } = require('../middleware/auth');

const router = express.Router();
const prisma = new PrismaClient();

// 所有接口都需要认证 + 管理员权限
router.use(authMiddleware);
router.use(adminMiddleware);

// 获取所有用户
router.get('/users', async (req, res) => {
    try {
        const { page = 1, limit = 20, search } = req.query;
        const skip = (page - 1) * limit;

        const where = search ? {
            OR: [
                { email: { contains: search, mode: 'insensitive' } },
                { nickname: { contains: search, mode: 'insensitive' } }
            ]
        } : {};

        const [users, total] = await Promise.all([
            prisma.user.findMany({
                where,
                skip,
                take: parseInt(limit),
                orderBy: { createdAt: 'desc' },
                select: {
                    id: true,
                    email: true,
                    nickname: true,
                    avatar: true,
                    isAdmin: true,
                    isBanned: true,
                    createdAt: true,
                    _count: {
                        select: {
                            favorites: true,
                            playlists: true,
                            playHistory: true
                        }
                    }
                }
            }),
            prisma.user.count({ where })
        ]);

        res.json({
            users,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / limit)
            }
        });
    } catch (error) {
        console.error('Get users error:', error);
        res.status(500).json({ error: 'Failed to get users' });
    }
});

// 获取单个用户详情
router.get('/users/:id', async (req, res) => {
    try {
        const { id } = req.params;

        const user = await prisma.user.findUnique({
            where: { id },
            include: {
                settings: true,
                _count: {
                    select: {
                        favorites: true,
                        playlists: true,
                        playHistory: true
                    }
                }
            }
        });

        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        res.json({ user });
    } catch (error) {
        console.error('Get user error:', error);
        res.status(500).json({ error: 'Failed to get user' });
    }
});

// 封禁/解封用户
router.post('/users/:id/ban', async (req, res) => {
    try {
        const { id } = req.params;
        const { ban } = req.body;

        const user = await prisma.user.update({
            where: { id },
            data: { isBanned: ban }
        });

        res.json({
            message: ban ? 'User banned' : 'User unbanned',
            user: { id: user.id, isBanned: user.isBanned }
        });
    } catch (error) {
        console.error('Ban user error:', error);
        res.status(500).json({ error: 'Failed to update user' });
    }
});

// 设置/取消管理员
router.post('/users/:id/admin', async (req, res) => {
    try {
        const { id } = req.params;
        const { isAdmin } = req.body;

        const user = await prisma.user.update({
            where: { id },
            data: { isAdmin }
        });

        res.json({
            message: isAdmin ? 'User is now admin' : 'Admin removed',
            user: { id: user.id, isAdmin: user.isAdmin }
        });
    } catch (error) {
        console.error('Set admin error:', error);
        res.status(500).json({ error: 'Failed to update user' });
    }
});

// 获取统计数据
router.get('/stats', async (req, res) => {
    try {
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const [
            totalUsers,
            todayUsers,
            totalFavorites,
            totalPlaylists,
            totalPlayHistory,
            recentUsers
        ] = await Promise.all([
            prisma.user.count(),
            prisma.user.count({ where: { createdAt: { gte: today } } }),
            prisma.favorite.count(),
            prisma.playlist.count(),
            prisma.playHistory.count(),
            prisma.user.findMany({
                take: 10,
                orderBy: { createdAt: 'desc' },
                select: { id: true, email: true, nickname: true, createdAt: true }
            })
        ]);

        res.json({
            stats: {
                totalUsers,
                todayUsers,
                totalFavorites,
                totalPlaylists,
                totalPlayHistory
            },
            recentUsers
        });
    } catch (error) {
        console.error('Get stats error:', error);
        res.status(500).json({ error: 'Failed to get stats' });
    }
});

// 删除用户
router.delete('/users/:id', async (req, res) => {
    try {
        const { id } = req.params;

        // 防止删除自己
        if (id === req.user.id) {
            return res.status(400).json({ error: 'Cannot delete yourself' });
        }

        await prisma.user.delete({ where: { id } });

        res.json({ message: 'User deleted' });
    } catch (error) {
        console.error('Delete user error:', error);
        res.status(500).json({ error: 'Failed to delete user' });
    }
});

module.exports = router;
