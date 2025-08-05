package tech.edgx.cms_demo_app.blockchain_publisher.service.tx

import com.bloxbean.cardano.client.account.Account
import com.bloxbean.cardano.client.api.model.Amount
import com.bloxbean.cardano.client.backend.api.BackendService
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil
import com.bloxbean.cardano.client.exception.CborSerializationException
import com.bloxbean.cardano.client.function.helper.SignerProviders
import com.bloxbean.cardano.client.metadata.Metadata
import com.bloxbean.cardano.client.metadata.MetadataBuilder
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadataMap
import com.bloxbean.cardano.client.metadata.helper.MetadataToJsonNoSchemaConverter
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder
import com.bloxbean.cardano.client.quicktx.Tx
import com.bloxbean.cardano.client.transaction.util.TransactionUtil
import com.google.common.collect.Sets
import io.vavr.control.Either
import jakarta.annotation.PostConstruct
import org.apache.commons.collections4.iterators.PeekingIterator
import org.cardanofoundation.lob.app.blockchain_publisher.domain.core.SerializedCardanoL1Transaction
import org.cardanofoundation.lob.app.blockchain_reader.BlockchainReaderPublicApiIF
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.zalando.problem.Problem
import org.zalando.problem.Status
import tech.edgx.cms_demo_app.blockchain_publisher.domain.core.ConsignmentBlockchainTransactions
import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.ConsignmentEntity
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

@Component
class ConsignmentL1TransactionCreator(
    @Qualifier("yaci_blockfrost") private val backendService: BackendService,
    private val consignmentMetadataSerialiser: ConsignmentMetadataSerialiser,
    private val blockchainReaderPublicApi: BlockchainReaderPublicApiIF,
    @Qualifier("lob_owner_account") private val organiserAccount: Account, // Changed to lob_owner_account
    @Value("\${lob.l1.transaction.metadata_label:1448}") private val metadataLabel: Int,
    @Value("\${lob.l1.transaction.debug_store_output_tx:false}") private val debugStoreOutputTx: Boolean
) {
    private val log = LoggerFactory.getLogger(ConsignmentL1TransactionCreator::class.java)
    private lateinit var runId: String

    companion object {
        private const val CARDANO_MAX_TRANSACTION_SIZE_BYTES = 16000
    }

    @PostConstruct
    fun init() {
        log.info("ConsignmentL1TransactionCreator::metadata label: {}", metadataLabel)
        log.info("ConsignmentL1TransactionCreator::debug store output tx: {}", debugStoreOutputTx)
        runId = UUID.randomUUID().toString()
        log.info("ConsignmentL1TransactionCreator::runId: {}", runId)
        log.info("ConsignmentL1TransactionCreator is initialised.")
    }

    fun pullBlockchainTransaction(organisationId: String, consignments: Set<ConsignmentEntity>): Either<Problem, Optional<ConsignmentBlockchainTransactions>> {
        return blockchainReaderPublicApi.getChainTip()
            .flatMap { chainTip -> handleTransactionCreation(organisationId, consignments, chainTip.absoluteSlot) }
    }

    private fun handleTransactionCreation(
        organisationId: String,
        consignments: Set<ConsignmentEntity>,
        creationSlot: Long
    ): Either<Problem, Optional<ConsignmentBlockchainTransactions>> {
        try {
            return createTransaction(organisationId, consignments, creationSlot)
        } catch (e: IOException) {
            log.error("Error creating blockchain transaction: ", e)
            return Either.left(
                Problem.builder()
                    .withTitle("ERROR_CREATING_TRANSACTION")
                    .withDetail("Exception encountered: ${e.message}")
                    .withStatus(Status.INTERNAL_SERVER_ERROR)
                    .build()
            )
        }
    }

    private fun createTransaction(
        organisationId: String,
        consignments: Set<ConsignmentEntity>,
        creationSlot: Long
    ): Either<Problem, Optional<ConsignmentBlockchainTransactions>> {
        log.info("Splitting {} consignments into blockchain transactions", consignments.size)

        val consignmentsBatch = LinkedHashSet<ConsignmentEntity>()
        val iterator = PeekingIterator.peekingIterator(consignments.iterator())

        while (iterator.hasNext()) {
            val consignment = iterator.next()
            consignmentsBatch.add(consignment)

            val serializedTransactionsE = serializeTransactionChunk(organisationId, consignmentsBatch, creationSlot)
            if (serializedTransactionsE.isLeft) {
                log.error("Error serializing transaction, abort processing, issue: {}", serializedTransactionsE.getLeft().getDetail())
                return Either.left(serializedTransactionsE.getLeft())
            }

            val serializedTransaction = serializedTransactionsE.get()
            val txBytes = serializedTransaction.txBytes()

            if (!iterator.hasNext()) {
                log.info("Processing final batch of size: {}", consignmentsBatch.size)
                log.info("Blockchain transaction created, id: {}, debugTxOutput: {}", TransactionUtil.getTxHash(txBytes), debugStoreOutputTx)
                potentiallyStoreTxs(creationSlot, serializedTransaction)

                val remaining = Sets.difference(consignments, consignmentsBatch)

                return Either.right(
                    Optional.of(
                        ConsignmentBlockchainTransactions(
                            organisationId,
                            consignmentsBatch,
                            remaining,
                            creationSlot,
                            txBytes,
                            organiserAccount.baseAddress()
                        )
                    )
                )
            }

            val consignmentPeek = iterator.peek()
            val newChunkTxBytesE = serializeTransactionChunk(
                organisationId,
                Stream.concat(consignmentsBatch.stream(), Stream.of(consignmentPeek)).collect(Collectors.toSet()),
                creationSlot
            )

            if (newChunkTxBytesE.isLeft) {
                log.error("Error serializing transaction, abort processing, issue: {}", newChunkTxBytesE.getLeft().getDetail())
                return Either.left(newChunkTxBytesE.getLeft())
            }

            val newChunkTxBytes = newChunkTxBytesE.get().txBytes()
            if (newChunkTxBytes.size >= CARDANO_MAX_TRANSACTION_SIZE_BYTES) {
                log.info("Blockchain transaction created, id: {}, debugTxOutput: {}", TransactionUtil.getTxHash(txBytes), debugStoreOutputTx)
                potentiallyStoreTxs(creationSlot, serializedTransaction)

                val remainingConsignments = Sets.difference(consignments, consignmentsBatch)

                return Either.right(
                    Optional.of(
                        ConsignmentBlockchainTransactions(
                            organisationId,
                            consignmentsBatch,
                            remainingConsignments,
                            creationSlot,
                            txBytes,
                            organiserAccount.baseAddress()
                        )
                    )
                )
            }
        }

        if (consignmentsBatch.isNotEmpty()) {
            log.info("Leftovers batch size: {}", consignmentsBatch.size)
            val serializedTxE = serializeTransactionChunk(organisationId, consignmentsBatch, creationSlot)

            if (serializedTxE.isLeft) {
                log.error("Error serializing transaction, abort processing, issue: {}", serializedTxE.getLeft().getDetail())
                return Either.left(serializedTxE.getLeft())
            }

            val serializedTx = serializedTxE.get()
            val txBytes = serializedTx.txBytes()
            log.info("Blockchain transaction created, id: {}, debugTxOutput: {}", TransactionUtil.getTxHash(txBytes), debugStoreOutputTx)
            potentiallyStoreTxs(creationSlot, serializedTx)

            val remaining = Sets.difference(consignments, consignmentsBatch)

            return Either.right(
                Optional.of(
                    ConsignmentBlockchainTransactions(
                        organisationId,
                        consignmentsBatch,
                        remaining,
                        creationSlot,
                        txBytes,
                        organiserAccount.baseAddress()
                    )
                )
            )
        }

        return Either.right(Optional.empty())
    }

    private fun potentiallyStoreTxs(creationSlot: Long, tx: SerializedCardanoL1Transaction) {
        if (debugStoreOutputTx) {
            val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            val prefix = "cms-consignments-metadata-$runId-$timestamp-$creationSlot"
            val tmpJsonTxFile = Files.createTempFile(prefix, ".json")
            val tmpCborFile = Files.createTempFile(prefix, ".cbor")

            log.info("DebugStoreTx enabled, storing JSON tx metadata to file: {}", tmpJsonTxFile)
            Files.writeString(tmpJsonTxFile, tx.metadataJson())
            log.info("DebugStoreTx enabled, storing CBOR tx metadata to file: {}", tmpCborFile)
            Files.write(tmpCborFile, tx.metadataCbor())
        }
    }

    private fun serializeTransactionChunk(
        organisationId: String,
        consignmentsBatch: Set<ConsignmentEntity>,
        creationSlot: Long
    ): Either<Problem, SerializedCardanoL1Transaction> {
        try {
            val metadataMap = consignmentMetadataSerialiser.serializeToMetadataMap(organisationId, consignmentsBatch, creationSlot)
            val data = metadataMap.map
            log.info("Metadata map contents: {}", data) // Log the map to inspect its contents
            val bytes = CborSerializationUtil.serialize(data)
            val json = MetadataToJsonNoSchemaConverter.cborBytesToJson(bytes)

            val metadata = MetadataBuilder.createMetadata()
            val cborMetadataMap = CBORMetadataMap(data)
            metadata.put(BigInteger.valueOf(metadataLabel.toLong()), cborMetadataMap)

            log.info("Metadata for consignment tx prepared, serializing tx now...")
            val serializedTx = serializeTransaction(metadata)

            return Either.right(SerializedCardanoL1Transaction(serializedTx, bytes, json))
        } catch (e: Exception) {
            log.error("Error serializing metadata to CBOR", e)
            return Either.left(
                Problem.builder()
                    .withTitle("ERROR_SERIALISING_METADATA")
                    .withDetail("Error serializing metadata to CBOR: ${e.message}")
                    .withStatus(Status.INTERNAL_SERVER_ERROR)
                    .build()
            )
        }
    }

    @Throws(CborSerializationException::class)
    protected fun serializeTransaction(metadata: Metadata): ByteArray {
        val quickTxBuilder = QuickTxBuilder(backendService)
        val tx = Tx()
            .payToAddress(organiserAccount.baseAddress(), Amount.ada(2.0))
            .attachMetadata(metadata)
            .from(organiserAccount.baseAddress())

        return quickTxBuilder.compose(tx)
            .withSigner(SignerProviders.signerFrom(organiserAccount))
            .buildAndSign()
            .serialize()
    }
}