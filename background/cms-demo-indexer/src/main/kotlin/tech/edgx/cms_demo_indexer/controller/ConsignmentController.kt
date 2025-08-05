package tech.edgx.cms_demo_indexer.controller

import org.springframework.data.domain.Limit
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import tech.edgx.cms_demo_indexer.domain.entity.ConsignmentEntity
import tech.edgx.cms_demo_indexer.repository.ConsignmentRepository

@RestController
@RequestMapping("/api/v1/consignments")
class ConsignmentController(
    private val consignmentRepository: ConsignmentRepository
) {
    @GetMapping
    fun getConsignments(@RequestParam(defaultValue = "1000") limit: Int): ResponseEntity<List<ConsignmentEntity>> {
        val consignments = consignmentRepository.findAllByOrderByL1AbsoluteSlotAsc(Limit.of(limit))
        return ResponseEntity.ok(consignments)
    }
}