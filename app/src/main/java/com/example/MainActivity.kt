package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.BillWithItems
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.CompanyProfileScreen
import com.example.ui.screens.CreateCustomerScreen
import com.example.ui.screens.CustomerBalanceScreen
import com.example.ui.screens.CustomerLedgerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditEntryScreen
import com.example.ui.screens.FinancialStatsScreen
import com.example.ui.screens.InvoiceViewDialog
import com.example.ui.screens.NewEntryScreen
import com.example.ui.screens.PurchaseHubScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.DoorBillingViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val billingViewModel: DoorBillingViewModel = viewModel()
        DoorBillingMainApp(viewModel = billingViewModel)
      }
    }
  }
}

@Composable
fun DoorBillingMainApp(
  viewModel: DoorBillingViewModel
) {
  val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
  val company by viewModel.companyProfile.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  var isAppLocked by remember { mutableStateOf(true) }
  var activeInvoicePreview by remember { mutableStateOf<BillWithItems?>(null) }

  if (isAppLocked) {
    AppLockScreen(
      onUnlockSuccess = {
        isAppLocked = false
      }
    )
    return
  }

  // Observe toast/snackbar messages from ViewModel
  LaunchedEffect(Unit) {
    viewModel.uiMessages.collectLatest { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  // Handle hardware back button navigation
  BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
    when (currentScreen) {
      AppScreen.CUSTOMER_LEDGER -> viewModel.navigateTo(AppScreen.CUSTOMER_BALANCE)
      else -> viewModel.navigateTo(AppScreen.DASHBOARD)
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (currentScreen) {
        AppScreen.DASHBOARD -> {
          DashboardScreen(
            viewModel = viewModel,
            onViewBill = { billWithItems -> activeInvoicePreview = billWithItems }
          )
        }

        AppScreen.CREATE_CUSTOMER -> {
          CreateCustomerScreen(viewModel = viewModel)
        }

        AppScreen.NEW_ENTRY -> {
          NewEntryScreen(viewModel = viewModel)
        }

        AppScreen.EDIT_ENTRY -> {
          EditEntryScreen(
            viewModel = viewModel,
            onViewBill = { billWithItems -> activeInvoicePreview = billWithItems }
          )
        }

        AppScreen.CUSTOMER_BALANCE -> {
          CustomerBalanceScreen(viewModel = viewModel)
        }

        AppScreen.CUSTOMER_LEDGER -> {
          CustomerLedgerScreen(
            viewModel = viewModel,
            onViewBill = { billWithItems -> activeInvoicePreview = billWithItems }
          )
        }

        AppScreen.COMPANY_PROFILE -> {
          CompanyProfileScreen(viewModel = viewModel)
        }

        AppScreen.FINANCIAL_STATS -> {
          FinancialStatsScreen(viewModel = viewModel)
        }

        AppScreen.PURCHASE_HUB -> {
          PurchaseHubScreen(viewModel = viewModel)
        }
      }

      // Detailed Invoice Modal Dialog when tapped from Dashboard, Edit Entry, or Ledger
      activeInvoicePreview?.let { billWithItems ->
        InvoiceViewDialog(
          billWithItems = billWithItems,
          company = company,
          onDismiss = { activeInvoicePreview = null },
          onEdit = {
            activeInvoicePreview = null
            viewModel.startEditBill(billWithItems)
          }
        )
      }
    }
  }
}
