package tech.edgx.cms_demo_app.blockchain_reader.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import tech.edgx.cms_demo_app.blockchain_publisher.repository.ConsignmentEntityRepositoryGateway
import tech.edgx.cms_demo_app.blockchain_reader.domain.IndexerConsignmentEntity

@Service
class ConsignmentBlockchainReaderService(
    private val consignmentRepositoryGateway: ConsignmentEntityRepositoryGateway,
    private val consignmentMetadataDeserialiserService: ConsignmentMetadataDeserialiserService,
    private val restClient: RestClient,
    @Value("\${lob.l1.transaction.metadata_label:1448}") private val metadataLabel: Int,
    @Value("\${lob.blockchain_reader.lob_follower_base_url:http://localhost:9090/api/v1/}") private val followerBaseUrl: String,
    @Value("\${lob.blockchain_reader.lob_follower_indexer_url:http://localhost:9090/yaci-api/}") private val indexerBaseUrl: String,
    @Value("\${lob.blockchain_reader.consignment_batch_size:1000}") private val batchSize: Int
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Scheduled(fixedRateString = "\${lob.blockchain_reader.rate.ms:60000}")
    @Transactional
    fun processNewConsignments() {
        log.info("Polling for consignment blockchain transactions to be read from the blockchain...")
        val indexedConsignments = fetchIndexedConsignments()
        log.info("Found ${indexedConsignments.size} consignments waiting ingestion")

        for (indexedConsignment in indexedConsignments) {
            val consignmentId = indexedConsignment.consignmentId
            val transactionHash = indexedConsignment.l1TransactionHash
            log.debug("Processing consignmentId: {}, transactionHash: {}", consignmentId, transactionHash)

            // Skip if already processed
            if (consignmentRepositoryGateway.findById(consignmentId).isPresent) {
                log.debug("Consignment {} already exists, skipping", consignmentId)
                continue
            }

            val metadata = fetchMetadata(transactionHash) ?: continue
            log.info("Onchain state for consignment from metadata: {}", metadata)

            // Extract type directly from the JSON metadata
            val type = metadata["type"] as? String
            if (type != "CONSIGNMENTS") {
                log.warn("Skipping non-consignment metadata for transactionHash: $transactionHash, type: $type")
                continue
            }

            try {
                val consignments = consignmentMetadataDeserialiserService.decodeConsignments(
                    metadata,
                    transactionHash,
                    indexedConsignment.l1AbsoluteSlot
                )
                log.debug("Decoded consignments: {}", consignments)
                val consignment = consignments.find { it.consignmentId == consignmentId }
                    ?: continue

                consignmentRepositoryGateway.storeConsignment(consignment)
                log.info("Stored consignment: $consignmentId")
            } catch (e: Exception) {
                log.warn("Failed to deserialize consignment id: {}, transactionHash: {}. Reason: {}. Skipping this consignment.",
                    consignmentId, transactionHash, e.message)
                continue
            }
        }
        log.info("Polling for consignment blockchain transactions to be read from the blockchain...done")
    }

    private fun fetchIndexedConsignments(): List<IndexerConsignmentEntity> {
        return try {
            restClient.get()
                .uri("$followerBaseUrl/consignments?limit=$batchSize")
                .retrieve()
                .body(Array<IndexerConsignmentEntity>::class.java)
                ?.toList() ?: emptyList()
        } catch (e: Exception) {
            log.error("Failed to fetch consignments from indexer", e)
            emptyList()
        }
    }

    private fun fetchMetadata(transactionHash: String): Map<String, Any>? {
        return try {
            val responseType = object : ParameterizedTypeReference<Array<Map<String, Any>>>() {}
            val response = restClient.get()
                .uri("$indexerBaseUrl/txs/$transactionHash/metadata")
                .retrieve()
                .body(responseType)
            log.debug("Fetch metadata response: {}", response)
            val metadata = response?.find { it["label"] == metadataLabel.toString() }
            metadata?.get("json_metadata") as? Map<String, Any>
        } catch (e: Exception) {
            log.error("Failed to fetch metadata for transactionHash: $transactionHash", e)
            null
        }
    }
}