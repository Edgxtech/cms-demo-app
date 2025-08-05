package tech.edgx.cms_demo_app.blockchain_publisher.service.tx

import com.bloxbean.cardano.client.metadata.MetadataBuilder
import com.bloxbean.cardano.client.metadata.MetadataMap
import org.cardanofoundation.lob.app.blockchain_publisher.domain.entity.txs.Organisation
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.ConsignmentEntity
import java.math.BigInteger
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Component
class ConsignmentMetadataSerialiser(
    private val clock: Clock
) {
    private val log = LoggerFactory.getLogger(ConsignmentMetadataSerialiser::class.java)

    companion object {
        const val VERSION = "1.0"
    }

    fun serializeToMetadataMap(organisationId: String, consignments: Set<ConsignmentEntity>, creationSlot: Long): MetadataMap {
        val globalMetadataMap = MetadataBuilder.createMap()
        globalMetadataMap.put("metadata", createMetadataSection(creationSlot))

        val organisationCollapsable = consignments.all { it.sender.id == organisationId }
        if (organisationCollapsable) {
            globalMetadataMap.put("org", serializeOrganisation(consignments.first().sender))
        }

        val consignmentList = MetadataBuilder.createList()
        consignments.forEach { consignment ->
            consignmentList.add(serializeConsignment(consignment, organisationCollapsable))
        }

        globalMetadataMap.put("type", "CONSIGNMENTS")
        globalMetadataMap.put("data", consignmentList)

        log.info("Serialized metadata map for organisationId={}, consignmentCount={}", organisationId, consignments.size)
        return globalMetadataMap
    }

    private fun createMetadataSection(creationSlot: Long): MetadataMap {
        val metadataMap = MetadataBuilder.createMap()
        val now = Instant.now(clock)

        metadataMap.put("creation_slot", BigInteger.valueOf(creationSlot))
        metadataMap.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(now))
        metadataMap.put("version", VERSION)

        return metadataMap
    }

    private fun serializeConsignment(
        consignment: ConsignmentEntity,
        isCollapsableOrganisation: Boolean
    ): MetadataMap {
        val metadataMap = MetadataBuilder.createMap()

        metadataMap.put("id", consignment.consignmentId ?: throw IllegalArgumentException("Consignment ID cannot be null"))
        metadataMap.put("goods", serializeGoods(consignment.goods))

        if (!isCollapsableOrganisation) {
            metadataMap.put("sender", serializeOrganisation(consignment.sender))
        }
        metadataMap.put("receiver", serializeOrganisation(consignment.receiver))

        log.info("Serialising tracking status: {}", consignment.trackingStatus)
        consignment.trackingStatus?.takeIf { it.isNotBlank() }?.let { metadataMap.put("tracking_status", it) }
        consignment.latitude?.let { latitude -> metadataMap.put("latitude", latitude.toString()) }
        consignment.longitude?.let { longitude -> metadataMap.put("longitude", longitude.toString()) }

        // Serialize dispatchedAt instead of createdAt
        val dispatchedAt = consignment.dispatchedAt ?: throw IllegalArgumentException("dispatchedAt cannot be null for consignment: ${consignment.consignmentId}")
        // Truncate to milliseconds to match database precision
        val formattedDispatchedAt = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dispatchedAt.truncatedTo(ChronoUnit.MILLIS))
        metadataMap.put("dispatched_at", formattedDispatchedAt)
        log.info("Serializing consignmentId={}, dispatchedAt={}", consignment.consignmentId, formattedDispatchedAt)

        log.info("Serialized consignment properties: {}", metadataMap)
        return metadataMap
    }

    private fun serializeOrganisation(org: Organisation): MetadataMap {
        val metadataMap = MetadataBuilder.createMap()
        metadataMap.put("id", org.id ?: throw IllegalArgumentException("Organisation ID cannot be null"))
        org.name?.takeIf { it.isNotBlank() }?.let { metadataMap.put("name", it) }
        org.taxIdNumber?.takeIf { it.isNotBlank() }?.let { metadataMap.put("tax_id_number", it) }
        org.currencyId?.takeIf { it.isNotBlank() }?.let { metadataMap.put("currency_id", it) }
        org.countryCode?.takeIf { it.isNotBlank() }?.let { metadataMap.put("country_code", it) }
        return metadataMap
    }

    private fun serializeGoods(goods: Map<String, Int>): MetadataMap {
        val goodsMap = MetadataBuilder.createMap()
        goods.forEach { (item, quantity) ->
            if (item == null) throw IllegalArgumentException("Goods item key cannot be null")
            goodsMap.put(item, BigInteger.valueOf(quantity.toLong()))
        }
        return goodsMap
    }
}