package com.banka.app

import com.banka.app.data.csv.CsvParser
import com.banka.app.domain.Categorizer
import com.banka.app.domain.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CsvParserTest {

    @Test
    fun `detecte le point virgule`() {
        assertEquals(';', CsvParser.detectSeparator("Date;Libellé;Montant\n01/02/2025;Carrefour;-45,20\n"))
    }

    @Test
    fun `detecte la virgule`() {
        assertEquals(',', CsvParser.detectSeparator("Date,Label,Amount\n2025-02-01,Carrefour,-45.20\n"))
    }

    @Test
    fun `parse montant francais avec espaces et euro`() {
        assertEquals(1234.56, CsvParser.parseAmount("1 234,56")!!, 0.001)
        assertEquals(1234.56, CsvParser.parseAmount("1 234,56 €")!!, 0.001)
    }

    @Test
    fun `parse montant avec parenthese negative`() {
        assertEquals(-45.2, CsvParser.parseAmount("(45,20)")!!, 0.001)
        assertEquals(-45.2, CsvParser.parseAmount("45,20-")!!, 0.001)
    }

    @Test
    fun `parse date format fr et iso`() {
        val expected = LocalDate.of(2025, 2, 1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, CsvParser.parseDate("01/02/2025"))
        assertEquals(expected, CsvParser.parseDate("2025-02-01"))
        assertNull(CsvParser.parseDate("pas une date"))
    }

    @Test
    fun `parse export credit agricole avec colonnes debit credit`() {
        val csv = "Date;Libellé;Débit;Crédit\n" +
            "01/02/2025;CARREFOUR MARKET;25,90;\n" +
            "02/02/2025;SALAIRE;;2 300,00\n"
        val result = CsvParser.parse(csv)
        assertEquals(2, result.rows.size)
        assertEquals(0, result.skippedRows)
        assertEquals(-25.9, result.rows[0].amount, 0.001)
        assertEquals(2300.0, result.rows[1].amount, 0.001)
        assertEquals("CARREFOUR MARKET", result.rows[0].label)
    }

    @Test
    fun `parse sans en-tete en devinant les colonnes`() {
        val csv = "01/02/2025;CARREFOUR;-25,90\n03/02/2025;SNCF;-42,00\n"
        val result = CsvParser.parse(csv)
        assertEquals(2, result.rows.size)
        assertEquals(-25.9, result.rows[0].amount, 0.001)
        assertEquals("CARREFOUR", result.rows[0].label)
    }

    @Test
    fun `gerer les champions entre guillemets`() {
        val csv = "Date;Libellé;Montant\n" +
            "01/02/2025;\"CARREFOUR; MARKET\";-10,00\n"
        val result = CsvParser.parse(csv)
        assertEquals(1, result.rows.size)
        assertEquals("CARREFOUR; MARKET", result.rows[0].label)
    }

    @Test
    fun `declarer des lignes incompletes`() {
        val csv = "Date;Libellé;Montant\n01/02/2025;;-10,00\nxx/yy/zzzz;Test;5\n"
        val result = CsvParser.parse(csv)
        assertEquals(0, result.rows.size)
        assertEquals(2, result.skippedRows)
    }

    @Test
    fun `categoriser les libelles courants`() {
        assertEquals(Category.ALIMENTATION, Categorizer.classify("CARREFOUR MARKET Paris"))
        assertEquals(Category.REVENUS, Categorizer.classify("VIREMENT SALAIRE ACME"))
        assertEquals(Category.TRANSPORT, Categorizer.classify("UBER TRIP"))
        assertEquals(Category.ABONNEMENTS, Categorizer.classify("NETFLIX.COM"))
        assertEquals(Category.AUTRE, Categorizer.classify("TRUC INCONNU"))
    }

    @Test
    fun `retour vide sur fichier vide`() {
        val result = CsvParser.parse("")
        assertNotNull(result)
        assertEquals(0, result.rows.size)
    }
}
