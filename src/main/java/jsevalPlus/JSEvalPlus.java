package jsevalPlus;

import arc.util.*;
import arc.files.Fi;
import arc.struct.Seq;
import mindustry.gen.*;
import mindustry.mod.*;
import java.util.regex.*;
import mindustry.Vars;
import static mindustry.Vars.dataDirectory;
import static mindustry.Vars.mods;

@SuppressWarnings("unused")

public class JSEvalPlus extends Plugin {

    private class Script {
        public Pattern namePattern = Pattern.compile("(?<=//NAME \").*(?!\\n)");
        public Pattern descPattern = Pattern.compile("(?<=//DESCRIPTION \").*(?!\\n)");
        public String name, description, code;
        public Fi file;

        public Script(Fi file){
            this(file.nameWithoutExtension(), file);
        }

        public Script(String name, Fi file){
            this.file = file;
            this.code = file.readString();

            this.name = getName(code, name);
            this.description = getDesc(code, "");
        }

        public String run(){
            if(Vars.net.client()){
                if(Vars.player.admin){
                    Call.sendChatMessage("/js " + code);
                    return "executed";
                }
                return "[scarlet]Scripts disabled[]";
            }
            return mods.getScripts().runConsole(code);
        }

        public String getName(String input, String def){
            Matcher nameMatcher = namePattern.matcher(input);
            if(nameMatcher.find()){
                return nameMatcher.group(nameMatcher.groupCount());
            }else return def;
        }

        public String getDesc(String input, String def){
            Matcher descMatcher = descPattern.matcher(input);
            if(descMatcher.find()){
                return descMatcher.group(descMatcher.groupCount());
            }else return def;
        }
    }

    public static Fi scriptDirectory, setupDirectory;
    public static Fi[] scriptFiles, setupFiles;
    public static Seq<Script> scripts = new Seq<>();

    public void load() {
        scriptDirectory = dataDirectory.child("scripts/");
        scriptFiles = scriptDirectory.list(".js");

        if(scriptFiles.length == 0){
            Log.infoTag("JS-Loader", "No scripts found.");
        }else {
            Log.infoTag("JS-Loader", "Found " + scriptFiles.length + " scripts");

            for (Fi file : scriptFiles) {
                scripts.add(new Script(file));
            }
        }

    };


    @Override
    public void registerClientCommands(CommandHandler handler) {

        handler.<Player>register("js", "<code...>", "Execute JavaScript code.", (args, player) -> {

                 String output = mods.getScripts().runConsole(args[0]);
                 player.sendMessage("> " + (isError(output) ? "[#ff341c]" + output : output));
        });

        handler.<Player>register("load", "load JavaScript files.", (args, player) -> {
            load();
            for (int i = 0; i< scripts.size; i++) {
                player.sendMessage(">[#77dd77]found[white]:[#7955be] " + scripts.get(i).name + ".js");
                scripts.get(i).run();
            };
            player.sendMessage(">[#77dd77]" + scripts.size + " files loaded");
        });
    }

    private boolean isError(String output) {
        try {
            String errorName = output.substring(0, output.indexOf(' ') - 1);
            Class.forName("org.mozilla.javascript." + errorName);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}

