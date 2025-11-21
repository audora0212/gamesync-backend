package com.example.scheduler.discord;

import com.example.scheduler.domain.Party;
import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.TimetableEntry;
import com.example.scheduler.domain.User;
import com.example.scheduler.repository.PartyRepository;
import com.example.scheduler.repository.ServerRepository;
import com.example.scheduler.repository.TimetableEntryRepository;
import com.example.scheduler.repository.UserRepository;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Component
public class DiscordBotListener extends ListenerAdapter {

    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final PartyRepository partyRepository;
    private final TimetableEntryRepository timetableEntryRepository;

    public DiscordBotListener(UserRepository userRepository,
                              ServerRepository serverRepository,
                              PartyRepository partyRepository,
                              TimetableEntryRepository timetableEntryRepository) {
        this.userRepository = userRepository;
        this.serverRepository = serverRepository;
        this.partyRepository = partyRepository;
        this.timetableEntryRepository = timetableEntryRepository;
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        // Guild commands are registered instantly (no 1-hour delay)
        event.getGuild().updateCommands()
                .addCommands(
                        Commands.slash("ping", "Check bot latency and user status"),
                        Commands.slash("파티모집", "새로운 내전/파티 일정을 생성합니다")
                                .addOption(OptionType.STRING, "시간", "시작 시간 (예: 21:00 또는 2024-01-15 21:00)", true)
                                .addOption(OptionType.INTEGER, "인원", "최대 참여 인원 (기본: 10)", false),
                        Commands.slash("참여", "일정에 참여합니다")
                                .addOption(OptionType.INTEGER, "일정번호", "참여할 일정 번호", true),
                        Commands.slash("일정목록", "현재 예약된 일정을 확인합니다"),
                        Commands.slash("서버연결", "이 디스코드 서버를 GameSync 서버와 연결합니다")
                                .addOption(OptionType.STRING, "초대코드", "GameSync 서버 초대 코드", true)
                )
                .queue();
    }

    @Override
    @Transactional
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();

        switch (command) {
            case "ping" -> handlePing(event);
            case "파티모집" -> handleCreateParty(event);
            case "참여" -> handleJoinParty(event);
            case "일정목록" -> handleListParties(event);
            case "서버연결" -> handleLinkServer(event);
        }
    }

    private void handlePing(SlashCommandInteractionEvent event) {
        String discordId = event.getUser().getId();
        Optional<User> userOpt = userRepository.findByDiscordId(discordId);

        if (userOpt.isPresent()) {
            event.reply("Pong! Hello, " + userOpt.get().getNickname() + "!").queue();
        } else {
            event.reply("Pong! I don't know you yet. Please log in to the website with Discord to link your account.").queue();
        }
    }

    private void handleCreateParty(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("이 명령어는 서버에서만 사용할 수 있습니다.").setEphemeral(true).queue();
            return;
        }

        String discordId = event.getUser().getId();
        Optional<User> userOpt = userRepository.findByDiscordId(discordId);

        if (userOpt.isEmpty()) {
            event.reply("먼저 웹사이트에서 Discord 로그인을 해주세요.").setEphemeral(true).queue();
            return;
        }

        String guildId = event.getGuild().getId();
        Optional<Server> serverOpt = serverRepository.findByDiscordGuildId(guildId);

        if (serverOpt.isEmpty()) {
            event.reply("이 디스코드 서버가 GameSync 서버와 연결되어 있지 않습니다.\n`/서버연결 <초대코드>` 명령어로 먼저 연결해주세요.").setEphemeral(true).queue();
            return;
        }

        User user = userOpt.get();
        Server server = serverOpt.get();

        String timeStr = event.getOption("시간").getAsString();
        int capacity = event.getOption("인원") != null ? event.getOption("인원").getAsInt() : 10;

        LocalDateTime slot;
        try {
            if (timeStr.contains("-")) {
                slot = LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } else {
                slot = LocalDateTime.now().withHour(Integer.parseInt(timeStr.split(":")[0]))
                        .withMinute(Integer.parseInt(timeStr.split(":")[1]))
                        .withSecond(0).withNano(0);
                if (slot.isBefore(LocalDateTime.now())) {
                    slot = slot.plusDays(1);
                }
            }
        } catch (DateTimeParseException | NumberFormatException e) {
            event.reply("시간 형식이 올바르지 않습니다. 예: `21:00` 또는 `2024-01-15 21:00`").setEphemeral(true).queue();
            return;
        }

        // 서버 멤버가 아니면 자동 추가
        if (!server.getMembers().contains(user)) {
            server.getMembers().add(user);
            serverRepository.save(server);
        }

        Party party = Party.builder()
                .server(server)
                .creator(user)
                .slot(slot)
                .capacity(capacity)
                .createdAt(LocalDateTime.now())
                .build();
        party.getParticipants().add(user);
        partyRepository.save(party);

        // 타임테이블 엔트리 생성
        TimetableEntry entry = TimetableEntry.builder()
                .server(server)
                .user(user)
                .slot(slot)
                .build();
        timetableEntryRepository.save(entry);

        String formattedTime = slot.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
        event.reply("**파티가 생성되었습니다!**\n" +
                "일정 번호: `" + party.getId() + "`\n" +
                "시간: `" + formattedTime + "`\n" +
                "인원: `1/" + capacity + "`\n\n" +
                "`/참여 " + party.getId() + "` 명령어로 참여하세요!").queue();
    }

    private void handleJoinParty(SlashCommandInteractionEvent event) {
        String discordId = event.getUser().getId();
        Optional<User> userOpt = userRepository.findByDiscordId(discordId);

        if (userOpt.isEmpty()) {
            event.reply("먼저 웹사이트에서 Discord 로그인을 해주세요.").setEphemeral(true).queue();
            return;
        }

        long partyId = event.getOption("일정번호").getAsLong();
        Optional<Party> partyOpt = partyRepository.findById(partyId);

        if (partyOpt.isEmpty()) {
            event.reply("해당 일정을 찾을 수 없습니다.").setEphemeral(true).queue();
            return;
        }

        User user = userOpt.get();
        Party party = partyOpt.get();

        if (party.getParticipants().contains(user)) {
            event.reply("이미 참여 중인 일정입니다.").setEphemeral(true).queue();
            return;
        }

        if (party.getParticipants().size() >= party.getCapacity()) {
            event.reply("인원이 가득 찼습니다.").setEphemeral(true).queue();
            return;
        }

        // 서버 멤버가 아니면 자동 추가
        Server server = party.getServer();
        if (!server.getMembers().contains(user)) {
            server.getMembers().add(user);
            serverRepository.save(server);
        }

        party.getParticipants().add(user);
        partyRepository.save(party);

        // 타임테이블 엔트리 생성
        TimetableEntry entry = TimetableEntry.builder()
                .server(server)
                .user(user)
                .slot(party.getSlot())
                .build();
        timetableEntryRepository.save(entry);

        String formattedTime = party.getSlot().format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
        event.reply("**참여 완료!**\n" +
                "일정: `" + formattedTime + "`\n" +
                "현재 인원: `" + party.getParticipants().size() + "/" + party.getCapacity() + "`").queue();
    }

    private void handleListParties(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("이 명령어는 서버에서만 사용할 수 있습니다.").setEphemeral(true).queue();
            return;
        }

        String guildId = event.getGuild().getId();
        Optional<Server> serverOpt = serverRepository.findByDiscordGuildId(guildId);

        if (serverOpt.isEmpty()) {
            event.reply("이 디스코드 서버가 GameSync 서버와 연결되어 있지 않습니다.").setEphemeral(true).queue();
            return;
        }

        Server server = serverOpt.get();
        List<Party> parties = partyRepository.findByServerOrderBySlotAsc(server);

        if (parties.isEmpty()) {
            event.reply("예약된 일정이 없습니다.\n`/파티모집` 명령어로 새 일정을 만들어보세요!").queue();
            return;
        }

        StringBuilder sb = new StringBuilder("**예약된 일정 목록**\n\n");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd HH:mm");

        for (Party party : parties) {
            sb.append("`#").append(party.getId()).append("` ")
                    .append(party.getSlot().format(fmt))
                    .append(" - ").append(party.getParticipants().size())
                    .append("/").append(party.getCapacity()).append("명")
                    .append(" (by ").append(party.getCreator().getNickname()).append(")\n");
        }

        event.reply(sb.toString()).queue();
    }

    private void handleLinkServer(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("이 명령어는 서버에서만 사용할 수 있습니다.").setEphemeral(true).queue();
            return;
        }

        String discordId = event.getUser().getId();
        Optional<User> userOpt = userRepository.findByDiscordId(discordId);

        if (userOpt.isEmpty()) {
            event.reply("먼저 웹사이트에서 Discord 로그인을 해주세요.").setEphemeral(true).queue();
            return;
        }

        String inviteCode = event.getOption("초대코드").getAsString();
        Optional<Server> serverOpt = serverRepository.findByInviteCode(inviteCode);

        if (serverOpt.isEmpty()) {
            event.reply("유효하지 않은 초대 코드입니다.").setEphemeral(true).queue();
            return;
        }

        Server server = serverOpt.get();
        User user = userOpt.get();

        if (!server.getOwner().equals(user) && !server.getAdmins().contains(user)) {
            event.reply("서버 연결은 서버장 또는 관리자만 할 수 있습니다.").setEphemeral(true).queue();
            return;
        }

        String guildId = event.getGuild().getId();

        Optional<Server> existingLink = serverRepository.findByDiscordGuildId(guildId);
        if (existingLink.isPresent()) {
            event.reply("이 디스코드 서버는 이미 `" + existingLink.get().getName() + "` 서버와 연결되어 있습니다.").setEphemeral(true).queue();
            return;
        }

        server.setDiscordGuildId(guildId);
        serverRepository.save(server);

        event.reply("**서버 연결 완료!**\n" +
                "디스코드 서버 `" + event.getGuild().getName() + "`이(가)\n" +
                "GameSync 서버 `" + server.getName() + "`와 연결되었습니다.\n\n" +
                "이제 `/파티모집`, `/참여`, `/일정목록` 명령어를 사용할 수 있습니다!").queue();
    }
}
