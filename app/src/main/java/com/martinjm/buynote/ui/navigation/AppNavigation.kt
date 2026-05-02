package com.martinjm.buynote.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.martinjm.buynote.ui.screens.catalog.CatalogScreen
import com.martinjm.buynote.ui.screens.categories.CategoriesScreen
import com.martinjm.buynote.ui.screens.history.HistoryScreen
import com.martinjm.buynote.ui.screens.listdetail.ListDetailScreen
import com.martinjm.buynote.ui.screens.lists.ActiveListsScreen
import com.martinjm.buynote.ui.screens.productform.ProductFormScreen
import com.martinjm.buynote.ui.screens.scanner.ScannerScreen

object Routes {
    const val LISTS = "lists"
    const val LIST_DETAIL = "list_detail/{listId}"
    const val CATALOG = "catalog"
    const val PRODUCT_FORM = "product_form?productId={productId}&name={name}&barcode={barcode}"
    const val CATEGORIES = "categories"
    const val HISTORY = "history"
    const val SCANNER = "scanner?listId={listId}"

    fun listDetail(listId: Long) = "list_detail/$listId"
    fun productForm(productId: Long? = null, name: String? = null, barcode: String? = null): String {
        val params = mutableListOf("productId=${productId ?: -1L}")
        if (name != null) params.add("name=${Uri.encode(name)}")
        if (barcode != null) params.add("barcode=${Uri.encode(barcode)}")
        return "product_form?${params.joinToString("&")}"
    }
    fun scanner(listId: Long) = "scanner?listId=$listId"
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.LISTS) {
        composable(Routes.LISTS) {
            ActiveListsScreen(navController = navController)
        }
        composable(
            route = Routes.LIST_DETAIL,
            arguments = listOf(navArgument("listId") { type = NavType.LongType })
        ) {
            ListDetailScreen(navController = navController)
        }
        composable(Routes.CATALOG) {
            CatalogScreen(navController = navController)
        }
        composable(
            route = Routes.PRODUCT_FORM,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("name") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("barcode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            ProductFormScreen(navController = navController)
        }
        composable(Routes.CATEGORIES) {
            CategoriesScreen(navController = navController)
        }
        composable(Routes.HISTORY) {
            HistoryScreen(navController = navController)
        }
        composable(
            route = Routes.SCANNER,
            arguments = listOf(navArgument("listId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            ScannerScreen(navController = navController)
        }
    }
}
