package com.example

import com.example.data.AiExtractionResult
import com.example.data.InvoiceAiService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InvoiceAiValidationTest {

    @Test
    fun emptyJson_isRejectedAsInvalidDocument() {
        val result = InvoiceAiService.parseAndValidateJson("{}")
        assertTrue(result is AiExtractionResult.InvalidDocument)
    }

    @Test
    fun nonAccountingDocumentFlag_isRejected() {
        val raw = """
            {
              "isAccountingDocument": false,
              "supplierName": "Selfie",
              "issueDate": "2026-11-16",
              "totalAmount": 10.0
            }
        """.trimIndent()
        val result = InvoiceAiService.parseAndValidateJson(raw)
        assertTrue(result is AiExtractionResult.InvalidDocument)
    }

    @Test
    fun zeroOrNegativeAmount_isRejected() {
        val raw = """
            {
              "isAccountingDocument": true,
              "supplierName": "Tesco Stores SR",
              "issueDate": "2026-11-16",
              "totalAmount": 0.0
            }
        """.trimIndent()
        val result = InvoiceAiService.parseAndValidateJson(raw)
        assertTrue(result is AiExtractionResult.InvalidDocument)
    }

    @Test
    fun validSlovakInvoice_isParsedAndNormalized() {
        val raw = """
            {
              "isAccountingDocument": true,
              "supplierName": "Tesco Stores SR, a.s.",
              "ico": "31322571",
              "dic": "2020301142",
              "issueDate": "16.11.2026",
              "totalAmount": 123.45,
              "vatAmount": 23.08,
              "vatRate": 23.0,
              "category": "Kancelária",
              "description": "Kancelársky papier"
            }
        """.trimIndent()
        val result = InvoiceAiService.parseAndValidateJson(raw)
        assertTrue(result is AiExtractionResult.Success)
        val data = (result as AiExtractionResult.Success).data
        assertEquals("Tesco Stores SR, a.s.", data.supplierName)
        assertEquals("2026-11-16", data.issueDate)
        assertEquals(123.45, data.totalAmount, 0.001)
        assertEquals(23.08, data.vatAmount!!, 0.001)
    }
}
