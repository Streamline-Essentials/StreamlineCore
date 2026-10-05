package net.streamline.platform.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConnectionListener;
import net.streamline.platform.Messenger;
import net.streamline.platform.compat.McCompat;
import singularity.events.server.ping.PingReceivedEvent;
import singularity.objects.CosmicFavicon;
import singularity.objects.PingedResponse;
import singularity.utils.MessageUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fires {@link PingReceivedEvent} for server-list pings on mod loaders, none of which has a
 * ping event of its own, and sends the response modules leave behind.
 *
 * <p>The handler sits in each connection's Netty pipeline just before Minecraft's
 * {@link Connection}. It reads the hostname from the inbound handshake and replaces the
 * outbound {@link ClientboundStatusResponsePacket}. New connections are reached through
 * the listening channels: a handler ahead of Netty's acceptor marks every accepted
 * channel, and on that channel's first read — after Minecraft has built its pipeline —
 * this handler is inserted. Only Minecraft's own types are referenced, so every loader's
 * compiler checks them; the one reflective step finds the listening channels by type.
 *
 * <p>The rebuilt {@link ServerStatus} copies every record component it does not set, since
 * loaders add their own (Forge's mod data on 1.20.1, NeoForge's modded flag later), and
 * components are matched by type because their names are obfuscated on some loaders.
 * Fields a module leaves unchanged keep their original values, so passing a field through
 * does not lose formatting the {@code PingedResponse} form cannot express.
 *
 * <p>Any failure sends the original response unchanged.
 */
public final class StatusPingHandler extends ChannelDuplexHandler {
    private static final String ACCEPTOR_NAME = "streamline_status_acceptor";
    private static final String PROBE_NAME = "streamline_status_probe";
    private static final String HANDLER_NAME = "streamline_status";

    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static volatile CachedFavicon lastFavicon;

    private String hostName = "";

    private StatusPingHandler() {}

    /**
     * Hooks every channel the server listens on. Called once the server has started, when
     * those channels are bound.
     */
    public static void install(MinecraftServer server) {
        try {
            ServerConnectionListener listener = server.getConnection();
            List<ChannelFuture> channels = listener == null ? List.of() : listeningChannels(listener);
            if (channels.isEmpty()) {
                MessageUtils.logWarning("Found no listening channels; server-list pings will not fire PingReceivedEvent.");
                return;
            }
            for (ChannelFuture future : channels) {
                Channel channel = future.channel();
                if (channel.pipeline().get(ACCEPTOR_NAME) == null) {
                    channel.pipeline().addFirst(ACCEPTOR_NAME, Acceptor.INSTANCE);
                }
            }
        } catch (Throwable e) {
            MessageUtils.logWarning("Could not hook server-list pings; PingReceivedEvent will not fire.", e);
        }
    }

    /** The listener's bound channels: its private list of {@link ChannelFuture}s, found by element type. */
    @SuppressWarnings("unchecked")
    private static List<ChannelFuture> listeningChannels(ServerConnectionListener listener) throws IllegalAccessException {
        for (Field field : ServerConnectionListener.class.getDeclaredFields()) {
            if (! List.class.isAssignableFrom(field.getType())) continue;
            field.setAccessible(true);
            List<?> list = (List<?>) field.get(listener);
            if (list != null && ! list.isEmpty() && list.get(0) instanceof ChannelFuture) {
                return (List<ChannelFuture>) list;
            }
        }
        return List.of();
    }

    /** On a listening channel: marks each accepted connection before Netty initializes it. */
    @ChannelHandler.Sharable
    private static final class Acceptor extends ChannelInboundHandlerAdapter {
        static final Acceptor INSTANCE = new Acceptor();

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof Channel) {
                try {
                    ((Channel) msg).pipeline().addFirst(PROBE_NAME, new Probe());
                } catch (Throwable ignored) {
                    // The connection simply goes without ping handling.
                }
            }
            ctx.fireChannelRead(msg);
        }
    }

    /**
     * On an accepted connection: by its first read Minecraft has built the pipeline, so the
     * handler goes in front of {@link Connection} and the probe removes itself.
     */
    private static final class Probe extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            try {
                ChannelHandlerContext connection = ctx.pipeline().context(Connection.class);
                if (connection != null && ctx.pipeline().get(HANDLER_NAME) == null) {
                    ctx.pipeline().addBefore(connection.name(), HANDLER_NAME, new StatusPingHandler());
                }
            } catch (Throwable ignored) {
                // The connection simply goes without ping handling.
            }
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ClientIntentionPacket) {
            try {
                hostName = cleanHost(McCompat.handshakeHost((ClientIntentionPacket) msg));
            } catch (Throwable ignored) {
                hostName = "";
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ClientboundStatusResponsePacket) {
            Object out = msg;
            try {
                out = rewrite((ClientboundStatusResponsePacket) msg);
            } catch (Throwable e) {
                if (WARNED.compareAndSet(false, true)) {
                    MessageUtils.logWarning("Could not apply PingReceivedEvent to a server-list ping; sending the original response.", e);
                }
            }
            super.write(ctx, out, promise);
            return;
        }
        super.write(ctx, msg, promise);
    }

    /**
     * The handshake hostname without the markers some clients and proxies append after a
     * NUL (Forge/NeoForge clients' FML tag, BungeeCord's forwarded IP and UUID).
     */
    private static String cleanHost(String host) {
        if (host == null) return "";
        int nul = host.indexOf('\0');
        return nul < 0 ? host : host.substring(0, nul);
    }

    private ClientboundStatusResponsePacket rewrite(ClientboundStatusResponsePacket packet) throws ReflectiveOperationException {
        ServerStatus status = packet.status();
        PingedResponse original = toResponse(status);
        Snapshot before = new Snapshot(original);

        PingReceivedEvent event = new PingReceivedEvent(original, hostName).fire();
        if (event.isCancelled() || event.getResponse() == null) return packet;
        PingedResponse result = event.getResponse();

        Component description = Objects.equals(result.getDescription(), before.description)
                ? status.description()
                : Component.literal(coded(result.getDescription()));

        PingedResponse.Players players = result.getPlayers();
        Optional<ServerStatus.Players> statusPlayers = players == null || before.samePlayers(players)
                ? status.players()
                : Optional.of(McCompat.statusPlayers(players.getMax(), players.getOnline(),
                        players.getSample() == null ? List.of() : Arrays.asList(players.getSample())));

        PingedResponse.Protocol version = result.getVersion();
        Optional<ServerStatus.Version> statusVersion = version == null || version.getProtocol() == -1 || before.sameVersion(version)
                ? status.version()
                : Optional.of(new ServerStatus.Version(version.getName(), version.getProtocol()));

        CosmicFavicon favicon = result.getFavicon();
        Optional<ServerStatus.Favicon> statusFavicon = favicon == before.favicon
                ? status.favicon()
                : favicon == null ? Optional.empty() : Optional.of(new ServerStatus.Favicon(favicon.toPng()));

        return new ClientboundStatusResponsePacket(StatusRecord.copy(status, description, statusPlayers, statusVersion, statusFavicon));
    }

    private static PingedResponse toResponse(ServerStatus status) {
        PingedResponse.Protocol version = status.version()
                .map(v -> new PingedResponse.Protocol(v.name(), v.protocol()))
                .orElse(new PingedResponse.Protocol("", -1));
        PingedResponse.Players players = status.players()
                .map(p -> new PingedResponse.Players(p.max(), p.online(), McCompat.readSample(p).toArray(new PingedResponse.PlayerInfo[0])))
                .orElse(new PingedResponse.Players(0, 0, new PingedResponse.PlayerInfo[0]));
        CosmicFavicon favicon = status.favicon().map(f -> favicon(f.iconBytes())).orElse(null);
        return new PingedResponse(version, players, status.description().getString(), favicon);
    }

    /**
     * The favicon for the server's icon bytes. The server reuses one byte array for as long
     * as its icon is unchanged, so the decoded image is cached against it.
     */
    private static CosmicFavicon favicon(byte[] png) {
        CachedFavicon cached = lastFavicon;
        if (cached != null && cached.png == png) return cached.favicon;
        CosmicFavicon favicon = CosmicFavicon.fromPng(png);
        lastFavicon = new CachedFavicon(png, favicon);
        return favicon;
    }

    private static String coded(String text) {
        if (text == null) return "";
        Messenger messenger = Messenger.getInstance();
        return messenger == null ? text : messenger.codedString(text);
    }

    private static final class CachedFavicon {
        final byte[] png;
        final CosmicFavicon favicon;

        CachedFavicon(byte[] png, CosmicFavicon favicon) {
            this.png = png;
            this.favicon = favicon;
        }
    }

    /** The values the event started with, to tell which fields a module changed. */
    private static final class Snapshot {
        final String description;
        final int max;
        final int online;
        final PingedResponse.PlayerInfo[] sample;
        final String versionName;
        final int protocol;
        final CosmicFavicon favicon;

        Snapshot(PingedResponse response) {
            description = response.getDescription();
            max = response.getPlayers().getMax();
            online = response.getPlayers().getOnline();
            sample = response.getPlayers().getSample().clone();
            versionName = response.getVersion().getName();
            protocol = response.getVersion().getProtocol();
            favicon = response.getFavicon();
        }

        boolean samePlayers(PingedResponse.Players players) {
            return players.getMax() == max && players.getOnline() == online && Arrays.equals(players.getSample(), sample);
        }

        boolean sameVersion(PingedResponse.Protocol version) {
            return version.getProtocol() == protocol && Objects.equals(version.getName(), versionName);
        }
    }

    /** Rebuilds {@link ServerStatus} through its canonical constructor, whatever components it has. */
    private static final class StatusRecord {
        private static final RecordComponent[] COMPONENTS = ServerStatus.class.getRecordComponents();
        private static final Constructor<ServerStatus> CANONICAL = canonical();

        private static Constructor<ServerStatus> canonical() {
            Class<?>[] types = new Class<?>[COMPONENTS.length];
            for (int i = 0; i < COMPONENTS.length; i++) types[i] = COMPONENTS[i].getType();
            try {
                return ServerStatus.class.getDeclaredConstructor(types);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("ServerStatus has no canonical constructor", e);
            }
        }

        static ServerStatus copy(ServerStatus status, Component description, Optional<ServerStatus.Players> players,
                                 Optional<ServerStatus.Version> version, Optional<ServerStatus.Favicon> favicon)
                throws ReflectiveOperationException {
            Object[] args = new Object[COMPONENTS.length];
            for (int i = 0; i < COMPONENTS.length; i++) {
                RecordComponent component = COMPONENTS[i];
                Object value = component.getAccessor().invoke(status);
                if (component.getType() == Component.class) {
                    value = description;
                } else if (component.getType() == Optional.class) {
                    Type element = optionalElement(component);
                    if (element == ServerStatus.Players.class) value = players;
                    else if (element == ServerStatus.Version.class) value = version;
                    else if (element == ServerStatus.Favicon.class) value = favicon;
                }
                args[i] = value;
            }
            return CANONICAL.newInstance(args);
        }

        private static Type optionalElement(RecordComponent component) {
            Type type = component.getGenericType();
            return type instanceof ParameterizedType ? ((ParameterizedType) type).getActualTypeArguments()[0] : null;
        }
    }
}
