package com.iridium.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
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
    val Add: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Add }
    val Animation: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Animation }
    val Back: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.AutoMirrored.Rounded.ArrowBack }
    val Bookmark: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Bookmark }
    val BookmarkBorder: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.BookmarkBorder }
    val BrokenImage: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.BrokenImage }
    val BugReport: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.BugReport }
    val Check: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Check }
    val Close: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Close }
    val Code: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Code }
    val Contrast: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Contrast }
    val Crop: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Crop }
    val DarkMode: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.DarkMode }
    val Delete: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Delete }
    val Edit: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Edit }
    val ExpandMore: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.ExpandMore }
    val FitScreen: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.FitScreen }
    val Folder: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Folder }
    val FormatPaint: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.FormatPaint }
    val Forward: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.AutoMirrored.Rounded.ArrowForward }
    val GridView: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.GridView }
    val Highlight: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Highlight }
    val History: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.History }
    val ImportFile: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.UploadFile }
    val ImportFolder: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.CreateNewFolder }
    val Info: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Info }
    val LightMode: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.LightMode }
    val List: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.List }
    val MenuBook: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.AutoMirrored.Rounded.MenuBook }
    val More: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.MoreVert }
    val Motion: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.MotionPhotosOn }
    val NewReleases: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.NewReleases }
    val Next: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.AutoMirrored.Rounded.NavigateNext }
    val Palette: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Palette }
    val Play: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.PlayArrow }
    val Previous: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.AutoMirrored.Rounded.NavigateBefore }
    val Refresh: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Refresh }
    val Remove: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Remove }
    val ScreenRotation: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.ScreenRotation }
    val Search: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Search }
    val Settings: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Settings }
    val Share: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Share }
    val SkipNext: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.SkipNext }
    val SkipPrevious: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.SkipPrevious }
    val Spa: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Spa }
    val Storage: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Storage }
    val SwapHoriz: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.SwapHoriz }
    val SwapVert: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.SwapVert }
    val TouchApp: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.TouchApp }
    val TrendingUp: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.TrendingUp }
    val Tune: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Tune }
    val Vibration: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Vibration }
    val ViewAgenda: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.ViewAgenda }
    val ViewCompact: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.ViewCompact }
    val ViewList: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.ViewList }
    val ViewModule: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.ViewModule }
    val Warning: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Warning }
    val Fire: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.LocalFireDepartment }
    val Trophy: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.EmojiEvents }
    val Sparkle: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.AutoAwesome }

    // Settings hub categories.
    val Shelves: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.CollectionsBookmark }
    val PrivacyLock: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.Lock }

    // Navigation pairs: outlined for idle tabs, filled for selected,
    // crossfaded on selection like the reference bottom toolbar.
    val MenuBookOutlined: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Outlined.MenuBook }
    val BarChartOutlined: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Outlined.BarChart }
    val SettingsOutlined: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Outlined.Settings }
    val BarChart: ImageVector by lazy(LazyThreadSafetyMode.NONE) { Icons.Rounded.BarChart }
}
