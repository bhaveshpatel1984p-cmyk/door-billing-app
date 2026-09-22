package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
}
