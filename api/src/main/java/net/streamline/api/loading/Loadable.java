package net.streamline.api.loading;

import tv.quaint.objects.Identifiable;

public interface Loadable extends Identifiable {
    void save();
}
