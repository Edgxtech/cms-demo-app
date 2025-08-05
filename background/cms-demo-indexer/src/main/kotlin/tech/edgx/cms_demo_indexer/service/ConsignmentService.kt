package tech.edgx.cms_demo_indexer.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.edgx.cms_demo_indexer.repository.ConsignmentRepository
import tech.edgx.cms_demo_indexer.domain.entity.ConsignmentEntity
import java.util.Optional

@Service
class ConsignmentService(
    private val consignmentRepository: ConsignmentRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun store(consignmentEntity: ConsignmentEntity) {
        consignmentRepository.save(consignmentEntity)
    }

    @Transactional
    fun deleteAfterSlot(absoluteSlot: Long) {
        log.info("Deleting consignments after slot: {}", absoluteSlot)
        consignmentRepository.deleteBySlotGreaterThan(absoluteSlot)
    }

    @Transactional
    fun storeIfNew(consignmentEntity: ConsignmentEntity) {
        consignmentRepository.findById(consignmentEntity.consignmentId)
            .ifPresentOrElse(
                { log.info("Consignment already exists, ignoring: {}", it.consignmentId) },
                { consignmentRepository.save(consignmentEntity) }
            )
    }

    fun exists(consignmentId: String): Boolean {
        return consignmentRepository.existsById(consignmentId)
    }

    fun find(consignmentId: String): Optional<ConsignmentEntity> {
        return consignmentRepository.findById(consignmentId)
    }
}