package com.ieum.dodam.welfare.domain.repository;
import com.ieum.dodam.welfare.domain.entity.ChildWelfareProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface ChildWelfareProfileRepository extends JpaRepository<ChildWelfareProfile, UUID> {
    Optional<ChildWelfareProfile> findByPatientId(UUID patientId);
    List<ChildWelfareProfile> findByRegionProvinceIsNull();
}
