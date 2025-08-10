package tech.edgx.cms_demo_app.blockchain_reader.service

import org.cardanofoundation.lob.app.blockchain_publisher.domain.core.BlockchainPublishStatus
import org.cardanofoundation.lob.app.blockchain_publisher.domain.entity.txs.L1SubmissionData
import org.cardanofoundation.lob.app.blockchain_publisher.domain.entity.txs.Organisation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.ConsignmentEntity
import tech.edgx.cms_demo_app.blockchain_publisher.repository.ConsignmentEntityRepositoryGateway
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class ConsignmentMetadataDeserialiserService(
    private val consignmentRepository: ConsignmentEntityRepositoryGateway,
    @Value("\${lob.l1.transaction.metadata_label:1448}") private val metadataLabel: Int
) {
    private val log = LoggerFactory.getLogger(ConsignmentMetadataDeserialiserService::class.java)

    fun decodeConsignments(payload: Map<String, Any>, txHash: String, slot: Long): Set<ConsignmentEntity> {
        val consignments = mutableSetOf<ConsignmentEntity>()
        val orgMap = payload["org"] as? Map<String, Any>
            ?: throw IllegalArgumentException("Missing 'org' in metadata")
        val orgId = orgMap["id"] as? String
            ?: throw IllegalArgumentException("Missing 'org.id' in metadata")
        val type = payload["type"] as? String
        if (type != "CONSIGNMENTS") {
            log.warn("Skipping non-consignment metadata: type=$type")
            return emptySet()
        }

        val dataList = payload["data"] as? List<Map<String, Any>>
            ?: throw IllegalArgumentException("Missing 'data' in metadata")
        val metadataMap = payload["metadata"] as? Map<String, Any>
            ?: throw IllegalArgumentException("Missing 'metadata' in metadata")
        val creationSlot = (metadataMap["creation_slot"] as? Number)?.toLong()
            ?: throw IllegalArgumentException("Missing 'creation_slot' in metadata")

        for (consignmentMap in dataList) {
            val id = consignmentMap["id"] as? String
                ?: throw IllegalArgumentException("Missing 'id' in consignment")
            log.debug("Consignment id: {}, txhash: {}, creation slot: {}", id, txHash, creationSlot)
            val consignment = deserialiseConsignment(consignmentMap, orgId, txHash, slot, creationSlot, orgMap)
            consignments.add(consignment)
        }

        return consignments
    }

    private fun deserialiseConsignment(
        consignmentMap: Map<String, Any>,
        orgId: String,
        txHash: String,
        absoluteSlot: Long,
        creationSlot: Long,
        orgMap: Map<String, Any>
    ): ConsignmentEntity {
        val id = consignmentMap["id"] as? String
            ?: throw IllegalArgumentException("Missing 'id' in consignment")
        val goodsMap = consignmentMap["goods"] as? Map<String, Any>
            ?: throw IllegalArgumentException("Missing 'goods' in consignment")

        val dispatchedAt = consignmentMap["dispatched_at"]?.let { dispatchedAtStr ->
            val parsed = LocalDateTime.parse(dispatchedAtStr as String, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            parsed
        } ?: throw IllegalArgumentException("Missing 'dispatched_at' in consignment")

        val goods = mutableMapOf<String, Int>()
        goodsMap.forEach { (key, value) ->
            val intValue = when (value) {
                is Int -> value
                is Long -> value.toInt()
                is Number -> value.toInt()
                else -> throw IllegalArgumentException("Unsupported goods value type for key $key: ${value?.javaClass ?: "null"}")
            }
            goods[key] = intValue
        }

        val receiverMap = consignmentMap["receiver"] as? Map<String, Any>
            ?: throw IllegalArgumentException("Missing 'receiver' in consignment")
        val receiverId = receiverMap["id"] as? String
            ?: throw IllegalArgumentException("Missing 'receiver.id' in consignment")
        val trackingStatus = consignmentMap["tracking_status"] as? String
        val latitude = when (val latValue = consignmentMap["latitude"]) {
            is Number -> latValue.toDouble()
            is String -> latValue.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid 'latitude' format: $latValue")
            else -> throw IllegalArgumentException("Unsupported 'latitude' type: ${latValue?.javaClass ?: "null"}")
        }
        val longitude = when (val lonValue = consignmentMap["longitude"]) {
            is Number -> lonValue.toDouble()
            is String -> lonValue.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid 'longitude' format: $lonValue")
            else -> throw IllegalArgumentException("Unsupported 'longitude' type: ${lonValue?.javaClass ?: "null"}")
        }

        val sender = Organisation().apply {
            this.id = orgId
            name = (orgMap["name"] as? String) ?: ""
            countryCode = (orgMap["country_code"] as? String) ?: ""
            taxIdNumber = (orgMap["tax_id_number"] as? String) ?: ""
            currencyId = (orgMap["currency_id"] as? String) ?: ""
        }
        val receiver = Organisation().apply {
            this.id = receiverId
            name = (receiverMap["name"] as? String) ?: ""
            countryCode = (receiverMap["country_code"] as? String) ?: ""
            taxIdNumber = (receiverMap["tax_id_number"] as? String) ?: ""
            currencyId = (receiverMap["currency_id"] as? String) ?: ""
        }

        val idControl = ConsignmentEntity.Companion.idControl(sender.id, receiver.id, dispatchedAt)
        val latestConsignment = consignmentRepository.findLatestByIdControl(idControl)
        val ver = latestConsignment?.let { it.ver + 1 } ?: 1L
        val consignmentId = ConsignmentEntity.Companion.id(sender.id, receiver.id, dispatchedAt, ver)
        log.debug("idControl: {}, latestConsignment: {}, next ver: {}, computedId: {}, onchain Id: {}", idControl, latestConsignment, ver, consignmentId, id)

        if (id != consignmentId) {
            log.error("Metadata id ($id) does not match computed consignmentId ($consignmentId) for idControl: $idControl")
            throw IllegalStateException("Consignment ID mismatch detected")
        }

        return ConsignmentEntity(
            consignmentId = consignmentId,
            idControl = idControl,
            ver = ver,
            goods = goods,
            sender = sender,
            receiver = receiver,
            l1SubmissionData = L1SubmissionData.builder()
                .transactionHash(txHash)
                .absoluteSlot(absoluteSlot)
                .creationSlot(creationSlot)
                .publishStatus(BlockchainPublishStatus.SUBMITTED)
                .finalityScore(null)
                .build(),
            trackingStatus = trackingStatus,
            latitude = latitude,
            longitude = longitude,
            dispatchedAt = dispatchedAt
        )
    }
}