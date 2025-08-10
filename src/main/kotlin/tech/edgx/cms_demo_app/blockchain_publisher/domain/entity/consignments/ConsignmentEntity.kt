package tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.Optional
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import org.cardanofoundation.lob.app.blockchain_publisher.domain.entity.txs.Organisation
import org.cardanofoundation.lob.app.blockchain_publisher.domain.entity.txs.L1SubmissionData
import org.cardanofoundation.lob.app.support.spring_audit.CommonDateOnlyEntity
import org.hibernate.annotations.Type
import java.time.LocalDateTime
import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8

@Entity
@Table(name = "blockchain_publisher_consignment")
@EntityListeners(AuditingEntityListener::class)
data class ConsignmentEntity(
    @Id
    @Column(name = "consignment_id", nullable = false, length = 64)
    val consignmentId: String,

    @Column(name = "id_control", nullable = false, length = 64)
    val idControl: String,

    @Column(name = "ver", nullable = false)
    val ver: Long,

    @Type(JsonBinaryType::class)
    @Column(columnDefinition = "jsonb")
    val goods: Map<String, Int>,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "id", column = Column(name = "sender_id")),
        AttributeOverride(name = "name", column = Column(name = "sender_name")),
        AttributeOverride(name = "countryCode", column = Column(name = "sender_country_code")),
        AttributeOverride(name = "taxIdNumber", column = Column(name = "sender_tax_id_number")),
        AttributeOverride(name = "currencyId", column = Column(name = "sender_currency_id"))
    )
    val sender: Organisation,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "id", column = Column(name = "receiver_id")),
        AttributeOverride(name = "name", column = Column(name = "receiver_name")),
        AttributeOverride(name = "countryCode", column = Column(name = "receiver_country_code")),
        AttributeOverride(name = "taxIdNumber", column = Column(name = "receiver_tax_id_number")),
        AttributeOverride(name = "currencyId", column = Column(name = "receiver_currency_id"))
    )
    val receiver: Organisation,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "transactionHash", column = Column(name = "l1_transaction_hash", length = 64)),
        AttributeOverride(name = "absoluteSlot", column = Column(name = "l1_absolute_slot")),
        AttributeOverride(name = "creationSlot", column = Column(name = "l1_creation_slot")),
        AttributeOverride(name = "finalityScore", column = Column(name = "l1_finality_score", columnDefinition = "blockchain_publisher_finality_score_type")),
        AttributeOverride(name = "publishStatus", column = Column(name = "l1_publish_status", columnDefinition = "blockchain_publisher_blockchain_publish_status_type"))
    )
    var l1SubmissionData: L1SubmissionData? = null,

    @Column(name = "tracking_status", length = 50)
    val trackingStatus: String? = null,

    @Column(name = "latitude")
    val latitude: Double? = null,

    @Column(name = "longitude")
    val longitude: Double? = null,

    @Column(name = "dispatched_at", nullable = false)
    val dispatchedAt: LocalDateTime // New field
) : CommonDateOnlyEntity(), Persistable<String> {

    constructor() : this(
        consignmentId = "",
        idControl = "",
        ver = 1L,
        goods = emptyMap(),
        sender = Organisation("", "", "", "", ""),
        receiver = Organisation("", "", "", "", ""),
        l1SubmissionData = null,
        trackingStatus = null,
        latitude = null,
        longitude = null,
        dispatchedAt = LocalDateTime.now()
    )

    override fun getId(): String = consignmentId
    override fun isNew(): Boolean = createdAt == null
    fun getL1SubmissionData(): Optional<L1SubmissionData> = Optional.ofNullable(l1SubmissionData)
    fun setL1SubmissionData(l1SubmissionData: Optional<L1SubmissionData>) {
        this.l1SubmissionData = l1SubmissionData.orElse(null)
    }

    companion object {
        fun digestAsHex(input: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(input.toByteArray(UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun idControl(senderId: String, receiverId: String, dispatchedAt: LocalDateTime): String {
            val input = "$senderId::$receiverId::$dispatchedAt"
            return digestAsHex(input)
        }

        fun id(senderId: String, receiverId: String, dispatchedAt: LocalDateTime, ver: Long): String {
            val input = "$senderId::$receiverId::$dispatchedAt::$ver"
            return digestAsHex(input)
        }
    }
}