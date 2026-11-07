package com.ieum.dodam.health_record.tag;
import com.ieum.dodam.health_record.domain.HealthRecordAccessPolicy;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.global.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
@Service @Transactional
public class MemoTagService {
 private final MemoTagRepository tags; private final HealthRecordAccessPolicy access; private final JdbcTemplate jdbc;
 public MemoTagService(MemoTagRepository tags,HealthRecordAccessPolicy access,JdbcTemplate jdbc){this.tags=tags;this.access=access;this.jdbc=jdbc;}
 @Transactional(readOnly=true) public List<MemoTag> list(User actor,UUID patientId){access.requirePatient(actor,patientId);return tags.findByPatientIdAndDeletedAtIsNullOrderByNameAsc(patientId);}
 public MemoTag save(User actor,UUID id,UUID patientId,String name,Long version){
  access.requirePatient(actor,patientId); lock(patientId);
  String value=name==null?"":name.strip();
  if(value.isBlank() || value.length()>20)throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
  MemoTag tag=id==null?MemoTag.create(patientId,value):require(id);
  if(!tag.getPatientId().equals(patientId))throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
  if(id!=null && (version==null || version!=tag.getVersion()))throw new BusinessException(GlobalErrorCode.CONFLICT);
  if(tags.existsByPatientIdAndNameAndDeletedAtIsNullAndIdNot(patientId,value,tag.getId()))throw new BusinessException(GlobalErrorCode.CONFLICT);
  tag.rename(value); return tags.saveAndFlush(tag);
 }
 public void delete(User actor,UUID id,long version){
  var tag=require(id);access.requirePatient(actor,tag.getPatientId());lock(tag.getPatientId());
  if(tag.getVersion()!=version)throw new BusinessException(GlobalErrorCode.CONFLICT);
  jdbc.update("delete from health_record_tags where tag_id=?",id); tag.delete();tags.flush();
 }
 private MemoTag require(UUID id){return tags.findByIdAndDeletedAtIsNull(id).orElseThrow(()->new BusinessException(GlobalErrorCode.INVALID_REQUEST));}
 private void lock(UUID id){jdbc.queryForObject("select id from patients where id=? for update",UUID.class,id);}
}
