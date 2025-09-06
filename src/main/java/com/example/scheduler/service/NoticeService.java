package com.example.scheduler.service;

import com.example.scheduler.domain.Notice;
import com.example.scheduler.domain.User;
import com.example.scheduler.dto.NoticeDto;
import com.example.scheduler.repository.NoticeRepository;
import com.example.scheduler.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {
    private final NoticeRepository noticeRepo;
    private final UserRepository userRepo;

    public List<NoticeDto.Summary> list() {
        return noticeRepo.findAll().stream()
                .sorted((a,b)->b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(n -> new NoticeDto.Summary(n.getId(), n.getTitle(), n.getCreatedAt(), n.getUpdatedAt()))
                .collect(Collectors.toList());
    }

    public NoticeDto.Detail detail(Long id) {
        Notice n = noticeRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new NoticeDto.Detail(n.getId(), n.getTitle(), n.getContent(), n.getAuthor()!=null?n.getAuthor().getNickname():"-", n.getCreatedAt(), n.getUpdatedAt());
    }

    public NoticeDto.Detail create(NoticeDto.Upsert req) {
        User me = currentUser();
        LocalDateTime now = LocalDateTime.now();
        Notice saved = noticeRepo.save(Notice.builder()
                .title(req.getTitle())
                .content(req.getContent())
                .author(me)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return detail(saved.getId());
    }

    public NoticeDto.Detail update(Long id, NoticeDto.Upsert req) {
        Notice n = noticeRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        n.setTitle(req.getTitle());
        n.setContent(req.getContent());
        n.setUpdatedAt(LocalDateTime.now());
        noticeRepo.save(n);
        return detail(id);
    }

    public void delete(Long id) {
        noticeRepo.deleteById(id);
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}


