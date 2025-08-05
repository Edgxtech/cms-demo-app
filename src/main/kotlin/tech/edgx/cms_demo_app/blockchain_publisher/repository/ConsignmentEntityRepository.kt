package tech.edgx.cms_demo_app.blockchain_publisher.repository

import jakarta.persistence.LockModeType
import org.cardanofoundation.lob.app.blockchain_publisher.domain.core.BlockchainPublishStatus
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.ConsignmentEntity
import java.util.*

interface ConsignmentEntityRepository : JpaRepository<ConsignmentEntity, String> {

    @Query("""
        SELECT c FROM ConsignmentEntity c
        WHERE c.sender.id = :organisationId
        AND c.consignmentId = :consignmentId
        AND c.l1SubmissionData.publishStatus IN :publishStatuses
        ORDER BY c.ver DESC
        LIMIT 1
    """)
    fun findLatestByConsignmentIdAndStatus(
        @Param("organisationId") organisationId: String,
        @Param("consignmentId") consignmentId: String,
        @Param("publishStatuses") publishStatuses: Set<BlockchainPublishStatus>
    ): Optional<ConsignmentEntity>

    @Query("""
        SELECT c FROM ConsignmentEntity c
        WHERE c.idControl = :idControl
        ORDER BY c.ver DESC
        LIMIT 1
    """)
    fun findLatestByIdControl(@Param("idControl") idControl: String): ConsignmentEntity?

    @Query("""
        SELECT c FROM ConsignmentEntity c
        WHERE c.sender.id = :organisationId
        AND c.l1SubmissionData.publishStatus IN :publishStatuses
        ORDER BY c.createdAt ASC, c.consignmentId ASC
    """)
    fun findConsignmentsByStatus(
        @Param("organisationId") organisationId: String,
        @Param("publishStatuses") publishStatuses: Set<BlockchainPublishStatus>,
        limit: Limit
    ): Set<ConsignmentEntity>

    @Query("""
        SELECT c FROM ConsignmentEntity c
        WHERE c.sender.id = :organisationId
        AND c.l1SubmissionData.publishStatus IN :publishStatuses
        AND c.l1SubmissionData IS NOT NULL
        ORDER BY c.createdAt ASC, c.consignmentId ASC
    """)
    fun findDispatchedConsignmentsThatAreNotFinalizedYet(
        @Param("organisationId") organisationId: String,
        @Param("publishStatuses") notFinalisedButVisibleOnChain: Set<BlockchainPublishStatus>,
        limit: Limit
    ): Set<ConsignmentEntity>

    @Query("""
    SELECT c FROM ConsignmentEntity c
    WHERE c.l1SubmissionData IS NOT NULL
    AND c.l1SubmissionData.publishStatus IN :publishStatuses
    ORDER BY c.createdAt ASC, c.consignmentId ASC
    """)
    fun findAllDispatchedConsignmentsThatAreNotFinalizedYet(
        @Param("publishStatuses") notFinalisedButVisibleOnChain: Set<BlockchainPublishStatus>,
        limit: Limit
    ): Set<ConsignmentEntity>


    @Query("SELECT c FROM ConsignmentEntity c WHERE c.idControl = :idControl")
    fun findByIdControl(idControl: String): List<ConsignmentEntity>

    @Query("SELECT c FROM ConsignmentEntity c WHERE c.consignmentId = :consignmentId ORDER BY c.ver DESC")
    fun findTopByConsignmentIdOrderByVerDesc(consignmentId: String): Optional<ConsignmentEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ConsignmentEntity c WHERE c.sender.id = :organisationId AND c.l1SubmissionData IS NULL ORDER BY c.ver DESC")
    fun findAndLockConsignmentsReadyToBeDispatched(organisationId: String, limit: Limit): List<ConsignmentEntity>

    @Query("SELECT c FROM ConsignmentEntity c WHERE c.sender.id = :organisationId AND c.l1SubmissionData IS NOT NULL AND c.l1SubmissionData.finalityScore NOT IN ('FINALIZED') ORDER BY c.ver DESC")
    fun findDispatchedConsignmentsThatAreNotFinalizedYet(organisationId: String, limit: Limit): List<ConsignmentEntity>
}