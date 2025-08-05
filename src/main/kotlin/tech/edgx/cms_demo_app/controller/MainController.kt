package tech.edgx.cms_demo_app.controller

import jakarta.transaction.Transactional
import org.cardanofoundation.lob.app.organisation.domain.entity.Organisation
import org.cardanofoundation.lob.app.organisation.domain.entity.OrganisationCurrency
import org.cardanofoundation.lob.app.organisation.repository.OrganisationCurrencyRepository
import org.cardanofoundation.lob.app.support.modulith.EventMetadata
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import tech.edgx.cms_demo_app.blockchain.domain.event.ConsignmentLedgerUpdateCommand
import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.Consignment
import tech.edgx.cms_demo_app.blockchain_publisher.domain.entity.consignments.ConsignmentEntity
import tech.edgx.cms_demo_app.blockchain_publisher.repository.ConsignmentEntityRepositoryGateway
import tech.edgx.cms_demo_app.blockchain_publisher.repository.CustomOrganisationRepository
import tech.edgx.cms_demo_app.blockchain_publisher.service.ConsignmentConverter
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.cardanofoundation.lob.app.organisation.domain.entity.OrganisationCurrency.Id as OrganisationCurrencyId

@RestController
@RequestMapping("/api", produces = [MediaType.APPLICATION_JSON_VALUE])
class MainController(
    private val consignmentEntityRepositoryGateway: ConsignmentEntityRepositoryGateway,
    private val consignmentConverter: ConsignmentConverter,
    private val organisationCurrencyRepository: OrganisationCurrencyRepository,
    private val organisationRepository: CustomOrganisationRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    private val log = LoggerFactory.getLogger(MainController::class.java)

    @GetMapping("/hello")
    fun hello(): String {
        return "Hello"
    }

    // Organisation CRUD Endpoints

    @PostMapping("/organisations")
    fun createOrganisation(@RequestBody org: Organisation): ResponseEntity<Organisation> {
        log.info("Creating organisation: ${org.name}, ID: ${org.id}..")
        val existingOrgById = organisationRepository.findById(org.id)
        if (existingOrgById.isPresent) {
            log.info("Organisation ${org.id} or ${org.name} already exists")
            return ResponseEntity.ok(existingOrgById.get())
        }
        if (org.currencyId.isNullOrBlank()) {
            log.error("Currency ID is null or blank for organisation: ${org.id}")
            throw IllegalArgumentException("Currency ID cannot be null or blank")
        }
        val savedOrg = organisationRepository.save(org)
        val currencyId = OrganisationCurrency.Id(savedOrg.id, "CUST_${savedOrg.name}")
        organisationCurrencyRepository.findById(currencyId).ifPresent { id ->
            log.info("Deleting existing OrganisationCurrency for organisation: ${savedOrg.id}, customer_code: CUST_${savedOrg.name}")
            organisationCurrencyRepository.deleteById(currencyId)
        }
        val organisationCurrency = OrganisationCurrency(currencyId, savedOrg.currencyId)
        log.debug("Saving OrganisationCurrency: id=${currencyId.organisationId}, customer_code=${currencyId.customerCode}, currency_id=${organisationCurrency.currencyId}")
        organisationCurrencyRepository.save(organisationCurrency)
        return ResponseEntity.ok(savedOrg)
    }

    @GetMapping("/organisations")
    fun getAllOrganisations(): ResponseEntity<List<Organisation>> {
        log.info("Fetching all organisations")
        return ResponseEntity.ok(organisationRepository.findAll())
    }

    @GetMapping("/organisations/{id}")
    fun getOrganisationById(@PathVariable id: String): ResponseEntity<Organisation> {
        log.info("Fetching organisation by ID: $id")
        val org = organisationRepository.findById(id)
        return if (org.isPresent) {
            ResponseEntity.ok(org.get())
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
    }

    @GetMapping("/organisations/name/{name}")
    fun getOrganisationByName(@PathVariable name: String): ResponseEntity<Organisation> {
        log.info("Fetching organisation by name: $name")
        val org = organisationRepository.findByName(name)
        return if (org.isPresent) {
            ResponseEntity.ok(org.get())
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
    }

    @PutMapping("/organisations/{id}")
    fun updateOrganisation(
        @PathVariable id: String,
        @RequestBody org: Organisation
    ): ResponseEntity<Organisation> {
        log.info("Updating organisation: $id")
        val existingOrg = organisationRepository.findById(id)
        if (!existingOrg.isPresent) {
            log.info("Organisation $id not found, cannot update")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }

        val currentOrg = existingOrg.get()
        currentOrg.name = org.name
        currentOrg.city = org.city
        currentOrg.postCode = org.postCode
        currentOrg.province = org.province
        currentOrg.countryCode = org.countryCode
        currentOrg.address = org.address
        currentOrg.phoneNumber = org.phoneNumber
        currentOrg.taxIdNumber = org.taxIdNumber
        currentOrg.preApproveTransactions = org.preApproveTransactions
        currentOrg.preApproveTransactionsDispatch = org.preApproveTransactionsDispatch
        currentOrg.accountPeriodDays = org.accountPeriodDays
        currentOrg.currencyId = org.currencyId
        currentOrg.reportCurrencyId = org.reportCurrencyId
        currentOrg.websiteUrl = org.websiteUrl
        currentOrg.adminEmail = org.adminEmail
        currentOrg.logo = org.logo

        val updatedOrg = organisationRepository.save(currentOrg)
        organisationCurrencyRepository.save(
            OrganisationCurrency(
                OrganisationCurrencyId(updatedOrg.id, "CUST_${updatedOrg.name}"),
                updatedOrg.currencyId
            )
        )
        return ResponseEntity.ok(updatedOrg)
    }

    @DeleteMapping("/organisations/{id}")
    fun deleteOrganisation(@PathVariable id: String): ResponseEntity<Void> {
        log.info("Deleting organisation: $id")
        val org = organisationRepository.findById(id)
        if (!org.isPresent) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
        organisationCurrencyRepository.deleteById(OrganisationCurrencyId(id, "CUST_${org.get().name}"))
        organisationRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    // Consignment CRUD Endpoints

    @PostMapping("/consignments")
    @Transactional
    fun createConsignment(@RequestBody consignment: Consignment): ResponseEntity<Consignment> {
        log.info("Creating consignment")
        if (consignment.sender?.id == null) {
            throw IllegalArgumentException("Sender ID cannot be null")
        }
        if (consignment.receiver?.id == null) {
            throw IllegalArgumentException("Receiver ID cannot be null")
        }
        val dispatchedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS) // Ensure millisecond precision
        log.info("Controller dispatchedAt: {}", dispatchedAt)
        val ver = 1L
        val consignmentId = ConsignmentEntity.id(
            consignment.sender.id,
            consignment.receiver.id,
            dispatchedAt,
            ver
        )
        val consignmentIdControl = ConsignmentEntity.idControl(
            consignment.sender.id,
            consignment.receiver.id,
            dispatchedAt
        )
        val updatedConsignment = consignment.copy(
            id = consignmentId,
            ver = ver,
            idControl = consignmentIdControl,
            dispatchedAt = dispatchedAt
        )
        applicationEventPublisher.publishEvent(
            ConsignmentLedgerUpdateCommand.create(
                EventMetadata.create(ConsignmentLedgerUpdateCommand.VERSION),
                consignment.sender.id,
                mutableSetOf(updatedConsignment)
            )
        )
        log.info("Created consignment: id={}, ver={}, idControl={}, dispatchedAt={}",
            consignmentId, ver, consignmentIdControl, dispatchedAt)
        return ResponseEntity.ok(updatedConsignment)
    }

    @PutMapping("/consignments/{id}")
    @Transactional
    fun updateConsignment(
        @PathVariable id: String,
        @RequestBody consignment: Consignment
    ): ResponseEntity<Consignment> {
        log.debug("Updating consignment: {}", id)
        if (consignment.sender?.id == null) {
            throw IllegalArgumentException("Sender ID cannot be null")
        }
        if (consignment.receiver?.id == null) {
            throw IllegalArgumentException("Receiver ID cannot be null")
        }
        // Find the latest version to determine the next ver
        val latestVersion = consignmentEntityRepositoryGateway.findLatestByIdControl(id)
        log.debug("Latest version of {}: {}", id, latestVersion)
        if (latestVersion == null) {
            log.error("Consignment not found: $id")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
        // Detach existing entity to avoid session conflict
        val newVersion = latestVersion.ver.plus(1)
        log.debug("New version: {}", newVersion)
        val dispatchedAt = latestVersion.dispatchedAt
        val newConsignmentId = ConsignmentEntity.id(
            consignment.sender.id,
            consignment.receiver.id,
            dispatchedAt,
            newVersion
        )
        val updatedConsignment = consignment.copy(
            id = newConsignmentId,
            ver = newVersion,
            idControl = id,
            dispatchedAt = dispatchedAt
        )
        // trigger storage
        applicationEventPublisher.publishEvent(
            ConsignmentLedgerUpdateCommand.create(
                EventMetadata.create(ConsignmentLedgerUpdateCommand.VERSION),
                consignment.sender.id,
                mutableSetOf(updatedConsignment)
            )
        )

        log.info("Updated consignment for blockchain publishing: id=$newConsignmentId, ver=$newVersion")
        return ResponseEntity.ok(updatedConsignment)
    }

    @GetMapping("/consignments")
    fun getAllConsignments(): ResponseEntity<List<Consignment>> {
        log.debug("Fetching all consignments")
        val entities = consignmentEntityRepositoryGateway.findAll()
        val consignments = consignmentConverter.convertFromDbToCanonicalForm(entities.toSet()).toList()
        return ResponseEntity.ok(consignments)
    }

    @GetMapping("/consignments/{id}")
    fun getConsignmentById(@PathVariable id: String): ResponseEntity<Consignment> {
        log.debug("Fetching consignment by ID: {}", id)
        val consignment = consignmentEntityRepositoryGateway.findLatestByIdControl(id)
        return if (consignment != null) {
            ResponseEntity.ok(consignmentConverter.convertToCanonical(consignment))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
    }

    @DeleteMapping("/consignments/{id}")
    @Transactional
    fun deleteConsignment(@PathVariable id: String): ResponseEntity<String> {
        log.debug("Deleting consignment by idControl: {}", id)
        val consignments = consignmentEntityRepositoryGateway.findByIdControl(id)
        if (!consignments.isEmpty()) {
            log.info("Consignment $id not found")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
        consignments.forEach {
            consignmentEntityRepositoryGateway.deleteById(it.id)
            log.info("Deleted ConsignmentEntity: id=${it.id}")
        }
        return ResponseEntity.ok("Deleted ${consignments.size}")
    }
}