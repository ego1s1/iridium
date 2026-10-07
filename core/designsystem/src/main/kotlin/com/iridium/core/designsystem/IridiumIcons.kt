package com.iridium.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewCompact
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.Warning

/**
 * Every icon the app uses, in one place. Never reference `Icons.*` directly
 * in a screen: swapping an icon should be a one-line change here.
 *
 * MenuBook stays AutoMirrored (Iridium convention for reading direction);
 * SkipNext/SkipPrevious stay non-mirrored deliberately: callers swap which
 * icon they show per direction, so automirroring would double-flip.
 */
object IridiumIcons {
    val Add = Icons.Rounded.Add
    val Animation = Icons.Rounded.Animation
    val Back = Icons.AutoMirrored.Rounded.ArrowBack
    val Bookmark = Icons.Rounded.Bookmark
    val BookmarkBorder = Icons.Rounded.BookmarkBorder
    val BrokenImage = Icons.Rounded.BrokenImage
    val BugReport = Icons.Rounded.BugReport
    val Check = Icons.Rounded.Check
    val Close = Icons.Rounded.Close
    val Code = Icons.Rounded.Code
    val Contrast = Icons.Rounded.Contrast
    val Crop = Icons.Rounded.Crop
    val DarkMode = Icons.Rounded.DarkMode
    val Delete = Icons.Rounded.Delete
    val Edit = Icons.Rounded.Edit
    val ExpandMore = Icons.Rounded.ExpandMore
    val FitScreen = Icons.Rounded.FitScreen
    val Folder = Icons.Rounded.Folder
    val FormatPaint = Icons.Rounded.FormatPaint
    val Forward = Icons.AutoMirrored.Rounded.ArrowForward
    val GridView = Icons.Rounded.GridView
    val Highlight = Icons.Rounded.Highlight
    val History = Icons.Rounded.History
    val ImportFile = Icons.Rounded.UploadFile
    val ImportFolder = Icons.Rounded.CreateNewFolder
    val Info = Icons.Rounded.Info
    val LightMode = Icons.Rounded.LightMode
    val List = Icons.Rounded.List
    val MenuBook = Icons.AutoMirrored.Rounded.MenuBook
    val More = Icons.Rounded.MoreVert
    val Motion = Icons.Rounded.MotionPhotosOn
    val NewReleases = Icons.Rounded.NewReleases
    val Next = Icons.AutoMirrored.Rounded.NavigateNext
    val Palette = Icons.Rounded.Palette
    val Play = Icons.Rounded.PlayArrow
    val Previous = Icons.AutoMirrored.Rounded.NavigateBefore
    val Refresh = Icons.Rounded.Refresh
    val Remove = Icons.Rounded.Remove
    val ScreenRotation = Icons.Rounded.ScreenRotation
    val Search = Icons.Rounded.Search
    val Settings = Icons.Rounded.Settings
    val Share = Icons.Rounded.Share
    val SkipNext = Icons.Rounded.SkipNext
    val SkipPrevious = Icons.Rounded.SkipPrevious
    val Spa = Icons.Rounded.Spa
    val Storage = Icons.Rounded.Storage
    val SwapHoriz = Icons.Rounded.SwapHoriz
    val SwapVert = Icons.Rounded.SwapVert
    val TouchApp = Icons.Rounded.TouchApp
    val TrendingUp = Icons.Rounded.TrendingUp
    val Tune = Icons.Rounded.Tune
    val Vibration = Icons.Rounded.Vibration
    val ViewAgenda = Icons.Rounded.ViewAgenda
    val ViewCompact = Icons.Rounded.ViewCompact
    val ViewList = Icons.Rounded.ViewList
    val ViewModule = Icons.Rounded.ViewModule
    val Warning = Icons.Rounded.Warning
    val Fire = Icons.Rounded.LocalFireDepartment
    val Trophy = Icons.Rounded.EmojiEvents
    val Sparkle = Icons.Rounded.AutoAwesome

    // Settings hub categories.
    val Shelves = Icons.Rounded.CollectionsBookmark
    val PrivacyLock = Icons.Rounded.Lock

    // Navigation pairs: outlined for idle tabs, filled for selected,
    // crossfaded on selection like the reference bottom toolbar.
    val MenuBookOutlined = Icons.Outlined.MenuBook
    val BarChartOutlined = Icons.Outlined.BarChart
    val SettingsOutlined = Icons.Outlined.Settings
    val BarChart = Icons.Rounded.BarChart
}
