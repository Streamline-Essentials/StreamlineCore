package host.plas.executables;

import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;

import java.util.concurrent.atomic.AtomicInteger;

@Setter
@Getter
public class ExecutableUser<T> {
    private T user;

    public ExecutableUser(T user) {
        this.user = user;
    }

    private static final int SUCCESS = 1;
    private static final int FAIL = -1;

    public int runCommand(String command) {
        return runCommand(command, user);
    }

    public static <T> int runCommand(String command, T user) {
        if (user instanceof CosmicSender) {
            CosmicSender sender = (CosmicSender) user;
            if (sender.isConsole()) {
                try {
                    ModuleUtils.getConsole().runCommand(command);
                    return SUCCESS;
                } catch (Exception e) {
                    e.printStackTrace();
                    return FAIL;
                }
            }

            if (sender instanceof CosmicPlayer) {
                CosmicSender player = (CosmicPlayer) user;

                try {
                    player.runCommand(command);
                    return SUCCESS;
                } catch (Exception e) {
                    e.printStackTrace();
                    return FAIL;
                }
            }
        }
//        if (user instanceof OperatorUser) {
//            OperatorUser operatorUser = (OperatorUser) user;
//            return ModuleUtils.runAs(operatorUser, command) ? SUCCESS : FAIL;
//        }
        if (user instanceof MultipleUser) {
            MultipleUser multipleUser = (MultipleUser) user;
            AtomicInteger c = new AtomicInteger();
            multipleUser.getUsers().forEach(a -> {
                if (runCommand(command, a) == SUCCESS) {
                    c.getAndIncrement();
                }
            });
            return c.get();
        }
        return FAIL;
    }
}
