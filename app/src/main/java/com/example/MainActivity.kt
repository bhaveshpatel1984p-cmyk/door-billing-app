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
import com.example.ui.screens.BackupSyncScreen
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

// Door billing management application main entry point
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    bringToFrontSafely()
    setContent {
      MyApplicationTheme {
        val billingViewModel: DoorBillingViewModel = viewModel()
        DoorBillingMainApp(viewModel = billingViewModel)
      }
    }
  }

  override fun onStart() {
    super.onStart()
    bringToFrontSafely()
  }

  override fun onResume() {
    super.onResume()
    bringToFrontSafely()
  }

  private fun bringToFrontSafely() {
    try {
      val am = getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
      am?.moveTaskToFront(taskId, android.app.ActivityManager.MOVE_TASK_WITH_HOME)
    } catch (_: Exception) {}
  }
}

@Composable
fun DoorBillingMainApp(
  viewModel: DoorBillingViewModel
) {
  val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
  val company by viewModel.companyProfile.collectAsStateWithLifecycle()
  val isPinLockEnabled by viewModel.isPinLockEnabled.collectAsStateWithLifecycle()
  val appSecurityPin by viewModel.appSecurityPin.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  var isAppLocked by remember { mutableStateOf(true) }
  var activeInvoicePreview by remember { mutableStateOf<BillWithItems?>(null) }

  if (isPinLockEnabled && isAppLocked) {
    AppLockScreen(
      expectedPin = appSecurityPin,
      onUnlockSuccess = {
        isAppLocked = false
      },
      onResetPin = { verification ->
        viewModel.resetPinWithMasterOrMobile(verification)
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

  val context = androidx.compose.ui.platform.LocalContext.current
  var lastBackPressTime by remember { mutableStateOf(0L) }

  // Handle hardware back button navigation safely
  BackHandler(enabled = true) {
    when (currentScreen) {
      AppScreen.DASHBOARD -> {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
          (context as? android.app.Activity)?.finish()
        } else {
          lastBackPressTime = currentTime
          android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
        }
      }
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

        AppScreen.BACKUP_SYNC -> {
          BackupSyncScreen(viewModel = viewModel)
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
          },
          onConvertToInvoice = {
            viewModel.convertQuotationToInvoice(billWithItems)
          }
        )
      }
    }
  }
}
