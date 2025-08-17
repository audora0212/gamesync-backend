package com.example.scheduler.service;

import com.example.scheduler.domain.*;
import com.example.scheduler.dto.PartyDto;
import com.example.scheduler.dto.TimetableDto;
import com.example.scheduler.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartyService {
    private final PartyRepository partyRepo;
    private final ServerRepository serverRepo;
    private final UserRepository userRepo;
    private final DefaultGameRepository defaultGameRepo;
    private final CustomGameRepository customGameRepo;
    private final TimetableService timetableService;

    private User currentUser() {
        return userRepo.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public PartyDto.Response create(PartyDto.CreateRequest req) {
        User user = currentUser();
        Server server = serverRepo.findById(req.getServerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Party p = Party.builder()
                .server(server)
                .creator(user)
                .slot(req.getSlot().truncatedTo(ChronoUnit.MINUTES))
                .capacity(Math.max(1, req.getCapacity()))
                .createdAt(LocalDateTime.now())
                .build();

        if (req.getCustomGameId() != null) {
            CustomGame cg = customGameRepo.findById(req.getCustomGameId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid customGameId"));
            p.setCustomGame(cg);
        } else if (req.getDefaultGameId() != null) {
            DefaultGame dg = defaultGameRepo.findById(req.getDefaultGameId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid defaultGameId"));
            p.setDefaultGame(dg);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game ID is required");
        }

        // 생성자는 자동 참가
        Party saved = partyRepo.save(p);
        saved.getParticipants().add(user);

        // 생성자의 타임테이블에도 등록
        TimetableDto.EntryRequest tReq = new TimetableDto.EntryRequest();
        tReq.setServerId(saved.getServer().getId());
        tReq.setSlot(saved.getSlot());
        if (saved.getCustomGame() != null) {
            tReq.setCustomGameId(saved.getCustomGame().getId());
        } else if (saved.getDefaultGame() != null) {
            tReq.setDefaultGameId(saved.getDefaultGame().getId());
        }
        timetableService.add(tReq);

        return toResp(saved);
    }

    public List<PartyDto.Response> list(Long serverId) {
        Server server = serverRepo.findById(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return partyRepo.findByServerOrderBySlotAsc(server).stream()
                .map(this::toResp)
                .toList();
    }

    @Transactional
    public PartyDto.Response join(Long partyId) {
        User user = currentUser();
        Party party = partyRepo.findById(partyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (party.getParticipants().contains(user)) {
            return toResp(party);
        }
        if (party.getParticipants().size() >= party.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Party is full");
        }

        // 타임테이블에도 등록
        TimetableDto.EntryRequest req = new TimetableDto.EntryRequest();
        req.setServerId(party.getServer().getId());
        req.setSlot(party.getSlot());
        if (party.getCustomGame() != null) {
            req.setCustomGameId(party.getCustomGame().getId());
        } else if (party.getDefaultGame() != null) {
            req.setDefaultGameId(party.getDefaultGame().getId());
        }
        timetableService.add(req);

        party.getParticipants().add(user);
        return toResp(party);
    }

    private PartyDto.Response toResp(Party party) {
        PartyDto.Response r = new PartyDto.Response();
        r.setId(party.getId());
        r.setServerId(party.getServer().getId());
        r.setCreator(party.getCreator().getNickname());
        r.setSlot(party.getSlot());
        if (party.getCustomGame() != null) {
            r.setGameId(party.getCustomGame().getId());
            r.setGameName(party.getCustomGame().getName());
            r.setCustom(true);
        } else {
            r.setGameId(party.getDefaultGame().getId());
            r.setGameName(party.getDefaultGame().getName());
            r.setCustom(false);
        }
        r.setCapacity(party.getCapacity());
        int participants = party.getParticipants() == null ? 0 : party.getParticipants().size();
        r.setParticipants(participants);
        r.setFull(participants >= party.getCapacity());
        Set<String> names = party.getParticipants() == null ? Set.of() : party.getParticipants().stream()
                .map(User::getNickname)
                .collect(Collectors.toSet());
        r.setParticipantNames(names);
        return r;
    }
}


