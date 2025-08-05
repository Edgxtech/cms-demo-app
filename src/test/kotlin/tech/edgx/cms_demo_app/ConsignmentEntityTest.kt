package tech.edgx.cms_demo_app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.ConsignmentEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ConsignmentEntityTest {

    @Test
    fun `test idControl generation`() {
        // Given
        val senderId = "sender123"
        val receiverId = "receiver456"
        val createdAt = LocalDateTime.parse("2025-07-31T12:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        // When
        val idControl = ConsignmentEntity.idControl(senderId, receiverId, createdAt)

        // Then
        val expectedInput = "$senderId::$receiverId::$createdAt" // Uses LocalDateTime.toString()
        val expectedIdControl = ConsignmentEntity.digestAsHex(expectedInput)
        assertEquals(expectedIdControl, idControl, "Generated idControl should match expected SHA-256 hash")
    }

    @Test
    fun `test id generation`() {
        // Given
        val senderId = "sender123"
        val receiverId = "receiver456"
        val createdAt = LocalDateTime.parse("2025-07-31T12:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val ver = 1L

        // When
        val id = ConsignmentEntity.id(senderId, receiverId, createdAt, ver)

        // Then
        val expectedInput = "$senderId::$receiverId::$createdAt::$ver" // Uses LocalDateTime.toString()
        println("Expected Input: $expectedInput")
        val expectedId = ConsignmentEntity.digestAsHex(expectedInput)
        assertEquals(expectedId, id, "Generated id should match expected SHA-256 hash")
    }
}