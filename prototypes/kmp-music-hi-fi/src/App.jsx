import { useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowLeftIcon as ArrowLeft,
  ArrowsClockwiseIcon as ArrowsClockwise,
  CaretRightIcon as CaretRight,
  CheckCircleIcon as CheckCircle,
  DesktopIcon as Desktop,
  DevicesIcon as Devices,
  DotsThreeVerticalIcon as DotsThreeVertical,
  DownloadSimpleIcon as DownloadSimple,
  EnvelopeSimpleIcon as EnvelopeSimple,
  FolderOpenIcon as FolderOpen,
  FolderSimpleIcon as FolderSimple,
  GearSixIcon as GearSix,
  HeartIcon as Heart,
  HouseIcon as House,
  ListBulletsIcon as ListBullets,
  LockSimpleIcon as LockSimple,
  MagnifyingGlassIcon as MagnifyingGlass,
  MicrophoneStageIcon as MicrophoneStage,
  MoonIcon as Moon,
  MusicNotesIcon as MusicNotes,
  PauseIcon as Pause,
  PlayIcon as Play,
  RepeatIcon as Repeat,
  ScanIcon as Scan,
  ShieldCheckIcon as ShieldCheck,
  ShuffleIcon as Shuffle,
  SignInIcon as SignIn,
  SkipBackIcon as SkipBack,
  SkipForwardIcon as SkipForward,
  SlidersHorizontalIcon as SlidersHorizontal,
  SpeakerHighIcon as SpeakerHigh,
  StarIcon as Star,
  SunIcon as Sun,
  TimerIcon as Timer,
  TrashIcon as Trash,
  UserIcon as User,
  VinylRecordIcon as VinylRecord,
  WarningCircleIcon as WarningCircle,
  XIcon as X,
} from "@phosphor-icons/react";

import albumBestOfMe from "./assets/album-best-of-me.png";
import albumRiverYear from "./assets/album-river-year.png";
import albumTimeForest from "./assets/album-time-forest.png";
import coverSeaDream from "./assets/cover-sea-dream.png";
import coverSummerWaltz from "./assets/cover-summer-waltz.png";
import heroLocalFolder from "./assets/hero-local-folder.png";

const albums = [
  {
    id: "river-year",
    title: "似水流年",
    artist: "旅行团乐队",
    count: 12,
    cover: albumRiverYear,
    mood: "清晨海风",
    year: "2026",
  },
  {
    id: "best-of-me",
    title: "The Best of Me",
    artist: "A-Lin",
    count: 15,
    cover: albumBestOfMe,
    mood: "安静人声",
    year: "2024",
  },
  {
    id: "time-forest",
    title: "时光森林",
    artist: "苏打绿",
    count: 10,
    cover: albumTimeForest,
    mood: "森林漫游",
    year: "2025",
  },
  {
    id: "dream-stories",
    title: "Dream Stories",
    artist: "久石让",
    count: 18,
    cover: coverSummerWaltz,
    mood: "钢琴叙事",
    year: "2021",
  },
];

const featuredSongs = [
  {
    id: "sea-dream",
    title: "海边的梦",
    artist: "旅行团乐队",
    album: "似水流年",
    duration: "3:45",
    cover: coverSeaDream,
    liked: true,
    plays: "今天 08:12",
    quality: "本地 FLAC",
    lyric: "海风吹过窗边，像一段刚醒来的旋律。",
  },
  {
    id: "summer-waltz",
    title: "Summer Waltz",
    artist: "久石让",
    album: "Dream Stories",
    duration: "4:25",
    cover: coverSummerWaltz,
    liked: false,
    plays: "昨天 22:18",
    quality: "本地 AAC",
    lyric: "琴键落下时，夏天慢慢转身。",
  },
  {
    id: "river",
    title: "像水流年",
    artist: "旅行团乐队",
    album: "似水流年",
    duration: "3:58",
    cover: albumRiverYear,
    liked: true,
    plays: "周二 19:42",
    quality: "本地 FLAC",
    lyric: "时间贴着掌心流过，带走一点点喧哗。",
  },
  {
    id: "best",
    title: "The Best of Me",
    artist: "A-Lin",
    album: "The Best of Me",
    duration: "4:07",
    cover: albumBestOfMe,
    liked: true,
    plays: "周一 12:34",
    quality: "本地 ALAC",
    lyric: "人声靠近时，世界忽然变得很轻。",
  },
  {
    id: "forest",
    title: "时光森林",
    artist: "苏打绿",
    album: "时光森林",
    duration: "5:11",
    cover: albumTimeForest,
    liked: false,
    plays: "6 月 13 日",
    quality: "本地 MP3",
    lyric: "林间的光斑，一路跳到副歌里。",
  },
  {
    id: "long-night",
    title: "夜航片段",
    artist: "陈婧霏",
    album: "夜航",
    duration: "3:36",
    cover: albumBestOfMe,
    liked: false,
    plays: "6 月 10 日",
    quality: "本地 AAC",
    lyric: "夜色铺开，低频像远处的灯。",
  },
];

const albumTrackCatalog = {
  "river-year": [
    { title: "海边的梦", duration: "3:45" },
    { title: "像水流年", duration: "3:58" },
    { title: "沿岸公路", duration: "4:11" },
    { title: "清晨浪花", duration: "3:27" },
    { title: "风把云吹远", duration: "4:03" },
    { title: "南方车站", duration: "3:52" },
    { title: "雨后码头", duration: "4:19" },
    { title: "慢慢靠岸", duration: "3:31" },
    { title: "月光旧信", duration: "4:06" },
    { title: "潮汐电台", duration: "3:40" },
    { title: "远处的灯", duration: "4:24" },
    { title: "回到海边", duration: "3:49" },
  ],
  "best-of-me": [
    { title: "The Best of Me", duration: "4:07" },
    { title: "给我一个理由忘记", duration: "4:43" },
    { title: "有一种悲伤", duration: "4:11" },
    { title: "失恋无罪", duration: "4:27" },
    { title: "幸福了 然后呢", duration: "4:35" },
    { title: "寂寞不痛", duration: "4:19" },
    { title: "以前以后", duration: "4:32" },
    { title: "难得", duration: "4:01" },
    { title: "我们会更好的", duration: "4:18" },
    { title: "做我自己", duration: "3:52" },
    { title: "爱上你等于爱上寂寞", duration: "4:36" },
    { title: "四季", duration: "4:08" },
    { title: "今晚你想念的人是不是我", duration: "4:25" },
    { title: "大大的拥抱", duration: "3:58" },
    { title: "声声慢", duration: "4:40" },
  ],
  "time-forest": [
    { title: "时光森林", duration: "5:11" },
    { title: "小情歌", duration: "4:33" },
    { title: "无与伦比的美丽", duration: "4:55" },
    { title: "我好想你", duration: "5:24" },
    { title: "频率", duration: "4:12" },
    { title: "迟到千年", duration: "4:44" },
    { title: "他夏了夏天", duration: "4:31" },
    { title: "故事", duration: "4:08" },
    { title: "你被写在我的歌里", duration: "4:37" },
    { title: "早点回家", duration: "4:02" },
  ],
  "dream-stories": [
    { title: "Summer Waltz", duration: "4:25" },
    { title: "One Summer's Day", duration: "4:08" },
    { title: "The Rain", duration: "5:02" },
    { title: "A Town With an Ocean View", duration: "3:37" },
    { title: "Merry-Go-Round", duration: "5:16" },
    { title: "Silent Garden", duration: "3:54" },
    { title: "Dream Stories", duration: "4:48" },
    { title: "Moonlit Piano", duration: "3:42" },
    { title: "Blue Train", duration: "4:13" },
    { title: "Paper Airship", duration: "3:51" },
    { title: "Rainy Window", duration: "4:20" },
    { title: "Little Journey", duration: "3:35" },
    { title: "Forest Postcard", duration: "4:06" },
    { title: "Wind Theme", duration: "3:58" },
    { title: "Night Lantern", duration: "4:29" },
    { title: "Old Theater", duration: "4:15" },
    { title: "Goodbye Waltz", duration: "3:47" },
    { title: "After Summer", duration: "4:34" },
  ],
};

const initialQueueIds = featuredSongs.map((song) => song.id);

const initialSongs = buildInitialSongs(featuredSongs);

const artists = [
  { id: "trip", name: "旅行团乐队", count: 18, cover: albumRiverYear, tag: "独立流行" },
  { id: "alin", name: "A-Lin", count: 15, cover: albumBestOfMe, tag: "华语人声" },
  { id: "sodagreen", name: "苏打绿", count: 10, cover: albumTimeForest, tag: "乐团精选" },
  { id: "hisaishi", name: "久石让", count: 18, cover: coverSummerWaltz, tag: "器乐原声" },
];

const navItems = [
  { key: "home", label: "首页", icon: House },
  { key: "favorites", label: "收藏", icon: Heart },
  { key: "me", label: "我的", icon: User },
];

const topLevelViews = ["home", "favorites", "me"];

function formatSongCount(count) {
  return `${count} 首`;
}

function songKey(song) {
  return `${song.album}::${song.title}`;
}

function buildInitialSongs(seedSongs) {
  const seedByKey = new Map(seedSongs.map((song) => [songKey(song), song]));
  const catalogSongs = albums.flatMap((album) => {
    const tracks = albumTrackCatalog[album.id] || [];

    return tracks.map((track, index) => {
      const seededSong = seedByKey.get(`${album.title}::${track.title}`);

      if (seededSong) {
        return {
          ...seededSong,
          trackNumber: index + 1,
        };
      }

      return {
        id: `${album.id}-${String(index + 1).padStart(2, "0")}`,
        title: track.title,
        artist: album.artist,
        album: album.title,
        duration: track.duration,
        cover: album.cover,
        liked: false,
        plays: "本地专辑",
        quality: album.id === "time-forest" ? "本地 MP3" : "本地 FLAC",
        lyric: `${album.mood}里的一段旋律。`,
        trackNumber: index + 1,
      };
    });
  });
  const catalogKeys = new Set(catalogSongs.map(songKey));
  const extraSeedSongs = seedSongs.filter((song) => !catalogKeys.has(songKey(song)));

  return [...catalogSongs, ...extraSeedSongs];
}

function AppHeader({ title, subtitle, onSearch, onSettings, onBack, compact = false }) {
  return (
    <header className={`app-header ${compact ? "compact" : ""}`}>
      <div>
        {onBack ? (
          <button className="back-button" type="button" onClick={onBack} aria-label="返回">
            <ArrowLeft size={22} weight="bold" />
          </button>
        ) : null}
        <h1>{title}</h1>
        {subtitle ? <p>{subtitle}</p> : null}
      </div>
      <div className="header-actions">
        {onSettings ? (
          <button className="round-icon" type="button" onClick={onSettings} aria-label="设置">
            <GearSix size={23} />
          </button>
        ) : null}
        {onSearch ? (
          <button className="round-icon strong" type="button" onClick={onSearch} aria-label="搜索">
            <MagnifyingGlass size={27} />
          </button>
        ) : null}
      </div>
    </header>
  );
}

function SectionTitle({ title, meta, actionLabel = "全部", actionAriaLabel, onAction }) {
  return (
    <div className="section-title">
      <h2>
        {title}
        {meta ? <span>{meta}</span> : null}
      </h2>
      {onAction ? (
        <button type="button" onClick={onAction} aria-label={actionAriaLabel || actionLabel}>
          {actionLabel}
          <CaretRight size={18} weight="bold" />
        </button>
      ) : null}
    </div>
  );
}

function SongRow({ song, active = false, currentSongId, onOpen, onPlay, onMore, onLike, dense = false }) {
  const isCurrentSong = active || song.id === currentSongId;

  return (
    <article className={`song-row ${isCurrentSong ? "active" : ""} ${dense ? "dense" : ""}`} aria-current={isCurrentSong ? "true" : undefined}>
      <button className="song-main" type="button" onClick={() => onOpen(song)} aria-label={`打开 ${song.title} 播放页`}>
        <img src={song.cover} alt={`${song.title} 封面`} />
        <span className="song-copy">
          <strong>{song.title}</strong>
          <span>{song.artist} · {song.album}</span>
          <small>
            <i aria-hidden="true" />
            {isCurrentSong ? <b>播放中</b> : null}
            {song.duration}
          </small>
        </span>
      </button>
      <div className="song-actions">
        {onLike ? (
          <button
            className={`tiny-icon like ${song.liked ? "is-liked" : ""}`}
            type="button"
            onClick={() => onLike(song.id)}
            aria-label={song.liked ? "取消收藏" : "收藏"}
          >
            <Heart size={20} weight={song.liked ? "fill" : "regular"} />
          </button>
        ) : null}
        <button className="tiny-icon play" type="button" onClick={() => onPlay(song)} aria-label={`播放 ${song.title}`}>
          <Play size={18} weight="fill" />
        </button>
        <button className="ghost-dots" type="button" onClick={() => onMore(song)} aria-label={`${song.title} 更多操作`}>
          <DotsThreeVertical size={25} weight="bold" />
        </button>
      </div>
    </article>
  );
}

function AlbumCard({ album, onOpen }) {
  return (
    <button className="album-card" type="button" onClick={() => onOpen(album)}>
      <img src={album.cover} alt={`${album.title} 专辑封面`} />
      <strong>{album.title}</strong>
      <span>{album.artist}</span>
      <small>{formatSongCount(album.count)}</small>
    </button>
  );
}

function HomeView({ songs, currentSong, currentSongId, onSearch, onScan, onLocalFolder, onSongOpen, onSongPlay, onMore, onAlbumOpen }) {
  const recent = songs.slice(0, 2);

  return (
    <>
      <AppHeader title="首页" subtitle="本地音乐 · 随时随地畅听" onSearch={onSearch} />

      <section className="library-card">
        <div className="library-copy">
          <span>本地音乐库</span>
          <div className="library-count">
            <strong>1,248</strong>
            <b>首歌曲</b>
          </div>
          <p>86 张专辑 · 128 位歌手</p>
          <div className="library-actions">
            <button className="primary-pill" type="button" onClick={onScan}>
              <Scan size={22} weight="bold" />
              扫描本地音乐
            </button>
            <button className="soft-pill" type="button" onClick={onLocalFolder} aria-label="打开本地文件夹">
              <FolderOpen size={23} />
            </button>
          </div>
        </div>
        <img className="folder-art" src={heroLocalFolder} alt="本地音乐库文件夹插画" />
      </section>

      <SectionTitle title="最近播放" actionLabel="全部" actionAriaLabel="查看全部最近播放歌曲" onAction={onSearch} />
      <div className="stack-list">
        {recent.map((song) => (
          <SongRow
            key={song.id}
            song={song}
            active={currentSong.id === song.id}
            currentSongId={currentSongId}
            onOpen={onSongOpen}
            onPlay={onSongPlay}
            onMore={onMore}
          />
        ))}
      </div>

      <SectionTitle title="本地专辑" actionLabel="更多" actionAriaLabel="查看更多本地专辑" onAction={() => onAlbumOpen(albums[0])} />
      <div className="album-strip">
        {albums.slice(0, 3).map((album) => (
          <AlbumCard key={album.id} album={album} onOpen={onAlbumOpen} />
        ))}
      </div>
    </>
  );
}

function FavoritesView({ songs, currentSongId, selectedType, onType, onSongOpen, onSongPlay, onMore, onLike, onAlbumOpen, onArtistOpen }) {
  const likedSongs = songs.filter((song) => song.liked);
  const likedAlbums = albums.filter((album) => likedSongs.some((song) => song.album === album.title));
  const likedArtists = artists.filter((artist) => likedSongs.some((song) => song.artist === artist.name));

  return (
    <>
      <AppHeader title="收藏" subtitle="喜欢的音乐都在这里" />
      <Segmented
        value={selectedType}
        onChange={onType}
        options={[
          { key: "songs", label: "歌曲" },
          { key: "albums", label: "专辑" },
          { key: "artists", label: "歌手" },
        ]}
      />

      {selectedType === "songs" ? (
        <div className="stack-list padded-top">
          {likedSongs.map((song) => (
            <SongRow
              key={song.id}
              song={song}
              currentSongId={currentSongId}
              active={false}
              onOpen={onSongOpen}
              onPlay={onSongPlay}
              onMore={onMore}
              onLike={onLike}
            />
          ))}
        </div>
      ) : null}

      {selectedType === "albums" ? (
        <div className="album-grid padded-top">
          {likedAlbums.map((album) => (
            <AlbumCard key={album.id} album={album} onOpen={onAlbumOpen} />
          ))}
        </div>
      ) : null}

      {selectedType === "artists" ? (
        <div className="artist-list padded-top">
          {likedArtists.map((artist) => (
            <button className="artist-row" key={artist.id} type="button" onClick={() => onArtistOpen(artist)}>
              <img src={artist.cover} alt={`${artist.name} 图片`} />
              <span>
                <strong>{artist.name}</strong>
                <small>{artist.tag} · {formatSongCount(artist.count)}</small>
              </span>
              <CaretRight size={20} />
            </button>
          ))}
        </div>
      ) : null}
    </>
  );
}

function MeView({ onSettings, onLogin, onAlbumOpen, onArtistOpen }) {
  return (
    <>
      <AppHeader title="我的" subtitle="本地资料与同步状态" onSettings={onSettings} />

      <section className="profile-card">
        <div className="avatar-cluster">
          <img src={albumTimeForest} alt="账号头像视觉" />
          <span />
        </div>
        <div>
          <h2>登录音乐账号</h2>
          <p>使用 Supabase 同步收藏、播放记录和多端资料。</p>
        </div>
        <button className="primary-pill wide" type="button" onClick={onLogin}>
          <SignIn size={21} weight="bold" />
          立即登录
        </button>
      </section>

      <div className="metric-grid">
        <Metric icon={VinylRecord} label="本地专辑" value="86" />
        <Metric icon={MicrophoneStage} label="歌手" value="128" />
        <Metric icon={Heart} label="收藏" value="42" />
      </div>

      <section className="plain-panel">
        <SectionTitle title="我的收藏" actionLabel="查看" actionAriaLabel="查看我的收藏专辑" onAction={() => onAlbumOpen(albums[0])} />
        <div className="mini-albums">
          {albums.map((album) => (
            <button key={album.id} type="button" onClick={() => onAlbumOpen(album)}>
              <img src={album.cover} alt={`${album.title} 封面`} />
              <span>{album.title}</span>
            </button>
          ))}
        </div>
      </section>

      <section className="plain-panel">
        <SectionTitle title="常听歌手" actionLabel="更多" actionAriaLabel="查看更多常听歌手" onAction={() => onArtistOpen(artists[0])} />
        <div className="artist-chips">
          {artists.map((artist) => (
            <button key={artist.id} type="button" onClick={() => onArtistOpen(artist)}>
              <img src={artist.cover} alt={`${artist.name} 图片`} />
              <span>{artist.name}</span>
            </button>
          ))}
        </div>
      </section>
    </>
  );
}

function SearchView({ query, onQuery, searchScope, onSearchScope, songs, currentSongId, onBack, onSongOpen, onSongPlay, onMore, onAlbumOpen, onArtistOpen }) {
  const normalized = query.trim().toLowerCase();
  const filteredSongs = songs.filter((song) => {
    if (!normalized) return true;
    return `${song.title} ${song.artist} ${song.album}`.toLowerCase().includes(normalized);
  });
  const filteredAlbums = albums.filter((album) => {
    if (!normalized) return true;
    return `${album.title} ${album.artist}`.toLowerCase().includes(normalized);
  });
  const filteredArtists = artists.filter((artist) => {
    if (!normalized) return true;
    return `${artist.name} ${artist.tag}`.toLowerCase().includes(normalized);
  });
  const showSongs = searchScope === "all" || searchScope === "songs";
  const showAlbums = searchScope === "all" || searchScope === "albums";
  const showArtists = searchScope === "all" || searchScope === "artists";
  const resultTotal = filteredSongs.length + filteredAlbums.length + filteredArtists.length;
  const visibleSongs = searchScope === "all" ? filteredSongs.slice(0, 8) : filteredSongs;

  return (
    <>
      <AppHeader title="搜索" subtitle="在本地音乐库中查找" onBack={onBack} compact />
      <label className="search-box">
        <MagnifyingGlass size={22} />
        <input
          value={query}
          onChange={(event) => onQuery(event.target.value)}
          placeholder="歌曲、歌手或专辑"
          aria-label="搜索本地音乐"
          autoFocus
        />
        <SlidersHorizontal size={22} />
      </label>

      <div className="quick-tags">
        {["旅行团乐队", "人声", "钢琴", "最近播放"].map((tag) => (
          <button
            key={tag}
            className={query === tag ? "active" : ""}
            type="button"
            onClick={() => onQuery(tag)}
            aria-pressed={query === tag}
          >
            {tag}
          </button>
        ))}
      </div>

      <div className="search-summary" aria-live="polite">
        找到 {resultTotal} 个结果
      </div>

      <Segmented
        value={searchScope}
        onChange={onSearchScope}
        options={[
          { key: "all", label: "全部" },
          { key: "songs", label: "歌曲" },
          { key: "albums", label: "专辑" },
          { key: "artists", label: "歌手" },
        ]}
      />

      {showSongs ? (
        <section className="search-section">
          <SectionTitle title="歌曲" meta={`${filteredSongs.length} 首`} />
          {filteredSongs.length ? (
            <div className="stack-list">
              {visibleSongs.map((song) => (
                <SongRow
                  key={song.id}
                  song={song}
                  currentSongId={currentSongId}
                  active={false}
                  onOpen={onSongOpen}
                  onPlay={onSongPlay}
                  onMore={onMore}
                  dense
                />
              ))}
              {searchScope === "all" && filteredSongs.length > visibleSongs.length ? (
                <button className="inline-more" type="button" onClick={() => onSearchScope("songs")}>
                  查看全部 {filteredSongs.length} 首歌曲
                  <CaretRight size={18} weight="bold" />
                </button>
              ) : null}
            </div>
          ) : (
            <EmptyState title="没有找到歌曲" detail="试试搜索歌手、专辑名，或换一个关键词。" />
          )}
        </section>
      ) : null}

      {showAlbums ? (
        <section className="search-section">
          <SectionTitle title="专辑" meta={`${filteredAlbums.length} 张`} />
          {filteredAlbums.length ? (
            <div className="search-grid">
              {filteredAlbums.map((album) => (
                <button key={album.id} type="button" onClick={() => onAlbumOpen(album)} aria-label={`打开专辑 ${album.title}`}>
                  <img src={album.cover} alt={`${album.title} 专辑封面`} />
                  <strong>{album.title}</strong>
                  <span>{album.artist}</span>
                </button>
              ))}
            </div>
          ) : (
            <EmptyState title="没有找到专辑" detail="本地专辑会在扫描后自动出现在这里。" />
          )}
        </section>
      ) : null}

      {showArtists ? (
        <section className="search-section">
          <SectionTitle title="歌手" meta={`${filteredArtists.length} 位`} />
          {filteredArtists.length ? (
            <div className="search-grid">
              {filteredArtists.map((artist) => (
                <button key={artist.id} type="button" onClick={() => onArtistOpen(artist)} aria-label={`打开歌手 ${artist.name}`}>
                  <img src={artist.cover} alt={`${artist.name} 图片`} />
                  <strong>{artist.name}</strong>
                  <span>{artist.tag}</span>
                </button>
              ))}
            </div>
          ) : (
            <EmptyState title="没有找到歌手" detail="换成歌手名、流派或专辑名再试一次。" />
          )}
        </section>
      ) : null}
    </>
  );
}

function PlayerView({ song, isPlaying, onToggle, onBack, onNext, onPrev, onLike, onQueue }) {
  return (
    <>
      <AppHeader title="正在播放" onBack={onBack} compact />
      <section className="player-view">
        <img className="player-cover" src={song.cover} alt={`${song.title} 封面`} />
        <div className="player-copy">
          <div>
            <h2>{song.title}</h2>
            <p>{song.artist} · {song.album}</p>
          </div>
          <button
            className={`tiny-icon like ${song.liked ? "is-liked" : ""}`}
            type="button"
            onClick={() => onLike(song.id)}
            aria-label="收藏当前歌曲"
          >
            <Heart size={24} weight={song.liked ? "fill" : "regular"} />
          </button>
        </div>

        <div className="progress-area">
          <input type="range" min="0" max="225" defaultValue="86" aria-label="播放进度" />
          <div>
            <span>1:26</span>
            <span>{song.duration}</span>
          </div>
        </div>

        <div className="player-controls">
          <button type="button" aria-label="随机播放">
            <Shuffle size={24} />
          </button>
          <button type="button" onClick={onPrev} aria-label="上一首">
            <SkipBack size={31} weight="fill" />
          </button>
          <button className="main-play" type="button" onClick={onToggle} aria-label={isPlaying ? "暂停" : "播放"}>
            {isPlaying ? <Pause size={33} weight="fill" /> : <Play size={33} weight="fill" />}
          </button>
          <button type="button" onClick={onNext} aria-label="下一首">
            <SkipForward size={31} weight="fill" />
          </button>
          <button type="button" aria-label="循环播放">
            <Repeat size={24} />
          </button>
        </div>

        <section className="lyric-card">
          <span>{song.quality}</span>
          <p>{song.lyric}</p>
        </section>

        <button className="queue-button" type="button" onClick={onQueue}>
          <ListBullets size={24} />
          播放队列
          <CaretRight size={19} />
        </button>
      </section>
    </>
  );
}

function AlbumDetail({ album, songs, currentSongId, onBack, onSongOpen, onSongPlay, onMore, onLike }) {
  const albumSongs = songs.filter((song) => song.album === album.title);

  return (
    <>
      <AppHeader title="专辑" onBack={onBack} compact />
      <section className="detail-hero">
        <img src={album.cover} alt={`${album.title} 专辑封面`} />
        <div>
          <span>{album.year} · {album.mood}</span>
          <h2>{album.title}</h2>
          <p>{album.artist} · {formatSongCount(albumSongs.length || album.count)}</p>
        </div>
      </section>
      <div className="detail-actions">
        <button className="primary-pill" type="button" onClick={() => onSongPlay(albumSongs[0] || songs[0])}>
          <Play size={20} weight="fill" />
          播放全部
        </button>
        <button className="soft-pill text" type="button">
          <DownloadSimple size={20} />
          离线
        </button>
      </div>
      <SectionTitle title="曲目" meta={`${albumSongs.length || album.count} 首`} />
      <div className="stack-list">
        {(albumSongs.length ? albumSongs : songs.slice(0, 3)).map((song) => (
          <SongRow
            key={song.id}
            song={song}
            currentSongId={currentSongId}
            active={false}
            onOpen={onSongOpen}
            onPlay={onSongPlay}
            onMore={onMore}
            onLike={onLike}
            dense
          />
        ))}
      </div>
    </>
  );
}

function ArtistDetail({ artist, songs, currentSongId, onBack, onSongOpen, onSongPlay, onMore, onLike, onAlbumOpen }) {
  const artistSongs = songs.filter((song) => song.artist === artist.name);

  return (
    <>
      <AppHeader title="歌手" onBack={onBack} compact />
      <section className="detail-hero artist">
        <img src={artist.cover} alt={`${artist.name} 图片`} />
        <div>
          <span>{artist.tag}</span>
          <h2>{artist.name}</h2>
          <p>{formatSongCount(artist.count)} · 本地收藏</p>
        </div>
      </section>
      <SectionTitle title="热门歌曲" />
      <div className="stack-list">
        {(artistSongs.length ? artistSongs : songs.slice(0, 3)).map((song) => (
          <SongRow
            key={song.id}
            song={song}
            currentSongId={currentSongId}
            active={false}
            onOpen={onSongOpen}
            onPlay={onSongPlay}
            onMore={onMore}
            onLike={onLike}
            dense
          />
        ))}
      </div>
      <SectionTitle title="相关专辑" />
      <div className="album-strip compact-strip">
        {albums
          .filter((album) => album.artist === artist.name || artist.name === "旅行团乐队")
          .slice(0, 3)
          .map((album) => (
            <AlbumCard key={album.id} album={album} onOpen={onAlbumOpen} />
          ))}
      </div>
    </>
  );
}

function SettingsView({ theme, onTheme, onBack, onScan, onClearCache }) {
  return (
    <>
      <AppHeader title="设置" subtitle="播放、扫描与外观" onBack={onBack} compact />

      <section className="settings-group">
        <h2>外观</h2>
        <div className="theme-switch">
          {[
            { key: "light", label: "浅色", icon: Sun },
            { key: "dark", label: "深色", icon: Moon },
            { key: "system", label: "跟随系统", icon: Desktop },
          ].map((item) => {
            const Icon = item.icon;
            return (
              <button
                className={theme === item.key ? "active" : ""}
                key={item.key}
                type="button"
                onClick={() => onTheme(item.key)}
              >
                <Icon size={20} />
                {item.label}
              </button>
            );
          })}
        </div>
      </section>

      <section className="settings-group">
        <h2>音乐库</h2>
        <SettingsRow icon={Scan} title="重新扫描本地音乐" detail="上次扫描：今天 08:36" onClick={onScan} />
        <SettingsRow icon={FolderSimple} title="本地文件夹" detail="/Music/KMP Library" />
        <SettingsRow icon={Trash} title="清理缓存" detail="可释放 428 MB" onClick={onClearCache} danger />
      </section>

      <section className="settings-group">
        <h2>播放</h2>
        <SettingsRow icon={SpeakerHigh} title="无损优先" detail="可用时优先播放 FLAC / ALAC" toggle />
        <SettingsRow icon={Timer} title="睡眠定时" detail="30 分钟后停止播放" />
        <SettingsRow icon={Devices} title="设备同步" detail="手机、桌面端同步播放记录" />
      </section>

      <section className="settings-group">
        <h2>账号与安全</h2>
        <SettingsRow icon={ShieldCheck} title="隐私保护" detail="本地音乐不会上传到云端" />
        <SettingsRow icon={ArrowsClockwise} title="同步状态" detail="收藏与记录已同步" />
      </section>
    </>
  );
}

function LoginView({ email, sent, onEmail, onSend, onBack }) {
  return (
    <>
      <AppHeader title="登录" subtitle="使用邮箱接收魔法链接" onBack={onBack} compact />
      <section className="login-panel">
        <div className="login-mark">
          <LockSimple size={34} />
        </div>
        <h2>同步收藏和播放记录</h2>
        <p>登录后可在 Android、iOS 和桌面端继续使用同一套资料。</p>
        <label className="email-field">
          <EnvelopeSimple size={22} />
          <input
            type="email"
            value={email}
            onChange={(event) => onEmail(event.target.value)}
            placeholder="name@example.com"
          />
        </label>
        <button className="primary-pill wide" type="button" onClick={onSend}>
          <SignIn size={20} weight="bold" />
          发送登录邮件
        </button>
        {sent ? (
          <div className="success-note">
            <CheckCircle size={20} weight="fill" />
            登录邮件已发送，请在邮箱中确认。
          </div>
        ) : null}
      </section>
    </>
  );
}

function LocalFolderView({ songs, currentSongId, onBack, onSongOpen, onSongPlay, onMore }) {
  return (
    <>
      <AppHeader title="本地文件夹" subtitle="Music / KMP Library" onBack={onBack} compact />
      <div className="folder-list">
        {["似水流年", "Dream Stories", "华语人声", "森林歌单"].map((folder, index) => (
          <button key={folder} type="button">
            <FolderSimple size={24} />
            <span>
              <strong>{folder}</strong>
              <small>{24 - index * 3} 首歌曲 · 已加入音乐库</small>
            </span>
            <CaretRight size={19} />
          </button>
        ))}
      </div>
      <SectionTitle title="最近导入" />
      <div className="stack-list">
        {songs.slice(0, 4).map((song) => (
          <SongRow
            key={song.id}
            song={song}
            currentSongId={currentSongId}
            onOpen={onSongOpen}
            onPlay={onSongPlay}
            onMore={onMore}
            dense
          />
        ))}
      </div>
    </>
  );
}

function Metric({ icon: Icon, label, value }) {
  return (
    <div className="metric">
      <Icon size={24} />
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  );
}

function EmptyState({ title, detail }) {
  return (
    <div className="empty-state" role="status">
      <strong>{title}</strong>
      <span>{detail}</span>
    </div>
  );
}

function SettingsRow({ icon: Icon, title, detail, onClick, toggle, danger }) {
  const [enabled, setEnabled] = useState(true);
  const content = (
    <>
      <span className={`settings-icon ${danger ? "danger" : ""}`}>
        <Icon size={22} />
      </span>
      <span className="settings-copy">
        <strong>{title}</strong>
        <small>{detail}</small>
      </span>
      {toggle ? (
        <span className={`switch ${enabled ? "on" : ""}`} aria-hidden="true">
          <i />
        </span>
      ) : (
        <CaretRight size={19} />
      )}
    </>
  );

  if (toggle) {
    return (
      <button className="settings-row" type="button" onClick={() => setEnabled(!enabled)}>
        {content}
      </button>
    );
  }

  return (
    <button className="settings-row" type="button" onClick={onClick}>
      {content}
    </button>
  );
}

function Segmented({ value, onChange, options }) {
  return (
    <div className="segmented" style={{ "--segment-count": options.length }}>
      {options.map((option) => (
        <button
          key={option.key}
          className={value === option.key ? "active" : ""}
          type="button"
          onClick={() => onChange(option.key)}
          aria-pressed={value === option.key}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}

function MiniPlayer({ song, isPlaying, isTopLevelView, onToggle, onOpen, onPrev, onNext, onQueue }) {
  return (
    <div className={`mini-player ${isTopLevelView ? "top-level" : "secondary-level"}`}>
      <button className="mini-info" type="button" onClick={onOpen}>
        <img src={song.cover} alt={`${song.title} 封面`} />
        <span>
          <strong>{song.title}</strong>
          <small>{song.artist}</small>
        </span>
      </button>
      <div className="mini-controls">
        <button type="button" onClick={onPrev} aria-label="上一首">
          <SkipBack size={24} weight="fill" />
        </button>
        <button type="button" onClick={onToggle} aria-label={isPlaying ? "暂停" : "播放"}>
          {isPlaying ? <Pause size={28} weight="fill" /> : <Play size={28} weight="fill" />}
        </button>
        <button type="button" onClick={onQueue} aria-label="播放队列">
          <ListBullets size={27} />
        </button>
      </div>
      <div className="mini-progress" aria-hidden="true" />
    </div>
  );
}

function BottomNav({ active, visible, onNavigate }) {
  return (
    <nav
      className={`bottom-nav ${visible ? "is-visible" : "is-hidden"}`}
      aria-label={visible ? "主导航" : undefined}
      aria-hidden={!visible}
    >
      {navItems.map((item) => {
        const Icon = item.icon;
        const isActive = active === item.key;
        return (
          <button
            key={item.key}
            className={isActive ? "active" : ""}
            type="button"
            tabIndex={visible ? 0 : -1}
            onClick={() => {
              if (visible) onNavigate(item.key);
            }}
          >
            <Icon size={28} weight={isActive ? "bold" : "regular"} />
            <span>{item.label}</span>
          </button>
        );
      })}
    </nav>
  );
}

const modalFocusableSelector = [
  "button:not([disabled])",
  "input:not([disabled])",
  "select:not([disabled])",
  "textarea:not([disabled])",
  "a[href]",
  "[tabindex]:not([tabindex='-1'])",
].join(",");

function getModalFocusableElements(root) {
  if (!root) return [];

  return Array.from(root.querySelectorAll(modalFocusableSelector)).filter((element) => {
    const style = window.getComputedStyle(element);
    return style.display !== "none" && style.visibility !== "hidden";
  });
}

function ModalShell({ open, onClose, className, labelledBy, describedBy, children }) {
  const dialogRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;

    const previousActiveElement = document.activeElement;
    const focusFrame = window.requestAnimationFrame(() => {
      const focusableElements = getModalFocusableElements(dialogRef.current);
      const target = focusableElements[0] || dialogRef.current;
      target?.focus({ preventScroll: true });
    });

    function handleKeyDown(event) {
      if (event.key === "Escape") {
        event.preventDefault();
        onClose();
        return;
      }

      if (event.key !== "Tab") return;

      const focusableElements = getModalFocusableElements(dialogRef.current);

      if (!focusableElements.length) {
        event.preventDefault();
        dialogRef.current?.focus({ preventScroll: true });
        return;
      }

      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];

      if (event.shiftKey && document.activeElement === firstElement) {
        event.preventDefault();
        lastElement.focus({ preventScroll: true });
      } else if (!event.shiftKey && document.activeElement === lastElement) {
        event.preventDefault();
        firstElement.focus({ preventScroll: true });
      }
    }

    document.addEventListener("keydown", handleKeyDown);

    return () => {
      window.cancelAnimationFrame(focusFrame);
      document.removeEventListener("keydown", handleKeyDown);

      if (previousActiveElement instanceof HTMLElement) {
        previousActiveElement.focus({ preventScroll: true });
      }
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="sheet-backdrop" role="presentation" onClick={onClose}>
      <section
        ref={dialogRef}
        className={className}
        role="dialog"
        aria-modal="true"
        aria-labelledby={labelledBy}
        aria-describedby={describedBy}
        tabIndex={-1}
        onClick={(event) => event.stopPropagation()}
      >
        {children}
      </section>
    </div>
  );
}

function QueueDrawer({ open, songs, currentSong, onClose, onSongPlay, onRemove }) {
  return (
    <ModalShell open={open} onClose={onClose} className="queue-sheet" labelledBy="queue-title">
        <div className="sheet-handle" />
        <div className="sheet-title">
          <h2 id="queue-title">播放队列</h2>
          <button type="button" onClick={onClose} aria-label="关闭播放队列">
            <X size={22} />
          </button>
        </div>
        <div className="stack-list sheet-list">
          {songs.map((song) => (
            <article className={`queue-row ${currentSong.id === song.id ? "active" : ""}`} key={song.id} aria-current={currentSong.id === song.id ? "true" : undefined}>
              <button type="button" onClick={() => onSongPlay(song)} aria-label={`播放 ${song.title}`}>
                <img src={song.cover} alt={`${song.title} 封面`} />
                <span>
                  <strong>{song.title}</strong>
                  <small>{song.artist} · {song.duration}</small>
                </span>
              </button>
              <button type="button" onClick={() => onRemove(song.id)} aria-label={`从播放队列移除 ${song.title}`}>
                <Trash size={19} />
              </button>
            </article>
          ))}
        </div>
    </ModalShell>
  );
}

function ScanModal({ open, done, onDone, onClose }) {
  return (
    <ModalShell open={open} onClose={onClose} className="scan-modal" labelledBy="scan-title" describedBy="scan-detail">
        <button className="close-modal" type="button" onClick={onClose} aria-label="关闭">
          <X size={20} />
        </button>
        <div className="scan-orb">
          {done ? <CheckCircle size={42} weight="fill" /> : <Scan size={42} weight="bold" />}
        </div>
        <h2 id="scan-title">{done ? "扫描完成" : "正在扫描本地音乐"}</h2>
        <p id="scan-detail">{done ? "新增 24 首歌曲，已更新 3 张专辑。" : "正在读取 /Music/KMP Library，已识别 1,248 首歌曲。"}</p>
        <div className="scan-progress">
          <span style={{ width: done ? "100%" : "72%" }} />
        </div>
        <button className="primary-pill wide" type="button" onClick={onDone}>
          {done ? <CheckCircle size={20} weight="fill" /> : <Scan size={20} weight="bold" />}
          {done ? "回到音乐库" : "完成扫描"}
        </button>
    </ModalShell>
  );
}

function ActionToast({ toast, onClose }) {
  if (!toast) return null;

  return (
    <button className="toast" type="button" onClick={onClose}>
      <CheckCircle size={18} weight="fill" />
      {toast}
    </button>
  );
}

function MoreSheet({ song, onClose, onLike, onAlbum, onArtist }) {
  return (
    <ModalShell open={Boolean(song)} onClose={onClose} className="more-sheet" labelledBy="more-title">
        <div className="sheet-handle" />
        <div className="more-song">
          <img src={song?.cover} alt={`${song?.title} 封面`} />
          <span>
            <strong id="more-title">{song?.title}</strong>
            <small>{song?.artist}</small>
          </span>
        </div>
        <button type="button" onClick={() => onLike(song.id)}>
          <Heart size={22} weight={song?.liked ? "fill" : "regular"} />
          {song?.liked ? "取消收藏" : "加入收藏"}
        </button>
        <button type="button" onClick={() => onAlbum(song)}>
          <VinylRecord size={22} />
          查看专辑
        </button>
        <button type="button" onClick={() => onArtist(song)}>
          <MicrophoneStage size={22} />
          查看歌手
        </button>
    </ModalShell>
  );
}

function ClearCacheDialog({ open, onCancel, onConfirm }) {
  return (
    <ModalShell open={open} onClose={onCancel} className="confirm-card" labelledBy="clear-cache-title" describedBy="clear-cache-detail">
        <WarningCircle size={42} />
        <h2 id="clear-cache-title">清理 428 MB 缓存？</h2>
        <p id="clear-cache-detail">只会删除封面缓存和临时文件，本地歌曲不会受到影响。</p>
        <div>
          <button type="button" onClick={onCancel}>取消</button>
          <button type="button" onClick={onConfirm}>清理</button>
        </div>
    </ModalShell>
  );
}

export function App() {
  const [view, setView] = useState("home");
  const [previousView, setPreviousView] = useState("home");
  const [songs, setSongs] = useState(initialSongs);
  const [queueIds, setQueueIds] = useState(initialQueueIds);
  const [currentSongId, setCurrentSongId] = useState("sea-dream");
  const [isPlaying, setIsPlaying] = useState(true);
  const [favoritesType, setFavoritesType] = useState("songs");
  const [selectedAlbum, setSelectedAlbum] = useState(albums[0]);
  const [selectedArtist, setSelectedArtist] = useState(artists[0]);
  const [query, setQuery] = useState("");
  const [searchScope, setSearchScope] = useState("all");
  const [queueOpen, setQueueOpen] = useState(false);
  const [scanOpen, setScanOpen] = useState(false);
  const [scanDone, setScanDone] = useState(false);
  const [moreSong, setMoreSong] = useState(null);
  const [theme, setTheme] = useState("light");
  const [toast, setToast] = useState("");
  const [clearCacheOpen, setClearCacheOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [mailSent, setMailSent] = useState(false);

  const currentSong = useMemo(
    () => songs.find((song) => song.id === currentSongId) || songs[0],
    [currentSongId, songs],
  );
  const queueSongs = useMemo(
    () => queueIds.map((songId) => songs.find((song) => song.id === songId)).filter(Boolean),
    [queueIds, songs],
  );

  const isTopLevelView = topLevelViews.includes(view);
  const activeRoot = isTopLevelView ? view : previousView;
  const hasOpenOverlay = queueOpen || scanOpen || Boolean(moreSong) || clearCacheOpen;
  const phoneAppRef = useRef(null);
  const appSurfaceRef = useRef(null);

  useEffect(() => {
    if (!appSurfaceRef.current) return;

    if (hasOpenOverlay) {
      appSurfaceRef.current.setAttribute("inert", "");
    } else {
      appSurfaceRef.current.removeAttribute("inert");
    }
  }, [hasOpenOverlay]);

  useEffect(() => {
    if (phoneAppRef.current) {
      phoneAppRef.current.scrollTop = 0;
    }
  }, [view, hasOpenOverlay]);

  function navigate(nextView) {
    setPreviousView(topLevelViews.includes(view) ? view : previousView);
    setView(nextView);
    setQueueOpen(false);
    setMoreSong(null);
  }

  function navigateRoot(nextView) {
    setPreviousView(nextView);
    setView(nextView);
    setQueueOpen(false);
    setMoreSong(null);
  }

  function back() {
    setView(previousView || "home");
  }

  function playSong(song) {
    setCurrentSongId(song.id);
    setIsPlaying(true);
    setQueueIds((ids) => (ids.includes(song.id) ? ids : [song.id, ...ids]));
    showToast(`正在播放：${song.title}`);
  }

  function openSong(song) {
    setCurrentSongId(song.id);
    setIsPlaying(true);
    setQueueIds((ids) => (ids.includes(song.id) ? ids : [song.id, ...ids]));
    navigate("player");
  }

  function toggleLike(songId) {
    setSongs((items) =>
      items.map((song) => (song.id === songId ? { ...song, liked: !song.liked } : song)),
    );
    showToast("收藏状态已更新");
  }

  function moveTrack(direction) {
    const playbackSource = queueSongs.length ? queueSongs : songs;
    const index = playbackSource.findIndex((song) => song.id === currentSongId);
    const safeIndex = index >= 0 ? index : 0;
    const nextIndex = (safeIndex + direction + playbackSource.length) % playbackSource.length;
    setCurrentSongId(playbackSource[nextIndex].id);
    setIsPlaying(true);
  }

  function showToast(message) {
    setToast(message);
    window.clearTimeout(window.__kmpToast);
    window.__kmpToast = window.setTimeout(() => setToast(""), 1800);
  }

  function openAlbum(album) {
    setSelectedAlbum(album);
    navigate("album");
  }

  function openArtist(artist) {
    setSelectedArtist(artist);
    navigate("artist");
  }

  function openAlbumFromSong(song) {
    const album = albums.find((item) => item.title === song.album) || albums[0];
    setMoreSong(null);
    openAlbum(album);
  }

  function openArtistFromSong(song) {
    const artist = artists.find((item) => item.name === song.artist) || artists[0];
    setMoreSong(null);
    openArtist(artist);
  }

  function removeFromQueue(songId) {
    if (queueIds.length <= 1) {
      showToast("队列至少保留一首歌曲");
      return;
    }

    const nextQueueIds = queueIds.filter((id) => id !== songId);
    setQueueIds(nextQueueIds);

    if (songId === currentSongId) {
      setCurrentSongId(nextQueueIds[0]);
      setIsPlaying(true);
    }

    showToast("已从队列移除");
  }

  function openScan() {
    setScanDone(false);
    setScanOpen(true);
  }

  function finishScan() {
    if (!scanDone) {
      setScanDone(true);
      return;
    }
    setScanOpen(false);
    showToast("音乐库已更新");
  }

  function sendLoginMail() {
    if (!email.includes("@")) {
      showToast("请输入有效邮箱");
      return;
    }
    setMailSent(true);
  }

  function clearCache() {
    setClearCacheOpen(false);
    showToast("缓存已清理");
  }

  const viewProps = {
    songs,
    currentSong,
    currentSongId,
    onSearch: () => navigate("search"),
    onScan: openScan,
    onLocalFolder: () => navigate("local"),
    onSongOpen: openSong,
    onSongPlay: playSong,
    onMore: setMoreSong,
    onLike: toggleLike,
    onAlbumOpen: openAlbum,
    onArtistOpen: openArtist,
  };

  return (
    <main className={`app-stage theme-${theme}`}>
      <section
        ref={phoneAppRef}
        className={`phone-app ${isTopLevelView ? "top-level-view" : "secondary-view"}`}
        aria-label="KMP Music 高保真原型"
      >
        <div ref={appSurfaceRef} className="app-surface" aria-hidden={hasOpenOverlay ? "true" : undefined}>
          <div className="view-content">
        {view === "home" ? <HomeView {...viewProps} /> : null}
        {view === "favorites" ? (
          <FavoritesView
            {...viewProps}
            selectedType={favoritesType}
            onType={setFavoritesType}
          />
        ) : null}
        {view === "me" ? (
          <MeView
            onSettings={() => navigate("settings")}
            onLogin={() => navigate("login")}
            onAlbumOpen={openAlbum}
            onArtistOpen={openArtist}
          />
        ) : null}
        {view === "search" ? (
          <SearchView
            {...viewProps}
            query={query}
            onQuery={setQuery}
            searchScope={searchScope}
            onSearchScope={setSearchScope}
            onBack={back}
          />
        ) : null}
        {view === "player" ? (
          <PlayerView
            song={currentSong}
            isPlaying={isPlaying}
            onToggle={() => setIsPlaying(!isPlaying)}
            onBack={back}
            onNext={() => moveTrack(1)}
            onPrev={() => moveTrack(-1)}
            onLike={toggleLike}
            onQueue={() => setQueueOpen(true)}
          />
        ) : null}
        {view === "album" ? (
          <AlbumDetail
            album={selectedAlbum}
            songs={songs}
            currentSongId={currentSongId}
            onBack={back}
            onSongOpen={openSong}
            onSongPlay={playSong}
            onMore={setMoreSong}
            onLike={toggleLike}
          />
        ) : null}
        {view === "artist" ? (
          <ArtistDetail
            artist={selectedArtist}
            songs={songs}
            currentSongId={currentSongId}
            onBack={back}
            onSongOpen={openSong}
            onSongPlay={playSong}
            onMore={setMoreSong}
            onLike={toggleLike}
            onAlbumOpen={openAlbum}
          />
        ) : null}
        {view === "settings" ? (
          <SettingsView
            theme={theme}
            onTheme={setTheme}
            onBack={back}
            onScan={openScan}
            onClearCache={() => setClearCacheOpen(true)}
          />
        ) : null}
        {view === "login" ? (
          <LoginView
            email={email}
            sent={mailSent}
            onEmail={setEmail}
            onSend={sendLoginMail}
            onBack={back}
          />
        ) : null}
        {view === "local" ? (
          <LocalFolderView
            songs={songs}
            currentSongId={currentSongId}
            onBack={back}
            onSongOpen={openSong}
            onSongPlay={playSong}
            onMore={setMoreSong}
          />
        ) : null}
          </div>

        <MiniPlayer
          song={currentSong}
          isPlaying={isPlaying}
          isTopLevelView={isTopLevelView}
          onToggle={() => setIsPlaying(!isPlaying)}
          onOpen={() => navigate("player")}
          onPrev={() => moveTrack(-1)}
          onNext={() => moveTrack(1)}
          onQueue={() => setQueueOpen(true)}
        />
        <BottomNav active={activeRoot} visible={isTopLevelView} onNavigate={navigateRoot} />
        </div>
        <QueueDrawer
          open={queueOpen}
          songs={queueSongs}
          currentSong={currentSong}
          onClose={() => setQueueOpen(false)}
          onSongPlay={playSong}
          onRemove={removeFromQueue}
        />
        <ScanModal open={scanOpen} done={scanDone} onDone={finishScan} onClose={() => setScanOpen(false)} />
        <MoreSheet
          song={moreSong}
          onClose={() => setMoreSong(null)}
          onLike={toggleLike}
          onAlbum={openAlbumFromSong}
          onArtist={openArtistFromSong}
        />
        <ClearCacheDialog open={clearCacheOpen} onCancel={() => setClearCacheOpen(false)} onConfirm={clearCache} />
        <ActionToast toast={toast} onClose={() => setToast("")} />
      </section>
    </main>
  );
}
