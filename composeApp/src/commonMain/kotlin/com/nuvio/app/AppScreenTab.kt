package com.nuvio.app

import com.nuvio.app.core.ui.NativeNavigationTab

enum class AppScreenTab {
    Home,
    Movies,
    Series,
    Trending,
    Search,
    Library,
    Settings,
    MyList,
    Kids,
    Iptv,
    LiveTv,
    ;

    companion object {
        fun fromName(name: String): AppScreenTab =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Home
    }
}

internal fun AppScreenTab.toNativeNavigationTab(): NativeNavigationTab = when (this) {
    AppScreenTab.Home -> NativeNavigationTab.Home
    AppScreenTab.Search -> NativeNavigationTab.Search
    AppScreenTab.Library -> NativeNavigationTab.Library
    AppScreenTab.Settings -> NativeNavigationTab.Settings
    AppScreenTab.MyList -> NativeNavigationTab.MyList
    // The native tab bar has a fixed tab set, so the IPTV and Matrix tabs fall back to Home there.
    AppScreenTab.Kids -> NativeNavigationTab.Home
    AppScreenTab.Iptv -> NativeNavigationTab.Home
    AppScreenTab.LiveTv -> NativeNavigationTab.Home
    AppScreenTab.Movies -> NativeNavigationTab.Home
    AppScreenTab.Series -> NativeNavigationTab.Home
    AppScreenTab.Trending -> NativeNavigationTab.Home
}

internal fun NativeNavigationTab.toAppScreenTab(): AppScreenTab = when (this) {
    NativeNavigationTab.Home -> AppScreenTab.Home
    NativeNavigationTab.Search -> AppScreenTab.Search
    NativeNavigationTab.Library -> AppScreenTab.Library
    NativeNavigationTab.Settings -> AppScreenTab.Settings
    NativeNavigationTab.MyList -> AppScreenTab.MyList
}
