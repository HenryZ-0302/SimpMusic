require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { PrismaClient } = require('@prisma/client');
const { execSync } = require('child_process');

const authRoutes = require('./routes/auth');
const syncRoutes = require('./routes/sync');
const adminRoutes = require('./routes/admin');
const announcementsRoutes = require('./routes/announcements');

const app = express();
const prisma = new PrismaClient();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Health check
app.get('/', (req, res) => {
    res.json({
        status: 'ok',
        message: 'HYMusic API is running',
        version: '1.0.0'
    });
});

app.get('/health', (req, res) => {
    res.json({ status: 'healthy' });
});

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/sync', syncRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/announcements', announcementsRoutes);

// Error handler
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ error: 'Something went wrong!' });
});

// Start server with DB initialization
async function main() {
    try {
        // 尝试初始化数据库
        console.log('📦 Initializing database...');
        execSync('npx prisma db push --skip-generate --accept-data-loss', { stdio: 'inherit' });
        console.log('✅ Database initialized');
    } catch (error) {
        console.log('⚠️ Database init skipped (may already exist)');
    }

    await prisma.$connect();
    console.log('✅ Database connected');

    app.listen(PORT, () => {
        console.log(`🚀 HYMusic API running on port ${PORT}`);
    });
}

main().catch(console.error);
