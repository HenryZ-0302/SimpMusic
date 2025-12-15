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
        console.log(`📤 [Sync Get All] User: ${userId}`);

        const [favorites, playlists, history, settings, syncedAlbums, syncedArtists, syncedYouTubePlaylists] = await Promise.all([
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
            prisma.userSettings.findUnique({ where: { userId } }),

            // Library
            prisma.syncedAlbum.findMany({ where: { userId } }),
            prisma.syncedArtist.findMany({ where: { userId } }),
            prisma.syncedYouTubePlaylist.findMany({ where: { userId } })
        ]);

        console.log(`✅ [Sync Get All] Favorites: ${favorites.length}, Playlists: ${playlists.length}, History: ${history.length}, Library: (Alb:${syncedAlbums.length}, Art:${syncedArtists.length}, PL:${syncedYouTubePlaylists.length})`);

        res.json({
            favorites,
            playlists,
            history,
            settings,
            library: {
                albums: syncedAlbums,
                artists: syncedArtists,
                playlists: syncedYouTubePlaylists
            }
        });
    } catch (error) {
        console.error('❌ [Sync Get All] Error:', error);
        res.status(500).json({ error: 'Failed to fetch sync data' });
    }
});

// 同步库 (Albums, Artists, YouTube Playlists) - 全量覆盖
router.post('/library', async (req, res) => {
    try {
        const { albums, artists, playlists } = req.body;
        const userId = req.user.id;

        console.log(`📥 [Sync Library] User: ${userId}`);

        await prisma.$transaction(async (tx) => {
            // 1. 清除旧数据
            await tx.syncedAlbum.deleteMany({ where: { userId } });
            await tx.syncedArtist.deleteMany({ where: { userId } });
            await tx.syncedYouTubePlaylist.deleteMany({ where: { userId } });

            // 2. 插入新数据
            if (albums && albums.length > 0) {
                await tx.syncedAlbum.createMany({
                    data: albums.map(item => ({
                        userId,
                        browseId: item.browseId,
                        title: item.title,
                        artist: item.artist,
                        thumbnail: item.thumbnail
                    }))
                });
            }

            if (artists && artists.length > 0) {
                await tx.syncedArtist.createMany({
                    data: artists.map(item => ({
                        userId,
                        channelId: item.channelId,
                        name: item.name,
                        thumbnail: item.thumbnail
                    }))
                });
            }

            if (playlists && playlists.length > 0) {
                await tx.syncedYouTubePlaylist.createMany({
                    data: playlists.map(item => ({
                        userId,
                        playlistId: item.playlistId,
                        title: item.title,
                        thumbnail: item.thumbnail
                    }))
                });
            }
        });

        res.json({ message: 'Library synced successfully' });
    } catch (error) {
        console.error('❌ [Sync Library] Error:', error);
        res.status(500).json({ error: 'Failed to sync library' });
    }
});

// 同步收藏
router.post('/favorites', async (req, res) => {
    try {
        const { favorites } = req.body;
        const userId = req.user.id;

        console.log(`📥 [Sync Favorites] User: ${userId}, Count: ${favorites?.length || 0}`);

        if (!favorites || favorites.length === 0) {
            console.log(`⚠️ [Sync Favorites] No favorites to sync`);
            return res.json({ message: 'No favorites to sync', count: 0 });
        }

        // 批量创建或更新
        for (const fav of favorites) {
            console.log(`   → Syncing: ${fav.videoId} - ${fav.title}`);
            await prisma.favorite.upsert({
                where: { userId_videoId: { userId, videoId: fav.videoId } },
                create: { userId, ...fav },
                update: fav
            });
        }

        console.log(`✅ [Sync Favorites] Success: ${favorites.length} songs synced`);
        res.json({ message: 'Favorites synced', count: favorites.length });
    } catch (error) {
        console.error('❌ [Sync Favorites] Error:', error);
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

        console.log(`📥 [Sync Playlists] User: ${userId}, Count: ${playlists?.length || 0}`);

        // 1. 删除该用户所有播放列表（级联删除会处理 songs）
        await prisma.playlist.deleteMany({ where: { userId } });

        // 2. 重新创建播放列表
        if (playlists && playlists.length > 0) {
            for (const playlist of playlists) {
                const { title, description, thumbnail, songs } = playlist;
                console.log(`   → Syncing playlist: ${title} with ${songs?.length || 0} songs`);
                await prisma.playlist.create({
                    data: {
                        userId,
                        title,
                        description,
                        thumbnail,
                        songs: {
                            create: songs?.map((song, index) => ({
                                ...song,
                                order: index
                            })) || []
                        }
                    }
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
