package net.streamline.api.command;

import net.streamline.api.modules.Module;
import net.streamline.utils.MessagingUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModuleCommandYamlParser {
    @NotNull
    public static List<Command> parse(@NotNull Module module) {
        List<Command> moduleCmds = new ArrayList<Command>();

        Map<String, Map<String, Object>> map = module.getDescription().getCommands();

        if (map == null) {
            return moduleCmds;
        }

        for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
            if (entry.getKey().contains(":")) {
                MessagingUtils.logSevere("Could not load command " + entry.getKey() + " for module " + module.getName() + ": Illegal Characters");
                continue;
            }
            Object description = entry.getValue().get("description");
            Object usage = entry.getValue().get("usage");
            Object aliases = entry.getValue().get("aliases");
            Object permission = entry.getValue().get("permission");
            Object permissionMessage = entry.getValue().get("permission-message");
            Command newCmd = new ModuleCommand(module, entry.getKey(), description, usage, permission, aliases);

            if (description != null) {
                newCmd.setDescription(description.toString());
            }

            if (usage != null) {
                newCmd.setUsage(usage.toString());
            }

            if (aliases != null) {
                List<String> aliasList = new ArrayList<String>();

                if (aliases instanceof List) {
                    for (Object o : (List<?>) aliases) {
                        if (o.toString().contains(":")) {
                            MessagingUtils.logSevere("Could not load alias " + o.toString() + " for module " + module.getName() + ": Illegal Characters");
                            continue;
                        }
                        aliasList.add(o.toString());
                    }
                } else {
                    if (aliases.toString().contains(":")) {
                        MessagingUtils.logSevere("Could not load alias " + aliases.toString() + " for module " + module.getName() + ": Illegal Characters");
                    } else {
                        aliasList.add(aliases.toString());
                    }
                }

                newCmd.setAliases(aliasList);
            }

            if (permission != null) {
                newCmd.setPermission(permission.toString());
            }

            if (permissionMessage != null) {
                newCmd.setPermissionMessage(permissionMessage.toString());
            }

            moduleCmds.add(newCmd);
        }
        return moduleCmds;
    }
}
