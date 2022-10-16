package net.streamline.api.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.modules.StreamlineSpringModule;
import net.streamline.api.utils.MessageUtils;
import org.pf4j.ExtensionPoint;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * Simple interface for tagging all EventListeners
 */
public interface StreamlineListener {
}
