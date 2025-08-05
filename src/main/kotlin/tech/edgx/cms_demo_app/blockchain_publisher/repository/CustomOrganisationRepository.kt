package tech.edgx.cms_demo_app.blockchain_publisher.repository

import org.cardanofoundation.lob.app.organisation.domain.entity.Organisation
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CustomOrganisationRepository : JpaRepository<Organisation, String> {
    fun findByName(name: String): Optional<Organisation>
}