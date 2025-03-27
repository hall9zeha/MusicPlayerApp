package com.barryzeha.core.common


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

const val MAIN_FRAGMENT = 0
const val SONG_LIST_FRAGMENT = 1
const val SETTINGS_FRAGMENT = 2

const val BY_ALBUM = 1
const val BY_ARTIST = 2
const val BY_GENRE = 3
const val BY_FAVORITE = 4

const val READ_STORAGE_REQ_CODE = 123
const val RECORD_AUDIO_REQ_CODE = 124


const val MUSIC_PLAYER_SESSION = "MusicPlayerSessionService"
const val HOME_PLAYER = "homePlayer"
const val LIST_PLAYER = "listPlayer"
const val SETTINGS = "settings"
const val SONG_INFO_EXTRA_KEY = "songInfoExtraKey"

// Song mode
const val REPEAT_ALL = 0
const val REPEAT_ONE = 1
const val SHUFFLE = 2
const val AB_LOOP = 3
const val CLEAR_MODE = -1
// Actions custom for media player notify

const val ACTION_CLOSE = "Action close"
const val ACTION_FAVORITE = "Action favorite"
const val CHANNEL_OR_SESSION_ID_EXTRA="channelOrSessionId"


// For animation in the album cover art when make next or prev action
const val NEXT =0
const val PREVIOUS =1
const val DEFAULT_DIRECTION = 2

// Extra intent
const val MUSIC_STATE_EXTRA = "musicState"
const val FAVORITE_STATE_EXTRA = "favoriteState"
