package dev.kolustar;

import dev.kolustar.flight.AnomalyZone;
import dev.kolustar.flight.FlightPermit;
import dev.kolustar.flight.GroundFlightServer;
import dev.kolustar.ground.*;
import dev.kolustar.ground.GroundPackets;
import dev.kolustar.ground.GroundServer;
import dev.kolustar.ground.GroundTickets;
import dev.kolustar.ground.ImageStream;
import dev.kolustar.network.KolustarNetwork;
import dev.kolustar.planet.Anomaly;
import dev.kolustar.planet.PlanetCore;
import dev.kolustar.planet.PlanetLevels;
import dev.kolustar.planet.PlanetSystem;
import dev.kolustar.render.GroundViewServer;
import dev.kolustar.render.PlanetRemoteServer;
import dev.kolustar.ship.AircraftDock;
import dev.kolustar.ship.Airstair;
import dev.kolustar.ship.DrillClearance;
import dev.kolustar.ship.HullVehicles;
import dev.kolustar.ship.SableBoarding;
import dev.kolustar.ship.SableBoardingPacket;
import dev.kolustar.ship.SableFrames;
import dev.kolustar.ship.ShipRecords;
import dev.kolustar.ship.Thrusters;
import dev.kolustar.sky.GroundClock;
import dev.kolustar.sky.GroundDaylight;
import dev.kolustar.space.SpaceServer;
import dev.kolustar.space.SpaceDust;
import dev.kolustar.universe.PlanetChunkGenerator;
import dev.kolustar.universe.SystemChunkGenerator;
import dev.kolustar.universe.Universe;
import dev.kolustar.waypoint.ShipGuide;
import dev.kolustar.waypoint.WaypointServer;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/** Brings the server side of every subsystem up in a fixed order, and owns the per-tick order between them and lifecycle wiring. */
public final class KolustarServer {
    private KolustarServer() {}

    public static void initialize(net.neoforged.bus.api.IEventBus modBus) {
        KolustarHostBindings.install();
        initializeServerSystems(modBus);
        dev.kolustar.planet.PlanetFire.initialize();
        registerPlanetLifecycle();
        initializePlayerSystems();
        registerTicketsAndCleanup();
        registerSleeping();
        registerClockTicks();
        registerCommands();
        dev.kolustar.diag.TickWatchdog.initialize();
    }

    private static void initializeServerSystems(net.neoforged.bus.api.IEventBus modBus) {
        Universe.grown =
                (level, system, updateClock) -> {
                    GroundServer.system(system);
                    var clock = updateClock.get();
                    for (var player : level.players()) {
                        GroundServer.shapes(player);
                        GroundFlightServer.restart(player, clock);
                    }
                };
        if (net.neoforged.fml.ModList.get().isLoaded("create")
                && net.neoforged.fml.ModList.get().isLoaded("sable")) DrillClearance.initialize();
        if (net.neoforged.fml.ModList.get().isLoaded("sable")) HullVehicles.initializeServer();
        SableFrames.initialize();
        SpaceServer.initialize();
        KolustarNetwork.playS2C(SableBoardingPacket.TYPE, SableBoardingPacket.CODEC);
        SableBoarding.initialize();
        Thrusters.initialize(modBus);
        dev.kolustar.space.GasPlayerExposureServer.initialize();
        Airstair.initialize(modBus);
        AircraftDock.initialize(modBus);
        SpaceDust.initialize(modBus);
        PlanetChunkGenerator.initialize(modBus);
        Anomaly.initialize(modBus);
        PlanetCore.initialize(modBus);
        FlightPermit.initialize(modBus);
        KolustarNetwork.playS2C(GroundPackets.Sky.TYPE, GroundPackets.Sky.CODEC);
        KolustarNetwork.playS2C(GroundPackets.Shapes.TYPE, GroundPackets.Shapes.CODEC);
    }

    private static void registerPlanetLifecycle() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.level.LevelEvent.Unload event) -> {
                    if (event.getLevel() instanceof ServerLevel level) {
                        dev.kolustar.ground.ChunkViews.release(level);
                        PlanetPlayers.release(level);
                    }
                });
        // A system's planets take their saved sizes before any of its chunks is addressed.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.level.LevelEvent.Load cpEvent) -> {
                    if (!(cpEvent.getLevel()
                            instanceof net.minecraft.server.level.ServerLevel level)) return;
                    var server = level.getServer();
                    if (level.getChunkSource().getGenerator()
                            instanceof SystemChunkGenerator system) {
                        system.materialize(level.getSeed());
                        Universe.bind(level, system);
                        dev.kolustar.worldgen.plan.PlanetPlanStore.bind(level,system);
                        GroundServer.system(system);
                    }
                    ShipRecords.attach(level);
                });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.server.ServerStoppedEvent cpEvent) -> {
                    var server = cpEvent.getServer();
                    PlanetPlayers.clear();
                    dev.kolustar.ground.ObserverFrames.clear();
                    WaypointServer.clear();
                    ShipRecords.clear();
                    dev.kolustar.ship.ShipDormancy.clear();
                    PlanetSystem.reset();
                    dev.kolustar.ground.ChunkViews.resetLayout();
                });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent
                                cpEvent) -> {
                    var handler =
                            ((net.minecraft.server.level.ServerPlayer) cpEvent.getEntity())
                                    .connection;
                    var server = handler.player.getServer();
                    GroundServer.shapes(handler.player);
                    PlanetPlayers.joined(handler.player);
                });
        // Someone reborn with no bed wakes on the home planet, as a new player does.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(PlanetPlayers::respawn);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) ->
                        PlanetPlayers.joined((net.minecraft.server.level.ServerPlayer) event.getEntity()));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.tick.ServerTickEvent.Post cpEvent) -> {
                    var server = cpEvent.getServer();
                    if (server.getTickCount() % 20 == 0) PlanetPlayers.arrive(server);
                });
        // Completed player deaths are handled at ServerPlayer.die TAIL by GroundDeathMixin.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent
                                cpEvent) -> {
                    var handler =
                            ((net.minecraft.server.level.ServerPlayer) cpEvent.getEntity())
                                    .connection;
                    var server = handler.player.getServer();
                    PlanetPlayers.cancel(handler.player.getUUID());
                    dev.kolustar.ground.ObserverFrames.forget(handler.player);
                    WaypointServer.forget(handler.player);
                    GroundServer.forget(handler.player.getUUID());
                });
    }

    private static void initializePlayerSystems() {
        GroundFlightServer.initialize();
        WaypointServer.initialize();
        dev.kolustar.waypoint.MapNavigationServer.initialize();
        ShipGuide.initialize();
        GroundViewServer.initialize();
        PlanetRemoteServer.initialize();
    }

    private static void registerTicketsAndCleanup() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.tick.LevelTickEvent.Pre cpEvent) -> {
                    if (!(cpEvent.getLevel()
                            instanceof net.minecraft.server.level.ServerLevel level)) return;
                    if (PlanetLevels.is(level)) GroundTickets.update(level);
                });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.server.ServerStoppingEvent cpEvent) -> {
                    var server = cpEvent.getServer();
                    GroundServer.clear();
                    GroundTickets.clear();
                    ImageStream.clear();
                    ChunkTickReadiness.clear();
                    AnomalyZone.clear();
                });
    }

    private static void registerSleeping() {
        // A bed on a planet is slept in while it is night where the bed stands, whatever the
        // level's clock says.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(PlanetPlayers::canSleep);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent event) -> {
                    var entity = event.getEntity();
                    if (!(entity instanceof net.minecraft.world.entity.player.Player)
                            || event.getProblem()
                                    == net.minecraft.world.entity.player.Player.BedSleepingProblem
                                            .NOT_POSSIBLE_HERE) return;
                    entity.getSleepingPos()
                            .ifPresent(
                                    pos -> {
                                        if (GroundDaylight.kept(entity.level(), pos))
                                            event.setContinueSleeping(
                                                    !GroundDaylight.day(
                                                            (ServerLevel) entity.level(), pos));
                                    });
                });
    }

    private static void registerClockTicks() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.tick.ServerTickEvent.Pre cpEvent) -> {
                    var server = cpEvent.getServer();
                    var a = server.getLevel(PlanetLevels.PLANET);
                    try {
                        if (a != null) GroundClock.get(a).advance();
                    } catch (IllegalArgumentException | IllegalStateException error) {
                        for (var p : server.getPlayerList().getPlayers())
                            if (PlanetLevels.is(p.level()))
                                p.sendSystemMessage(
                                        Component.translatable(
                                                "message.kolustar.clock_stopped",
                                                WaypointServer.failure(error)));
                    }
                });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.tick.LevelTickEvent.Post cpEvent) -> {
                    if (!(cpEvent.getLevel()
                            instanceof net.minecraft.server.level.ServerLevel level)) return;
                    if (!PlanetLevels.is(level)) return;
                    try {
                        var saved = GroundClock.get(level);
                        GroundFlightServer.tick(level, saved);
                        ImageStream.tick(level);
                        ChunkTickReadiness.postProcessHeld(level);
                        GroundViewServer.tick(level, saved);
                        AnomalyZone.tick(level);
                        DevelopmentHooks.tickLevel.accept(level);
                        if (level.getGameTime() % 20 == 0) {
                            ShipRecords.refresh(level);
                            for (var player : List.copyOf(level.players())) {
                                var at = GroundFlightServer.position(player, saved);
                                if (at != null && Universe.grow(level, at))
                                    saved = GroundClock.get(level);
                            }
                            for (var player : level.players()) PlanetPlayers.send(player, saved);
                        }
                    } catch (IllegalArgumentException | IllegalStateException error) {
                        for (var player : level.players())
                            player.sendSystemMessage(
                                    Component.translatable(
                                            "message.kolustar.clock_failed",
                                            WaypointServer.failure(error)));
                    }
                });
    }

    private static void registerCommands() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.RegisterCommandsEvent cpEvent) -> {
                    KolustarCommands.register(cpEvent.getDispatcher());
                });
    }

}
