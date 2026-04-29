package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.event.Event;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EventMessageBuilder {

    public String buildEventReminder(String userName, String eventTitle, LocalDateTime date) {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

        String formattedDate = (date != null)
                ? date.format(formatter)
                : "date inconnue";

        return "📢 Rappel événement\n\n"
                + "Bonjour " + userName + ",\n"
                + "Votre événement \"" + eventTitle + "\" est prévu le " + formattedDate + ".\n\n"
                + "Merci et à bientôt 👍";
    }
}