package com.ieum.dodam.notification.push;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PushDeviceRepository extends JpaRepository<PushDevice,String> {
    List<PushDevice> findByUserIdAndActiveTrue(UUID userId);
}
