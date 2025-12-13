const express = require('express');
const { PrismaClient } = require('@prisma/client');
const { authMiddleware } = require('../middleware/auth');

const router = express.Router();
const prisma = new PrismaClient();

// 所有接口都需要认证
router.use(authMiddleware);

// 获取所有同步数据
router.get('/all', async (req, res) => {
    try {
        const userId = req.user.id;

        const [favorites, playlists, history, settings] = await Promise.all([
            prisma.favorite.findMany({ where: { userId }, orderBy: { createdAt: 'desc' } }),
            prisma.playlist.findMany({
                where: { userId },
                include: { songs: { orderBy: { order: 'asc' } } },
                orderBy: { updatedAt: 'desc' }
            }),
            prisma.playHistory.findMany({
                where: { userId },
                orderBy: { playedAt: 'desc' },
                take: 100  // 最近100条
            }),
            prisma.userSettings.findUnique({ where: { userId } })
        ]);

        res.json({ favorites, playlists, history, settings });
    } catch (error) {
        console.error('Sync all error:', error);
        res.status(500).json({ error: 'Failed to fetch sync data' });
    }
});

// 同步收藏
router.post('/favorites', async (req, res) => {
    try {
        const { favorites } = req.body; // Array of { videoId, title, artist, thumbnail, duration }
        const userId = req.user.id;

        // 批量创建或更新
        for (const fav of favorites) {
            await prisma.favorite.upsert({
                where: { userId_videoId: { userId, videoId: fav.videoId } },
                create: { userId, ...fav },
                update: fav
            });
        }

        res.json({ message: 'Favorites synced', count: favorites.length });
    } catch (error) {
        console.error('Sync favorites error:', error);
        res.status(500).json({ error: 'Failed to sync favorites' });
    }
});

// 删除收藏
router.delete('/favorites/:videoId', async (req, res) => {
    try {
        const { videoId } = req.params;
        const userId = req.user.id;

        await prisma.favorite.delete({
            where: { userId_videoId: { userId, videoId } }
        });

        res.json({ message: 'Favorite removed' });
    } catch (error) {
        console.error('Delete favorite error:', error);
        res.status(500).json({ error: 'Failed to remove favorite' });
    }
});

// 同步播放列表
router.post('/playlists', async (req, res) => {
    try {
        const { playlists } = req.body;
        const userId = req.user.id;

        for (const playlist of playlists) {
            const { id, title, description, thumbnail, songs } = playlist;

            const createdPlaylist = await prisma.playlist.upsert({
                where: { id: id || 'new' },
                create: {
                    userId,
                    title,
                    description,
                    thumbnail,
                    songs: {
                        create: songs?.map((song, index) => ({ ...song, order: index })) || []
                    }
                },
                update: {
                    title,
                    description,
                    thumbnail
                }
            });

            // 更新歌曲列表
            if (songs && createdPlaylist.id) {
                await prisma.playlistSong.deleteMany({ where: { playlistId: createdPlaylist.id } });
                await prisma.playlistSong.createMany({
                    data: songs.map((song, index) => ({
                        playlistId: createdPlaylist.id,
                        ...song,
                        order: index
                    }))
                });
            }
        }

        res.json({ message: 'Playlists synced', count: playlists.length });
    } catch (error) {
        console.error('Sync playlists error:', error);
        res.status(500).json({ error: 'Failed to sync playlists' });
    }
});

// 同步播放历史
router.post('/history', async (req, res) => {
    try {
        const { history } = req.body;
        const userId = req.user.id;

        await prisma.playHistory.createMany({
            data: history.map(item => ({ userId, ...item })),
            skipDuplicates: true
        });

        res.json({ message: 'History synced', count: history.length });
    } catch (error) {
        console.error('Sync history error:', error);
        res.status(500).json({ error: 'Failed to sync history' });
    }
});

// 同步设置
router.post('/settings', async (req, res) => {
    try {
        const { settings } = req.body;
        const userId = req.user.id;

        await prisma.userSettings.upsert({
            where: { userId },
            create: { userId, ...settings },
            update: settings
        });

        res.json({ message: 'Settings synced' });
    } catch (error) {
        console.error('Sync settings error:', error);
        res.status(500).json({ error: 'Failed to sync settings' });
    }
});

module.exports = router;
