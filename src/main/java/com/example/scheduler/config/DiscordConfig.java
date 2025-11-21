package com.example.scheduler.config;

import com.example.scheduler.discord.DiscordBotListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.security.auth.login.LoginException;

@Configuration
public class DiscordConfig {

    @Value("${discord.bot.token}")
    private String discordBotToken;

    private final DiscordBotListener discordBotListener;

    public DiscordConfig(DiscordBotListener discordBotListener) {
        this.discordBotListener = discordBotListener;
    }

    @Bean
    public JDA jda() {
        if (discordBotToken == null || discordBotToken.isEmpty() || discordBotToken.equals("YOUR_BOT_TOKEN_HERE")) {
            System.out.println("Discord Bot Token is missing. Bot will not start.");
            return null;
        }

        try {
            return JDABuilder.createDefault(discordBotToken)
                    .setActivity(Activity.playing("GameSync"))
                    .addEventListeners(discordBotListener)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
