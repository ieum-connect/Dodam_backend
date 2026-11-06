package com.ieum.dodam.action_item.domain.repository;

import com.ieum.dodam.action_item.domain.entity.ActionItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ActionItemRepository extends JpaRepository<ActionItem, UUID> {

    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ActionItem a where a.id = :id")
    java.util.Optional<ActionItem> lockById(@Param("id") UUID id);

    @Query(
            "select a from ActionItem a, Visit v, MedicalDocument d where a.visitId = v.id and"
                + " a.documentId = d.id and d.visitId = v.id and v.patientId = :patientId and"
                + " d.deletedAt is null and v.deletedAt is null order by a.createdAt desc")
    java.util.List<ActionItem> findForPatient(@Param("patientId") UUID patientId);

    /** 문서에서 나온 PENDING 후보를 모두 CANCELED로 바꾸고 변경된 행 수를 반환한다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            """
update ActionItem a
   set a.approvalStatus = com.ieum.dodam.action_item.domain.ActionItemApprovalStatus.CANCELED,
       a.cancelReason = :cancelReason
 where a.documentId = :documentId
   and a.approvalStatus = com.ieum.dodam.action_item.domain.ActionItemApprovalStatus.PENDING
""")
    int cancelPendingByDocumentId(
            @Param("documentId") UUID documentId, @Param("cancelReason") String cancelReason);
}
