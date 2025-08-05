package tech.edgx.cms_demo_indexer.repository

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import tech.edgx.cms_demo_indexer.domain.entity.ConsignmentEntity

interface ConsignmentRepository : JpaRepository<ConsignmentEntity, String> {
    @Modifying
    @Query("DELETE FROM ConsignmentEntity c WHERE c.l1AbsoluteSlot > :absoluteSlot")
    fun deleteBySlotGreaterThan(absoluteSlot: Long)

    @Query("SELECT c FROM ConsignmentEntity c ORDER BY c.l1AbsoluteSlot ASC")
    fun findAllByOrderByL1AbsoluteSlotAsc(limit: Limit): List<ConsignmentEntity>
}