package com.ieum.dodam.notification.push;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
public interface PushDeliveryRepository extends JpaRepository<PushDelivery,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from PushDelivery d where d.status='PENDING' and d.nextAttemptAt<=:now order by d.nextAttemptAt,d.id")
    List<PushDelivery> lockDue(@Param("now") Instant now, Pageable page);
}
