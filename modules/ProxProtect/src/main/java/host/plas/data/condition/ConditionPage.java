package host.plas.data.condition;

import host.plas.data.cause.HazardCause;
import lombok.Getter;
import lombok.Setter;

import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter @Setter
public class ConditionPage {
    private ConcurrentSkipListMap<Integer, String> lines;
    private String link;

    public ConditionPage(ConcurrentSkipListMap<Integer, String> lines, String link) {
        this.lines = lines;
        this.link = link;
    }

    public ConditionPage(String link) {
        this(new ConcurrentSkipListMap<>(), link);

        readFromLink();
    }

    public void readFromLink() {
        ConcurrentSkipListMap<Integer, String> l = new ConcurrentSkipListMap<>();

        try {
            URL url = new URL(link);
            try (Scanner s = new Scanner(url.openStream())) {
                while (s.hasNext()) {
                    String line = s.nextLine();

                    l.put(l.size(), line);
                }
            }
        } catch (Exception e) {
            // Do nothing.
        }
    }

    public boolean check(HazardCondition condition, HazardCause cause) {
        AtomicBoolean pass = new AtomicBoolean(false);

        lines.forEach((i, line) -> {
            if (pass.get()) return;
            pass.set(condition.checkHazard(cause, line, true));
        });

        return pass.get();
    }
}
