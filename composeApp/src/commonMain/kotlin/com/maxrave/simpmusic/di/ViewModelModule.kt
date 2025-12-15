package com.maxrave.simpmusic.di

import com.maxrave.simpmusic.api.HYMusicApiService
import com.maxrave.simpmusic.viewModel.AlbumViewModel
import com.maxrave.simpmusic.viewModel.ArtistViewModel
import com.maxrave.simpmusic.viewModel.HomeViewModel
import com.maxrave.simpmusic.viewModel.LibraryDynamicPlaylistViewModel
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import com.maxrave.simpmusic.viewModel.LocalPlaylistViewModel
import com.maxrave.simpmusic.viewModel.LogInViewModel
import com.maxrave.simpmusic.viewModel.MoodViewModel
import com.maxrave.simpmusic.viewModel.MoreAlbumsViewModel
import com.maxrave.simpmusic.viewModel.NotificationViewModel
import com.maxrave.simpmusic.viewModel.NowPlayingBottomSheetViewModel
import com.maxrave.simpmusic.viewModel.PlaylistViewModel
import com.maxrave.simpmusic.viewModel.PodcastViewModel
import com.maxrave.simpmusic.viewModel.RecentlySongsViewModel
import com.maxrave.simpmusic.viewModel.SearchViewModel
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.sync.SyncManager
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        // HYMusic API 服务 (单例，注入 DataStoreManager)
        single { HYMusicApiService(get()) }
        
        // 数据同步管理器 (单例，注入 apiService, songRepository, localPlaylistRepository, dataStoreManager)
        single {
            SyncManager(
                apiService = get(),
                songRepository = get(),
                localPlaylistRepository = get(),
                albumRepository = get(),
                artistRepository = get(),
                playlistRepository = get(),
                dataStoreManager = get()
            )
        }
        
        single {
            SharedViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        single {
            SearchViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            NowPlayingBottomSheetViewModel(
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LibraryViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LibraryDynamicPlaylistViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            AlbumViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            HomeViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            SettingsViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            ArtistViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            PlaylistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LogInViewModel(
                get(),
            )
        }
        viewModel {
            PodcastViewModel(
                get(),
            )
        }
        viewModel {
            MoreAlbumsViewModel(
                get(),
            )
        }
        viewModel {
            RecentlySongsViewModel(
                get(),
            )
        }
        viewModel {
            LocalPlaylistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            NotificationViewModel(
                get(),
            )
        }
        viewModel {
            MoodViewModel(
                get(),
                get(),
            )
        }
    }