package tech.edgx.cms_demo_app.blockchain_publisher.service

import org.slf4j.LoggerFactory
import org.cardanofoundation.lob.app.blockchain_publisher.domain.core.BlockchainPublishStatus
import org.cardanofoundation.lob.app.blockchain_publisher.domain.entity.txs.L1SubmissionData
import org.springframework.stereotype.Service
import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.ConsignmentEntity
import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.Consignment

@Service
class ConsignmentConverter(
    private val organisationConverter: OrganisationConverter
) {
    private val log = LoggerFactory.getLogger(ConsignmentConverter::class.java)

    fun convertToDbDetached(consignment: Consignment): ConsignmentEntity {
        val ver = consignment.ver ?: 1L
        val dispatchedAt = consignment.dispatchedAt ?: throw IllegalArgumentException("dispatchedAt cannot be null")
        val senderId = consignment.sender?.id ?: ""
        val receiverId = consignment.receiver?.id ?: ""
        return ConsignmentEntity(
            consignmentId = consignment.id ?: ConsignmentEntity.id(senderId, receiverId, dispatchedAt, ver),
            idControl = consignment.idControl ?: ConsignmentEntity.idControl(senderId, receiverId, dispatchedAt),
            ver = ver,
            goods = consignment.goods,
            sender = organisationConverter.convertToBlockchainOrganisation(consignment.sender)
                ?: throw IllegalArgumentException("Sender organisation cannot be null for consignment: ${consignment.id}"),
            receiver = organisationConverter.convertToBlockchainOrganisation(consignment.receiver)
                ?: throw IllegalArgumentException("Receiver organisation cannot be null for consignment: ${consignment.id}"),
            l1SubmissionData = L1SubmissionData.builder()
                .publishStatus(BlockchainPublishStatus.STORED)
                .build(),
            trackingStatus = consignment.trackingStatus,
            latitude = consignment.latitude,
            longitude = consignment.longitude,
            dispatchedAt = dispatchedAt
        ).also {
            log.debug("ConsignmentEntity created: consignmentId={}, idControl={}, dispatchedAt={}",
                it.consignmentId, it.idControl, it.dispatchedAt)
        }
    }

    fun convertFromDbToCanonicalForm(entities: Set<ConsignmentEntity>): Set<Consignment> {
        log.debug("Converting {} ConsignmentEntity objects to canonical form", entities.size)
        return entities.map { convertToCanonical(it) }.toSet()
    }

    fun convertToCanonical(entity: ConsignmentEntity): Consignment {
        log.debug("Converting ConsignmentEntity to canonical Consignment: id={}", entity.consignmentId)
        return Consignment(
            id = entity.consignmentId,
            ver = entity.ver,
            goods = entity.goods,
            sender = organisationConverter.convertToCoreOrganisation(entity.sender),
            receiver = organisationConverter.convertToCoreOrganisation(entity.receiver),
            trackingStatus = entity.trackingStatus,
            latitude = entity.latitude,
            longitude = entity.longitude,
            l1SubmissionData = entity.l1SubmissionData,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            dispatchedAt = entity.dispatchedAt
        ).also {
            log.debug("Converted to Consignment: id={}", it.id)
        }
    }
}