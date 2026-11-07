package com.ieum.dodam.health_record.tag;
import com.ieum.dodam.user.query.application.UserQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.*;
@RestController @RequestMapping("/api/memo-tags")
public class MemoTagController {
 private final MemoTagService tags;private final UserQueryService users;
 public MemoTagController(MemoTagService tags,UserQueryService users){this.tags=tags;this.users=users;}
 public record Write(@NotNull UUID patientId,@NotBlank @Size(max=20) String name,Long version){}
 @GetMapping public List<MemoTag> list(@AuthenticationPrincipal OAuth2User p,@RequestParam UUID patientId){return tags.list(users.requireByKakaoId(String.valueOf((Object)p.getAttribute("id"))),patientId);}
 @PostMapping public MemoTag create(@AuthenticationPrincipal OAuth2User p,@Valid @RequestBody Write r){return tags.save(users.requireByKakaoId(String.valueOf((Object)p.getAttribute("id"))),null,r.patientId(),r.name(),null);}
 @PutMapping("/{id}") public MemoTag update(@AuthenticationPrincipal OAuth2User p,@PathVariable UUID id,@Valid @RequestBody Write r){return tags.save(users.requireByKakaoId(String.valueOf((Object)p.getAttribute("id"))),id,r.patientId(),r.name(),r.version());}
 @DeleteMapping("/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void delete(@AuthenticationPrincipal OAuth2User p,@PathVariable UUID id,@RequestParam long version){tags.delete(users.requireByKakaoId(String.valueOf((Object)p.getAttribute("id"))),id,version);}
}
