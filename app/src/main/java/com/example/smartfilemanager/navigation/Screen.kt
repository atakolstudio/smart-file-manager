package com.example.smartfilemanager.navigation

/**
 * Uygulamadaki tüm gezinme rotalarını tek noktadan tanımlar.
 * Böylece rota isimleri kod içinde dağınık string literal olarak kalmaz.
 */
sealed class Screen(val route: String) {
    data object Permission : Screen("permission")
    data object Home : Screen("home")
    data object Files : Screen("files")
    data object Preview : Screen("preview")
    data object Favorites : Screen("favorites")
    data object Settings : Screen("settings")
    data object RecycleBin : Screen("recycle_bin")
    data object Apps : Screen("apps")
    data object StorageAnalysis : Screen("storage_analysis")

    companion object {
        const val FILES_PATH_ARG = "path"
        const val PREVIEW_PATH_ARG = "path"

        fun filesRouteWithPath(path: String): String {
            val encoded = java.net.URLEncoder.encode(path, "UTF-8")
            return "${Files.route}?$FILES_PATH_ARG=$encoded"
        }

        fun previewRouteWithPath(path: String): String {
            val encoded = java.net.URLEncoder.encode(path, "UTF-8")
            return "${Preview.route}?$PREVIEW_PATH_ARG=$encoded"
        }
    }
}
