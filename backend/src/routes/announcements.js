const express = require('express');
const { PrismaClient } = require('@prisma/client');
const { authMiddleware, adminMiddleware } = require('../middleware/auth');

const router = express.Router();
const prisma = new PrismaClient();

// 获取所有活跃公告（所有用户可访问）
router.get('/', async (req, res) => {
    try {
        const announcements = await prisma.announcement.findMany({
            where: { isActive: true },
            orderBy: [
                { priority: 'desc' },
                { createdAt: 'desc' }
            ]
        });
        res.json(announcements);
    } catch (error) {
        console.error('Get announcements error:', error);
        res.status(500).json({ error: 'Failed to get announcements' });
    }
});

// 获取所有公告（管理员）
router.get('/all', authMiddleware, adminMiddleware, async (req, res) => {
    try {
        const announcements = await prisma.announcement.findMany({
            orderBy: [
                { priority: 'desc' },
                { createdAt: 'desc' }
            ]
        });
        res.json(announcements);
    } catch (error) {
        console.error('Get all announcements error:', error);
        res.status(500).json({ error: 'Failed to get announcements' });
    }
});

// 创建公告（管理员）
router.post('/', authMiddleware, adminMiddleware, async (req, res) => {
    try {
        const { title, content, priority } = req.body;

        if (!title || !content) {
            return res.status(400).json({ error: 'Title and content are required' });
        }

        const announcement = await prisma.announcement.create({
            data: {
                title,
                content,
                priority: priority || 0
            }
        });

        res.json(announcement);
    } catch (error) {
        console.error('Create announcement error:', error);
        res.status(500).json({ error: 'Failed to create announcement' });
    }
});

// 更新公告（管理员）
router.put('/:id', authMiddleware, adminMiddleware, async (req, res) => {
    try {
        const { id } = req.params;
        const { title, content, isActive, priority } = req.body;

        const announcement = await prisma.announcement.update({
            where: { id },
            data: {
                ...(title !== undefined && { title }),
                ...(content !== undefined && { content }),
                ...(isActive !== undefined && { isActive }),
                ...(priority !== undefined && { priority })
            }
        });

        res.json(announcement);
    } catch (error) {
        console.error('Update announcement error:', error);
        res.status(500).json({ error: 'Failed to update announcement' });
    }
});

// 删除公告（管理员）
router.delete('/:id', authMiddleware, adminMiddleware, async (req, res) => {
    try {
        const { id } = req.params;

        await prisma.announcement.delete({
            where: { id }
        });

        res.json({ message: 'Announcement deleted' });
    } catch (error) {
        console.error('Delete announcement error:', error);
        res.status(500).json({ error: 'Failed to delete announcement' });
    }
});

module.exports = router;
