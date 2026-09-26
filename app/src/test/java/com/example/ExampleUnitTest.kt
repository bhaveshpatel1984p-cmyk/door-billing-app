package com.example

import com.example.data.db.CompanyProfileEntity
import com.example.util.QrCodeHelper
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testUpiPayloadGeneration() {
    val company = CompanyProfileEntity()
    val effectiveUpi = QrCodeHelper.resolveEffectiveUpiId(company)
    assertEquals("nirmaldoor@upi", effectiveUpi)
    val upiString = QrCodeHelper.buildUpiString(effectiveUpi, company.businessName, 1500.0)
    assertTrue(upiString.startsWith("upi://pay?pa=nirmaldoor@upi"))
    assertTrue(upiString.contains("am=1500.00"))
    assertTrue(upiString.contains("cu=INR"))
  }

  @Test
  fun testPurchaseItemUnitsFormatting() {
    val calc = com.example.util.DimensionCalculator
    assertEquals("10 Pcs", calc.formatQtyWithUnit(10.0, "Pcs"))
    assertEquals("2.5 Kg", calc.formatQtyWithUnit(2.5, "Kg"))
    assertEquals("1.75 Ltr", calc.formatQtyWithUnit(1.75, "Ltr"))
    assertEquals("100 Meter", calc.formatQtyWithUnit(100.0, "Meter"))
    assertEquals("5 Box", calc.formatQtyWithUnit(5.0, "Box"))
    assertEquals("1 Drum", calc.formatQtyWithUnit(1.0, "Drum"))

    val kgItem = com.example.data.db.PurchaseItemEntity(
      slNo = 1,
      particular = "Fevicol Marine",
      qty = 25.5,
      rate = 220.0,
      amount = 25.5 * 220.0,
      unit = "Kg"
    )
    assertEquals("25.5 Kg", kgItem.formattedQtyWithUnit)
    assertEquals(5610.0, kgItem.amount, 0.01)

    val ltrItem = com.example.data.db.PurchaseItemEntity(
      slNo = 2,
      particular = "Wood Polish Varnish",
      qty = 5.0,
      rate = 450.0,
      amount = 5.0 * 450.0,
      unit = "Ltr"
    )
    assertEquals("5 Ltr", ltrItem.formattedQtyWithUnit)
    assertEquals(2250.0, ltrItem.amount, 0.01)
  }
}

