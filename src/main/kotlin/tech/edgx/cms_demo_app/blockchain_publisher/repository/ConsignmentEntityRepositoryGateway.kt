package tech.edgx.cms_demo_app.blockchain_publisher.repository

import com.google.common.collect.Sets
import org.cardanofoundation.lob.app.blockchain_publisher.domain.core.BlockchainPublishStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.ConsignmentEntity
import java.time.Clock
import java.time.Duration
import java.util.*

@Service
@Transactional(readOnly = true)
class ConsignmentEntityRepositoryGateway(
    private val consignmentEntityRepository: ConsignmentEntityRepository,
    private val clock: Clock,
    @Value("\${lob.blockchain_publisher.dispatcher.lock_timeout:PT3H}") private val lockTimeoutDuration: Duration = Duration.ofHours(3)
) {

    private val log = LoggerFactory.getLogger(ConsignmentEntityRepositoryGateway::class.java)

    fun findById(consignmentId: String): Optional<ConsignmentEntity> {
        return consignmentEntityRepository.findById(consignmentId)
    }

    fun findConsignmentsByStatus(organisationId: String, pullConsignmentsBatchSize: Int): Set<ConsignmentEntity> {
        val dispatchStatuses = BlockchainPublishStatus.toDispatchStatuses()
        val limit = Limit.of(pullConsignmentsBatchSize)
        return consignmentEntityRepository.findConsignmentsByStatus(organisationId, dispatchStatuses, limit)
    }

    fun deleteById(consignmentId: String) {
        consignmentEntityRepository.deleteById(consignmentId)
    }

    fun findAll(): List<ConsignmentEntity> {
         return consignmentEntityRepository.findAll()
    }

    fun findByIdControl(idControl: String): List<ConsignmentEntity> {
        return consignmentEntityRepository.findByIdControl(idControl)
    }

    fun findLatestByIdControl(idControl: String): ConsignmentEntity? {
        return consignmentEntityRepository.findLatestByIdControl(idControl)
    }

    fun findDispatchedConsignmentsThatAreNotFinalizedYet(organisationId: String, limit: Limit): Set<ConsignmentEntity> {
        val notFinalisedButVisibleOnChain = BlockchainPublishStatus.notFinalisedButVisibleOnChain()
        return consignmentEntityRepository.findDispatchedConsignmentsThatAreNotFinalizedYet(
            organisationId,
            notFinalisedButVisibleOnChain,
            limit
        )
    }

    fun findAllDispatchedConsignmentsThatAreNotFinalizedYet(limit: Limit): Set<ConsignmentEntity> {
        val notFinalisedButVisibleOnChain = BlockchainPublishStatus.notFinalisedButVisibleOnChain()
        return consignmentEntityRepository.findAllDispatchedConsignmentsThatAreNotFinalizedYet(
            notFinalisedButVisibleOnChain,
            limit
        )
    }

    @Transactional
    fun storeOnlyNew(consignmentEntities: Set<ConsignmentEntity>): Set<ConsignmentEntity> {
        log.info("StoreOnlyNewConsignments:{}", consignmentEntities.size)

        val consignmentIds = consignmentEntities
            .map { it.consignmentId }
            .toSet()
        log.debug("Consignment Ids: {}", consignmentIds)

        val existingConsignments = HashSet(consignmentEntityRepository.findAllById(consignmentIds))
        log.debug("Existing consignments: {}", existingConsignments)

        val newConsignments = Sets.difference(consignmentEntities, existingConsignments)
        log.debug("New consignments: {}", newConsignments)

        return consignmentEntityRepository.saveAll(newConsignments).toSet()
    }

    @Transactional
    fun storeConsignment(consignmentEntity: ConsignmentEntity) {
        consignmentEntityRepository.save(consignmentEntity)
    }

    @Transactional
    fun storeConsignments(successfullyUpdatedConsignmentEntities: Set<ConsignmentEntity>) {
        consignmentEntityRepository.saveAll(successfullyUpdatedConsignmentEntities)
    }
}