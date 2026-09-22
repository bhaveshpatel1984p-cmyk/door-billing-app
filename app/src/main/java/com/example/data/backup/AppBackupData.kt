package com.example.data.backup

import com.example.data.db.BillEntity
import com.example.data.db.BillItemEntity
import com.example.data.db.CompanyProfileEntity
import com.example.data.db.CustomerEntity
import com.example.data.db.PaymentEntity
import com.example.data.db.PurchaseEntity
import com.example.data.db.PurchaseItemEntity
import com.example.data.db.PurchasePaymentEntity
import com.example.data.db.SupplierEntity
import org.json.JSONArray
import org.json.JSONObject

data class AppBackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val appName: String = "Nirmal Door Billing",
    val companyProfile: CompanyProfileEntity? = null,
    val customers: List<CustomerEntity> = emptyList(),
    val bills: List<BillEntity> = emptyList(),
    val billItems: List<BillItemEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val purchases: List<PurchaseEntity> = emptyList(),
    val purchaseItems: List<PurchaseItemEntity> = emptyList(),
    val purchasePayments: List<PurchasePaymentEntity> = emptyList()
) {
    fun getSummary(): BackupSummary = BackupSummary(
        timestamp = timestamp,
        businessName = companyProfile?.businessName ?: "Nirmal Door",
        customerCount = customers.size,
        billCount = bills.size,
        paymentCount = payments.size,
        supplierCount = suppliers.size,
        purchaseCount = purchases.size
    )

    fun toJsonString(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("timestamp", timestamp)
        root.put("appName", appName)

        // Company Profile
        companyProfile?.let { cp ->
            val cpObj = JSONObject()
            cpObj.put("id", cp.id)
            cpObj.put("businessName", cp.businessName)
            cpObj.put("address", cp.address)
            cpObj.put("gstNo", cp.gstNo)
            cpObj.put("mobile", cp.mobile)
            cpObj.put("email", cp.email)
            cpObj.put("pan", cp.pan)
            cpObj.put("state", cp.state)
            cpObj.put("stateCode", cp.stateCode)
            cpObj.put("bankName", cp.bankName)
            cpObj.put("accountNo", cp.accountNo)
            cpObj.put("ifscCode", cp.ifscCode)
            cpObj.put("jurisdiction", cp.jurisdiction)
            cpObj.put("declaration", cp.declaration)
            cpObj.put("logoUri", cp.logoUri ?: "")
            cpObj.put("qrCodeUri", cp.qrCodeUri ?: "")
            cpObj.put("upiId", cp.upiId)
            root.put("companyProfile", cpObj)
        }

        // Customers
        val custArray = JSONArray()
        customers.forEach { c ->
            val o = JSONObject()
            o.put("id", c.id)
            o.put("firmName", c.firmName)
            o.put("name", c.name)
            o.put("mobile", c.mobile)
            o.put("address", c.address)
            o.put("gstNo", c.gstNo)
            o.put("createdAt", c.createdAt)
            custArray.put(o)
        }
        root.put("customers", custArray)

        // Bills
        val billsArray = JSONArray()
        bills.forEach { b ->
            val o = JSONObject()
            o.put("id", b.id)
            o.put("invoiceNo", b.invoiceNo)
            o.put("customerId", b.customerId)
            o.put("customerName", b.customerName)
            o.put("customerMobile", b.customerMobile)
            o.put("customerAddress", b.customerAddress)
            o.put("customerGstNo", b.customerGstNo)
            o.put("dateMillis", b.dateMillis)
            o.put("dimensionUnit", b.dimensionUnit)
            o.put("taxRate", b.taxRate)
            o.put("isGstIncluded", b.isGstIncluded)
            o.put("subTotal", b.subTotal)
            o.put("cgstAmount", b.cgstAmount)
            o.put("sgstAmount", b.sgstAmount)
            o.put("igstAmount", b.igstAmount)
            o.put("discountAmount", b.discountAmount)
            o.put("otherCharges", b.otherCharges)
            o.put("otherChargesDescription", b.otherChargesDescription)
            o.put("roundOffAmount", b.roundOffAmount)
            o.put("grandTotal", b.grandTotal)
            o.put("previousBalance", b.previousBalance)
            o.put("netPayable", b.netPayable)
            o.put("paidAmount", b.paidAmount)
            o.put("notes", b.notes)
            o.put("createdAt", b.createdAt)
            billsArray.put(o)
        }
        root.put("bills", billsArray)

        // Bill Items
        val itemsArray = JSONArray()
        billItems.forEach { bi ->
            val o = JSONObject()
            o.put("id", bi.id)
            o.put("billId", bi.billId)
            o.put("slNo", bi.slNo)
            o.put("particular", bi.particular)
            o.put("hsnSac", bi.hsnSac)
            o.put("height", bi.height)
            o.put("width", bi.width)
            o.put("qty", bi.qty)
            o.put("sqFt", bi.sqFt)
            o.put("rate", bi.rate)
            o.put("amount", bi.amount)
            itemsArray.put(o)
        }
        root.put("billItems", itemsArray)

        // Payments
        val payArray = JSONArray()
        payments.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("customerId", p.customerId)
            o.put("customerName", p.customerName)
            if (p.billId != null) o.put("billId", p.billId)
            o.put("amount", p.amount)
            o.put("dateMillis", p.dateMillis)
            o.put("paymentMode", p.paymentMode)
            o.put("referenceNo", p.referenceNo)
            o.put("notes", p.notes)
            o.put("createdAt", p.createdAt)
            payArray.put(o)
        }
        root.put("payments", payArray)

        // Suppliers
        val supArray = JSONArray()
        suppliers.forEach { s ->
            val o = JSONObject()
            o.put("id", s.id)
            o.put("name", s.name)
            o.put("mobile", s.mobile)
            o.put("address", s.address)
            o.put("gstNo", s.gstNo)
            o.put("createdAt", s.createdAt)
            supArray.put(o)
        }
        root.put("suppliers", supArray)

        // Purchases
        val purArray = JSONArray()
        purchases.forEach { pr ->
            val o = JSONObject()
            o.put("id", pr.id)
            o.put("invoiceNo", pr.invoiceNo)
            o.put("supplierId", pr.supplierId)
            o.put("supplierName", pr.supplierName)
            o.put("supplierMobile", pr.supplierMobile)
            o.put("supplierAddress", pr.supplierAddress)
            o.put("supplierGstNo", pr.supplierGstNo)
            o.put("dateMillis", pr.dateMillis)
            o.put("dimensionUnit", pr.dimensionUnit)
            o.put("taxRate", pr.taxRate)
            o.put("isGstIncluded", pr.isGstIncluded)
            o.put("subTotal", pr.subTotal)
            o.put("cgstAmount", pr.cgstAmount)
            o.put("sgstAmount", pr.sgstAmount)
            o.put("igstAmount", pr.igstAmount)
            o.put("discountAmount", pr.discountAmount)
            o.put("otherCharges", pr.otherCharges)
            o.put("otherChargesDescription", pr.otherChargesDescription)
            o.put("roundOffAmount", pr.roundOffAmount)
            o.put("grandTotal", pr.grandTotal)
            o.put("paidAmount", pr.paidAmount)
            o.put("notes", pr.notes)
            o.put("createdAt", pr.createdAt)
            purArray.put(o)
        }
        root.put("purchases", purArray)

        // Purchase Items
        val purItemsArray = JSONArray()
        purchaseItems.forEach { pi ->
            val o = JSONObject()
            o.put("id", pi.id)
            o.put("purchaseId", pi.purchaseId)
            o.put("slNo", pi.slNo)
            o.put("particular", pi.particular)
            o.put("hsnSac", pi.hsnSac)
            o.put("height", pi.height)
            o.put("width", pi.width)
            o.put("qty", pi.qty)
            o.put("sqFt", pi.sqFt)
            o.put("rate", pi.rate)
            o.put("amount", pi.amount)
            purItemsArray.put(o)
        }
        root.put("purchaseItems", purItemsArray)

        // Purchase Payments
        val purPayArray = JSONArray()
        purchasePayments.forEach { pp ->
            val o = JSONObject()
            o.put("id", pp.id)
            o.put("supplierId", pp.supplierId)
            o.put("supplierName", pp.supplierName)
            if (pp.purchaseId != null) o.put("purchaseId", pp.purchaseId)
            o.put("amount", pp.amount)
            o.put("dateMillis", pp.dateMillis)
            o.put("paymentMode", pp.paymentMode)
            o.put("referenceNo", pp.referenceNo)
            o.put("notes", pp.notes)
            o.put("createdAt", pp.createdAt)
            purPayArray.put(o)
        }
        root.put("purchasePayments", purPayArray)

        return root.toString(2)
    }

    companion object {
        fun fromJsonString(jsonString: String): AppBackupData {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())
            val appName = root.optString("appName", "Nirmal Door Billing")

            // Company Profile
            val companyProfile = if (root.has("companyProfile")) {
                val cp = root.getJSONObject("companyProfile")
                CompanyProfileEntity(
                    id = cp.optInt("id", 1),
                    businessName = cp.optString("businessName", "NIRMAL DOOR"),
                    address = cp.optString("address", ""),
                    gstNo = cp.optString("gstNo", ""),
                    mobile = cp.optString("mobile", ""),
                    email = cp.optString("email", ""),
                    pan = cp.optString("pan", ""),
                    state = cp.optString("state", "Gujarat"),
                    stateCode = cp.optString("stateCode", "24"),
                    bankName = cp.optString("bankName", ""),
                    accountNo = cp.optString("accountNo", ""),
                    ifscCode = cp.optString("ifscCode", ""),
                    jurisdiction = cp.optString("jurisdiction", "Kudachi"),
                    declaration = cp.optString("declaration", ""),
                    logoUri = cp.optString("logoUri").ifBlank { null },
                    qrCodeUri = cp.optString("qrCodeUri").ifBlank { null },
                    upiId = cp.optString("upiId", "")
                )
            } else null

            // Customers
            val custList = mutableListOf<CustomerEntity>()
            if (root.has("customers")) {
                val arr = root.getJSONArray("customers")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    custList.add(
                        CustomerEntity(
                            id = o.optLong("id", 0L),
                            firmName = o.optString("firmName", ""),
                            name = o.optString("name", "Unknown"),
                            mobile = o.optString("mobile", ""),
                            address = o.optString("address", ""),
                            gstNo = o.optString("gstNo", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Bills
            val billList = mutableListOf<BillEntity>()
            if (root.has("bills")) {
                val arr = root.getJSONArray("bills")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    billList.add(
                        BillEntity(
                            id = o.optLong("id", 0L),
                            invoiceNo = o.optString("invoiceNo", "INV-0001"),
                            customerId = o.optLong("customerId", 0L),
                            customerName = o.optString("customerName", ""),
                            customerMobile = o.optString("customerMobile", ""),
                            customerAddress = o.optString("customerAddress", ""),
                            customerGstNo = o.optString("customerGstNo", ""),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            dimensionUnit = o.optString("dimensionUnit", "Inches"),
                            taxRate = o.optDouble("taxRate", 18.0),
                            isGstIncluded = o.optBoolean("isGstIncluded", true),
                            subTotal = o.optDouble("subTotal", 0.0),
                            cgstAmount = o.optDouble("cgstAmount", 0.0),
                            sgstAmount = o.optDouble("sgstAmount", 0.0),
                            igstAmount = o.optDouble("igstAmount", 0.0),
                            discountAmount = o.optDouble("discountAmount", 0.0),
                            otherCharges = o.optDouble("otherCharges", 0.0),
                            otherChargesDescription = o.optString("otherChargesDescription", "Cutting Charges"),
                            roundOffAmount = o.optDouble("roundOffAmount", 0.0),
                            grandTotal = o.optDouble("grandTotal", 0.0),
                            previousBalance = o.optDouble("previousBalance", 0.0),
                            netPayable = o.optDouble("netPayable", o.optDouble("grandTotal", 0.0) + o.optDouble("previousBalance", 0.0)),
                            paidAmount = o.optDouble("paidAmount", 0.0),
                            notes = o.optString("notes", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Bill Items
            val billItemsList = mutableListOf<BillItemEntity>()
            if (root.has("billItems")) {
                val arr = root.getJSONArray("billItems")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    billItemsList.add(
                        BillItemEntity(
                            id = o.optLong("id", 0L),
                            billId = o.optLong("billId", 0L),
                            slNo = o.optInt("slNo", i + 1),
                            particular = o.optString("particular", ""),
                            hsnSac = o.optString("hsnSac", "4418"),
                            height = o.optDouble("height", 0.0),
                            width = o.optDouble("width", 0.0),
                            qty = o.optInt("qty", 1),
                            sqFt = o.optDouble("sqFt", 0.0),
                            rate = o.optDouble("rate", 0.0),
                            amount = o.optDouble("amount", 0.0)
                        )
                    )
                }
            }

            // Payments
            val payList = mutableListOf<PaymentEntity>()
            if (root.has("payments")) {
                val arr = root.getJSONArray("payments")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    payList.add(
                        PaymentEntity(
                            id = o.optLong("id", 0L),
                            customerId = o.optLong("customerId", 0L),
                            customerName = o.optString("customerName", ""),
                            billId = if (o.has("billId") && !o.isNull("billId")) o.getLong("billId") else null,
                            amount = o.optDouble("amount", 0.0),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            paymentMode = o.optString("paymentMode", "Cash"),
                            referenceNo = o.optString("referenceNo", ""),
                            notes = o.optString("notes", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Suppliers
            val supList = mutableListOf<SupplierEntity>()
            if (root.has("suppliers")) {
                val arr = root.getJSONArray("suppliers")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    supList.add(
                        SupplierEntity(
                            id = o.optLong("id", 0L),
                            name = o.optString("name", "Unknown Supplier"),
                            mobile = o.optString("mobile", ""),
                            address = o.optString("address", ""),
                            gstNo = o.optString("gstNo", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Purchases
            val purList = mutableListOf<PurchaseEntity>()
            if (root.has("purchases")) {
                val arr = root.getJSONArray("purchases")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    purList.add(
                        PurchaseEntity(
                            id = o.optLong("id", 0L),
                            invoiceNo = o.optString("invoiceNo", "PUR-0001"),
                            supplierId = o.optLong("supplierId", 0L),
                            supplierName = o.optString("supplierName", ""),
                            supplierMobile = o.optString("supplierMobile", ""),
                            supplierAddress = o.optString("supplierAddress", ""),
                            supplierGstNo = o.optString("supplierGstNo", ""),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            dimensionUnit = o.optString("dimensionUnit", "Inches"),
                            taxRate = o.optDouble("taxRate", 18.0),
                            isGstIncluded = o.optBoolean("isGstIncluded", true),
                            subTotal = o.optDouble("subTotal", 0.0),
                            cgstAmount = o.optDouble("cgstAmount", 0.0),
                            sgstAmount = o.optDouble("sgstAmount", 0.0),
                            igstAmount = o.optDouble("igstAmount", 0.0),
                            discountAmount = o.optDouble("discountAmount", 0.0),
                            otherCharges = o.optDouble("otherCharges", 0.0),
                            otherChargesDescription = o.optString("otherChargesDescription", "Transportation"),
                            roundOffAmount = o.optDouble("roundOffAmount", 0.0),
                            grandTotal = o.optDouble("grandTotal", 0.0),
                            paidAmount = o.optDouble("paidAmount", 0.0),
                            notes = o.optString("notes", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Purchase Items
            val purItemsList = mutableListOf<PurchaseItemEntity>()
            if (root.has("purchaseItems")) {
                val arr = root.getJSONArray("purchaseItems")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    purItemsList.add(
                        PurchaseItemEntity(
                            id = o.optLong("id", 0L),
                            purchaseId = o.optLong("purchaseId", 0L),
                            slNo = o.optInt("slNo", i + 1),
                            particular = o.optString("particular", ""),
                            hsnSac = o.optString("hsnSac", "4418"),
                            height = o.optDouble("height", 0.0),
                            width = o.optDouble("width", 0.0),
                            qty = o.optInt("qty", 1),
                            sqFt = o.optDouble("sqFt", 0.0),
                            rate = o.optDouble("rate", 0.0),
                            amount = o.optDouble("amount", 0.0)
                        )
                    )
                }
            }

            // Purchase Payments
            val purPayList = mutableListOf<PurchasePaymentEntity>()
            if (root.has("purchasePayments")) {
                val arr = root.getJSONArray("purchasePayments")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    purPayList.add(
                        PurchasePaymentEntity(
                            id = o.optLong("id", 0L),
                            supplierId = o.optLong("supplierId", 0L),
                            supplierName = o.optString("supplierName", ""),
                            purchaseId = if (o.has("purchaseId") && !o.isNull("purchaseId")) o.getLong("purchaseId") else null,
                            amount = o.optDouble("amount", 0.0),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            paymentMode = o.optString("paymentMode", "Bank Transfer"),
                            referenceNo = o.optString("referenceNo", ""),
                            notes = o.optString("notes", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            return AppBackupData(
                version = version,
                timestamp = timestamp,
                appName = appName,
                companyProfile = companyProfile,
                customers = custList,
                bills = billList,
                billItems = billItemsList,
                payments = payList,
                suppliers = supList,
                purchases = purList,
                purchaseItems = purItemsList,
                purchasePayments = purPayList
            )
        }
    }
}

data class BackupSummary(
    val timestamp: Long,
    val businessName: String,
    val customerCount: Int,
    val billCount: Int,
    val paymentCount: Int,
    val supplierCount: Int,
    val purchaseCount: Int
)
