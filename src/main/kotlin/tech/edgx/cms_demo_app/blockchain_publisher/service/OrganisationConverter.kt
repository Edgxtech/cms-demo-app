package tech.edgx.cms_demo_app.blockchain_publisher.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.cardanofoundation.lob.app.blockchain_publisher.domain.entity.txs.Organisation as BlockchainOrganisation
import org.cardanofoundation.lob.app.organisation.domain.entity.Organisation as CoreOrganisation

@Component
class OrganisationConverter {

    private val logger = LoggerFactory.getLogger(OrganisationConverter::class.java)

    fun convertToBlockchainOrganisation(coreOrg: CoreOrganisation?): BlockchainOrganisation? {
        if (coreOrg == null) {
            logger.warn("Core organisation is null, returning null")
            return null
        }
        logger.debug("Converting CoreOrganisation to BlockchainOrganisation: id={}", coreOrg.id)
        return BlockchainOrganisation().apply {
            id = coreOrg.id
            name = coreOrg.name
            countryCode = coreOrg.countryCode
            taxIdNumber = coreOrg.taxIdNumber
            currencyId = coreOrg.currencyId
        }.also {
            logger.debug("Converted to BlockchainOrganisation: id={}", it.id)
        }
    }

    fun convertToCoreOrganisation(blockchainOrg: BlockchainOrganisation): CoreOrganisation {
        logger.debug("Converting BlockchainOrganisation to CoreOrganisation: id={}", blockchainOrg.id)
        return CoreOrganisation().apply {
            id = blockchainOrg.id
            name = blockchainOrg.name
            countryCode = blockchainOrg.countryCode
            taxIdNumber = blockchainOrg.taxIdNumber
            currencyId = blockchainOrg.currencyId
        }.also {
            logger.debug("Converted to CoreOrganisation: id={}", it.id)
        }
    }
}