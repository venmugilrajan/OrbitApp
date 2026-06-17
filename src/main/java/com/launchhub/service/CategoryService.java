package com.launchhub.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryService {

    private static final Map<String, List<String>> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("Development", List.of(
            "vscode", "code", "studio", "intellij", "eclipse", "netbeans", "git", "github", 
            "python", "node", "java", "compiler", "docker", "postman", "sublime", "webstorm", 
            "pycharm", "dbeaver", "android", "sdk", "unity", "unreal", "blender", "qt", "mingw"
        ));
        KEYWORDS.put("Browsers", List.of(
            "chrome", "firefox", "edge", "opera", "safari", "brave", "vivaldi", "chromium", "explorer"
        ));
        KEYWORDS.put("Productivity", List.of(
            "office", "word", "excel", "powerpoint", "notion", "adobe", "acrobat", "pdf", 
            "evernote", "oneNote", "figma", "obsidian", "slack", "zoom", "teams", "skype", 
            "wordpad", "onenote", "trello", "jira", "outlook", "thunderbird"
        ));
        KEYWORDS.put("Media", List.of(
            "vlc", "spotify", "itunes", "winamp", "audacity", "handbrake", "obs", "premiere", 
            "photoshop", "illustrator", "lightroom", "gimp", "inkscape", "netflix", "youtube", 
            "plex", "kodi", "quicktime", "potplayer"
        ));
        KEYWORDS.put("Gaming", List.of(
            "steam", "epic", "gog", "uplay", "origin", "riot", "valorant", "league", 
            "minecraft", "discord", "battlenet", "blizzard", "ea", "xbox", "geforce", "retroarch"
        ));
        KEYWORDS.put("Communication", List.of(
            "discord", "slack", "zoom", "teams", "skype", "whatsapp", "telegram", "viber", 
            "signal", "messenger", "webex"
        ));
        KEYWORDS.put("Utilities", List.of(
            "winrar", "7-zip", "ccleaner", "notepad++", "notepad", "windirstat", "rufus", 
            "putty", "filezilla", "anydesk", "teamviewer", "winscp", "utorrent", "bittorrent", 
            "defender", "antivirus", "malwarebytes"
        ));
        KEYWORDS.put("Education", List.of(
            "matlab", "geogebra", "duolingo", "anki", "wikipedia", "encarta", "coursera", "edx"
        ));
    }

    public static String classify(String appName, String publisher, String installLocation) {
        if (appName == null) return "Others";
        
        String cleanName = appName.toLowerCase();
        String cleanPub = publisher != null ? publisher.toLowerCase() : "";
        String cleanPath = installLocation != null ? installLocation.toLowerCase() : "";

        // Special priority: if it's discord/slack/teams, put it under Communication/Productivity
        for (Map.Entry<String, List<String>> entry : KEYWORDS.entrySet()) {
            String category = entry.getKey();
            for (String keyword : entry.getValue()) {
                if (cleanName.contains(keyword) || cleanPub.contains(keyword) || cleanPath.contains(keyword)) {
                    return category;
                }
            }
        }
        return "Others";
    }
}
