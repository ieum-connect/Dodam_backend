package com.ieum.dodam.health_record.tag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MemoTagRepository extends JpaRepository<MemoTag,UUID> {
 List<MemoTag> findByPatientIdAndDeletedAtIsNullOrderByNameAsc(UUID patientId);
 Optional<MemoTag> findByIdAndDeletedAtIsNull(UUID id);
 boolean existsByPatientIdAndNameAndDeletedAtIsNullAndIdNot(UUID patientId,String name,UUID id);
}
