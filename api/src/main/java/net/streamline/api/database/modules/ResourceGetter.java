package net.streamline.api.database.modules;

import java.util.function.Supplier;

public interface ResourceGetter<T> extends Supplier<T> {}