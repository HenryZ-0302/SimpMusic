const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { PrismaClient } = require('@prisma/client');
const { authMiddleware } = require('../middleware/auth');

const router = express.Router();
const prisma = new PrismaClient();

// 注册
router.post('/register', async (req, res) => {
    try {
        const { email, password, nickname } = req.body;

        // 验证输入
        if (!email || !password) {
            return res.status(400).json({ error: 'Email and password are required' });
        }

        // 验证密码强度
        // 至少8位，包含大小写字母、数字和特殊字符
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
        if (!passwordRegex.test(password)) {
            return res.status(400).json({
                error: 'Password must be at least 8 characters and contain uppercase, lowercase, number, and special character (@$!%*?&)'
            });
        }

        // 检查邮箱是否已注册
        const existingUser = await prisma.user.findUnique({
            where: { email }
        });

        if (existingUser) {
            return res.status(400).json({ error: 'Email already registered' });
        }

        // 加密密码
        const hashedPassword = await bcrypt.hash(password, 10);

        // 创建用户
        const user = await prisma.user.create({
            data: {
                email,
                password: hashedPassword,
                nickname: nickname || email.split('@')[0],
                settings: {
                    create: {} // 创建默认设置
                }
            }
        });

        // 生成 Token
        const token = jwt.sign(
            { userId: user.id },
            process.env.JWT_SECRET,
            { expiresIn: '30d' }
        );

        res.json({
            message: 'Registration successful',
            token,
            user: {
                id: user.id,
                email: user.email,
                nickname: user.nickname
            }
        });
    } catch (error) {
        console.error('Register error:', error);
        res.status(500).json({ error: 'Registration failed' });
    }
});

// 登录
router.post('/login', async (req, res) => {
    try {
        const { email, password } = req.body;

        if (!email || !password) {
            return res.status(400).json({ error: 'Email and password are required' });
        }

        const user = await prisma.user.findUnique({
            where: { email }
        });

        if (!user) {
            return res.status(401).json({ error: 'Invalid credentials' });
        }

        if (user.isBanned) {
            return res.status(403).json({ error: 'User is banned' });
        }

        const validPassword = await bcrypt.compare(password, user.password);
        if (!validPassword) {
            return res.status(401).json({ error: 'Invalid credentials' });
        }

        const token = jwt.sign(
            { userId: user.id },
            process.env.JWT_SECRET,
            { expiresIn: '30d' }
        );

        res.json({
            message: 'Login successful',
            token,
            user: {
                id: user.id,
                email: user.email,
                nickname: user.nickname,
                isAdmin: user.isAdmin
            }
        });
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ error: 'Login failed' });
    }
});

// 获取当前用户
router.get('/me', authMiddleware, async (req, res) => {
    try {
        const user = await prisma.user.findUnique({
            where: { id: req.user.id },
            include: { settings: true }
        });

        res.json({
            user: {
                id: user.id,
                email: user.email,
                nickname: user.nickname,
                avatar: user.avatar,
                isAdmin: user.isAdmin,
                settings: user.settings,
                createdAt: user.createdAt
            }
        });
    } catch (error) {
        console.error('Get me error:', error);
        res.status(500).json({ error: 'Failed to get user info' });
    }
});

// 更新用户信息
router.put('/me', authMiddleware, async (req, res) => {
    try {
        const { nickname, avatar } = req.body;

        const user = await prisma.user.update({
            where: { id: req.user.id },
            data: {
                nickname,
                avatar
            }
        });

        res.json({
            message: 'Profile updated',
            user: {
                id: user.id,
                email: user.email,
                nickname: user.nickname,
                avatar: user.avatar
            }
        });
    } catch (error) {
        console.error('Update me error:', error);
        res.status(500).json({ error: 'Failed to update profile' });
    }
});

module.exports = router;


