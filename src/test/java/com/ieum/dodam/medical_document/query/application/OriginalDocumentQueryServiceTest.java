package com.ieum.dodam.medical_document.query.application;
import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.global.status.DocumentStatus;
import com.ieum.dodam.medical_document.domain.*;
import com.ieum.dodam.medical_document.domain.entity.MedicalDocument;
import com.ieum.dodam.medical_document.domain.repository.MedicalDocumentRepository;
import com.ieum.dodam.user.domain.User;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class OriginalDocumentQueryServiceTest {
 final MedicalDocumentRepository docs=mock(MedicalDocumentRepository.class);
 final MedicalDocumentAccessPolicy access=mock(MedicalDocumentAccessPolicy.class);
 final OriginalDocumentStoragePort storage=mock(OriginalDocumentStoragePort.class);
 final OriginalDocumentQueryService service=new OriginalDocumentQueryService(docs,access,storage);
 final UUID id=UUID.randomUUID(), child=UUID.randomUUID();
 final User user=mock(User.class);
 void document(boolean allowed) {
  var d=mock(MedicalDocument.class);
  when(docs.findByIdAndStatusNot(id,DocumentStatus.DELETED)).thenReturn(Optional.of(d));
  when(d.getPatientId()).thenReturn(child);when(d.getStorageKey()).thenReturn("documents/test/original.pdf");
  when(d.getMimeType()).thenReturn("application/pdf");when(d.getFileName()).thenReturn("검진.pdf");
  when(access.canRead(user,child)).thenReturn(allowed);
 }
 @Test void authorizedGuardianReadsBytes(){document(true);when(storage.readOriginal(anyString())).thenReturn(new byte[]{1,2});assertThat(service.get(user,id).content()).containsExactly(1,2);}
 @Test void unrelatedGuardianCannotReadStorage(){document(false);assertThatThrownBy(()->service.get(user,id)).isInstanceOf(BusinessException.class);verifyNoInteractions(storage);}
 @Test void deletedOrMissingDocumentCannotReadStorage(){when(docs.findByIdAndStatusNot(id,DocumentStatus.DELETED)).thenReturn(Optional.empty());assertThatThrownBy(()->service.get(user,id)).isInstanceOf(BusinessException.class);verifyNoInteractions(storage);}
 @Test void missingFileReturnsControlledError(){document(true);when(storage.readOriginal(anyString())).thenThrow(new DocumentStorageException("missing"));assertThatThrownBy(()->service.get(user,id)).isInstanceOf(BusinessException.class);}
}
