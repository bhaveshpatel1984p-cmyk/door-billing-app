package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Door Billing", appName)
  }

  @Test
  fun `dimension calculator square feet calculation`() {
    val sqFt = com.example.util.DimensionCalculator.calculateSqFt(78.0, 32.0, 1, "Inches")
    assertEquals(17.33, sqFt, 0.01)
  }

  @Test
  fun `format jurisdiction clause eliminates duplicates and formats correctly`() {
    val calc = com.example.util.DimensionCalculator
    assertEquals("SUBJECT TO KUDACHI JURISDICTION ONLY", calc.formatJurisdictionClause("Kudachi"))
    assertEquals("SUBJECT TO KUDACHI JURISDICTION ONLY", calc.formatJurisdictionClause("Subject to Kudachi Jurisdiction only"))
    assertEquals("SUBJECT TO KUDACHI JURISDICTION ONLY", calc.formatJurisdictionClause("SUBJECT TO SUBJECT TO KUDACHI JURISDICTION ONLY JURISDICTION"))
    assertEquals("SUBJECT TO LOCAL JURISDICTION ONLY", calc.formatJurisdictionClause("SUBJECT TO SUBJECT TO LOCAL JURISDICTION ONLY JURISDICTION"))
    assertEquals("SUBJECT TO KUDACHI JURISDICTION ONLY", calc.formatJurisdictionClause(""))
    assertEquals("SUBJECT TO KUDACHI JURISDICTION ONLY", calc.formatJurisdictionClause(null))
  }

  @Test
  fun `bill entity stores other charges like cutting charges and computes grand total correctly`() {
    val subTotal = 1500.0
    val gst = 270.0 // 18%
    val cuttingCharges = 120.0
    val discount = 50.0
    val grandTotal = (subTotal + gst + cuttingCharges) - discount

    val bill = com.example.data.db.BillEntity(
      invoiceNo = "INV/2026/0010",
      customerId = 1L,
      customerName = "Test Customer",
      customerMobile = "9876543210",
      customerAddress = "Kudachi",
      customerGstNo = "",
      subTotal = subTotal,
      cgstAmount = 135.0,
      sgstAmount = 135.0,
      discountAmount = discount,
      otherCharges = cuttingCharges,
      otherChargesDescription = "Cutting Charges",
      grandTotal = grandTotal
    )

    assertEquals(1840.0, bill.grandTotal, 0.01)
    assertEquals(120.0, bill.otherCharges, 0.01)
    assertEquals("Cutting Charges", bill.otherChargesDescription)
  }

  @Test
  fun `customer entity displayName prioritizing firmName over contact name`() {
    val custBoth = com.example.data.db.CustomerEntity(
      firmName = "Shree Ram Hardware",
      name = "Ramesh Patel",
      mobile = "9876543210"
    )
    assertEquals("Shree Ram Hardware (Ramesh Patel)", custBoth.displayName)
    assertEquals("Shree Ram Hardware", custBoth.primaryTitle)
    assertEquals("Ramesh Patel", custBoth.subtitle)

    val custFirmOnly = com.example.data.db.CustomerEntity(
      firmName = "Sharma Traders",
      name = ""
    )
    assertEquals("Sharma Traders", custFirmOnly.displayName)
    assertEquals("Sharma Traders", custFirmOnly.primaryTitle)
    assertEquals(null, custFirmOnly.subtitle)

    val custNameOnly = com.example.data.db.CustomerEntity(
      firmName = "",
      name = "Vikram Singh"
    )
    assertEquals("Vikram Singh", custNameOnly.displayName)
    assertEquals("Vikram Singh", custNameOnly.primaryTitle)
    assertEquals(null, custNameOnly.subtitle)
  }

  @Test
  fun `qr code bitmap generation creates valid bitmap`() {
    val company = com.example.data.db.CompanyProfileEntity()
    val bmp = com.example.util.QrCodeHelper.getPaymentQrBitmap(company, 1250.0, 180)
    org.junit.Assert.assertNotNull(bmp)
    assertEquals(180, bmp!!.width)
    assertEquals(180, bmp.height)
  }

  @Test
  fun `bill entity has isGstIncluded default to false`() {
    val bill = com.example.data.db.BillEntity(
      invoiceNo = "INV/2026/0001",
      customerId = 1L,
      customerName = "Customer A",
      customerMobile = "9876543210",
      customerAddress = "Locality",
      customerGstNo = "",
      subTotal = 1000.0,
      grandTotal = 1000.0
    )
    org.junit.Assert.assertFalse(bill.isGstIncluded)
  }

  @Test
  fun `saving a new bill adds to customer ledger`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.DoorDatabase::class.java).build()
    val repo = com.example.data.repository.DoorBillingRepository(
      customerDao = database.customerDao(),
      billDao = database.billDao(),
      paymentDao = database.paymentDao(),
      companyProfileDao = database.companyProfileDao(),
      supplierDao = database.supplierDao(),
      purchaseDao = database.purchaseDao(),
      purchasePaymentDao = database.purchasePaymentDao(),
      doorPresetDao = database.doorPresetDao(),
      purchaseReturnDao = database.purchaseReturnDao(),
      rawMaterialCatalogDao = database.rawMaterialCatalogDao()
    )

    val custId = repo.saveCustomer(
      com.example.data.db.CustomerEntity(
        firmName = "Patel Wood",
        name = "Bhavesh",
        mobile = "9876543210"
      )
    )

    val bill = com.example.data.db.BillEntity(
      invoiceNo = repo.generateNextInvoiceNumber(),
      customerId = custId,
      customerName = "Patel Wood",
      customerMobile = "9876543210",
      customerAddress = "Kudachi",
      customerGstNo = "",
      grandTotal = 5000.0
    )

    val items = listOf(
      com.example.data.db.BillItemEntity(
        slNo = 1,
        particular = "Flush Door 30mm",
        height = 78.0,
        width = 30.0,
        qty = 1,
        sqFt = 16.25,
        rate = 150.0,
        amount = 5000.0
      )
    )

    repo.saveBill(bill, items)

    val ledgerEntries = repo.getCustomerLedger(custId).first()
    val billEntries = ledgerEntries.filterIsInstance<com.example.data.db.LedgerEntry.BillEntry>()
    assertEquals(1, billEntries.size)
    assertEquals(5000.0, billEntries[0].grandTotal, 0.01)
  }
}

