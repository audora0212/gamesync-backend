package com.example.scheduler.controller;

import com.example.scheduler.dto.NoticeDto;
import com.example.scheduler.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.example.scheduler.security.AdminGuard;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final AdminGuard adminGuard;

    @GetMapping("/notices")
    public ResponseEntity<List<NoticeDto.Summary>> list() {
        return ResponseEntity.ok(noticeService.list());
    }

    @GetMapping("/notices/{id}")
    public ResponseEntity<NoticeDto.Detail> detail(@PathVariable Long id) {
        return ResponseEntity.ok(noticeService.detail(id));
    }

    // 관리자 전용 CRUD (간단 권한 가드)
    @PostMapping("/admin/notices")
    public ResponseEntity<NoticeDto.Detail> create(@Valid @RequestBody NoticeDto.Upsert req, org.springframework.security.core.Authentication auth) {
        if (auth == null || !adminGuard.isAdmin(auth.getName())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(noticeService.create(req));
    }

    @PutMapping("/admin/notices/{id}")
    public ResponseEntity<NoticeDto.Detail> update(@PathVariable Long id, @Valid @RequestBody NoticeDto.Upsert req, org.springframework.security.core.Authentication auth) {
        if (auth == null || !adminGuard.isAdmin(auth.getName())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(noticeService.update(id, req));
    }

    @DeleteMapping("/admin/notices/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, org.springframework.security.core.Authentication auth) {
        if (auth == null || !adminGuard.isAdmin(auth.getName())) {
            return ResponseEntity.status(403).build();
        }
        noticeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


