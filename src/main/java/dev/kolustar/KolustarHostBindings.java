package dev.kolustar;

import dev.kolustar.flight.FlightHost;
import dev.kolustar.flight.GroundFlightServer;
import dev.kolustar.geometry.BentStripGeometry.Point;
import dev.kolustar.ground.*;
import dev.kolustar.render.GroundViewServer;
import dev.kolustar.ship.SableFrames;
import dev.kolustar.ship.ShipHost;
import dev.kolustar.space.SpacePackets;
import dev.kolustar.sky.GroundEphemerisSave;
import dev.kolustar.sky.SkyHost;
import dev.kolustar.space.SpaceApproach;
import dev.kolustar.space.SpaceHost;
import dev.kolustar.space.SpaceServer;
import dev.kolustar.space.SpaceView;
import dev.kolustar.waypoint.ShipGuide;
import dev.kolustar.waypoint.WaypointSave;
import dev.kolustar.waypoint.WaypointServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** Cross-subsystem server adapters, installed before subsystem initialization. */
final class KolustarHostBindings {
    private KolustarHostBindings() {}

    static void install() {
        installFlightHost();
        installGroundHost();
        installShipHost();
        installSkyHost();
        installSpaceHost();
    }

    private static void installFlightHost() {
        FlightHost.install(
                new FlightHost() {
                    @Override
                    public void viewLost(net.minecraft.server.level.ServerPlayer player) {
                        GroundViewServer.remove(player);
                    }

                    @Override
                    public void targetSync(net.minecraft.server.level.ServerPlayer player) {
                        WaypointServer.send(player);
                    }

                    @Override
                    public void targetCleared(net.minecraft.server.level.ServerPlayer player) {
                        WaypointSave.get(player.serverLevel()).clear(player.getUUID());
                    }

                    @Override
                    public Component failure(RuntimeException error) {
                        return WaypointServer.failure(error);
                    }

                    @Override
                    public int leave(net.minecraft.server.level.ServerPlayer player) {
                        return PlanetPlayers.leave(player);
                    }

                    @Override
                    public boolean nearHull(
                            net.minecraft.server.level.ServerPlayer player, double distance) {
                        return dev.kolustar.ship.SableFrames.near(player, distance);
                    }
                });
    }

    private static void installGroundHost() {
        GroundHost.install(
                new GroundHost() {
                    @Override
                    public void afterDeath(net.minecraft.server.level.ServerPlayer player) {
                        PlanetPlayers.afterDeath(player);
                        dev.kolustar.space.GasPlayerExposureServer.completedDeath(player);
                    }

                    @Override
                    public void viewLost(net.minecraft.server.level.ServerPlayer player) {
                        GroundViewServer.remove(player);
                    }

                    @Override
                    public void resited(net.minecraft.server.level.ServerPlayer player) {
                        GroundFlightServer.site(player);
                    }

                    @Override
                    public void regionSync(net.minecraft.server.level.ServerPlayer player) {
                        SpaceServer.sync(player);
                    }

                    @Override
                    public boolean controlled(net.minecraft.world.entity.Entity entity) {
                        return GroundFlightServer.controlled(entity);
                    }

                    @Override
                    public boolean aloft(net.minecraft.server.level.ServerPlayer player) {
                        return GroundFlightServer.aloft(player);
                    }

                    @Override
                    public boolean carrying(net.minecraft.server.level.ServerPlayer player) {
                        return SableFrames.carrying(player);
                    }

                    @Override
                    public void clearArrival() {
                        SpaceApproach.clear();
                    }
                });
    }

    private static void installShipHost() {
        ShipHost.install(
                new ShipHost() {
                    @Override
                    public void mapSync(ServerLevel level) { dev.kolustar.waypoint.MapNavigationServer.sync(level); }

                    @Override
                    public boolean alwaysTicket(java.util.UUID ship) {
                        return DevelopmentHooks.alwaysTicket.test(ship);
                    }

                    @Override
                    public boolean retainLead(
                            ServerLevel level, dev.ryanhcode.sable.sublevel.ServerSubLevel body) {
                        return SpaceServer.retainLead(level, body);
                    }

                    @Override
                    public boolean probeTicket(java.util.UUID ship) {
                        return DevelopmentHooks.heldTicket.test(ship);
                    }

                    @Override
                    public boolean externalRoll(
                            net.minecraft.world.level.Level level,
                            dev.ryanhcode.sable.sublevel.ServerSubLevel body) {
                        return SpaceServer.rollControl(level, body);
                    }

                    @Override
                    public void gasCollected(dev.ryanhcode.sable.sublevel.ServerSubLevel body) {
                        ShipGuide.skim(body);
                    }

                    @Override
                    public void pilotReleased(net.minecraft.server.level.ServerPlayer player) {
                        SpaceServer.releaseHelm(player);
                    }

                    @Override
                    public void releaseArrival(ServerLevel level) {
                        SpaceApproach.release(level);
                    }

                    @Override
                    public void advanceSurroundings(
                            ServerLevel level,
                            dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer container) {
                        SpaceServer.step(level, container);
                    }

                    @Override
                    public dev.kolustar.sky.Space.Frame departureFrame(
                            ServerLevel level,
                            dev.kolustar.sky.Space.Motion motion,
                            org.joml.Quaterniond orientation,
                            dev.kolustar.chart.GroundEphemeris.Model model,
                            int planet,
                            long tick) {
                        return SpaceServer.entering(
                                level, motion, orientation, model, planet, tick);
                    }

                    @Override
                    public void hullDeparted(ServerLevel level, int planet, String id) {
                        SpaceServer.departed(level, planet, id);
                    }

                    @Override
                    public dev.kolustar.sky.Space.Frame surroundingsFrame(ServerLevel level) {
                        return SpaceServer.frame(level);
                    }

                    @Override
                    public Point carriedSpin(ServerLevel level, java.util.UUID hull) {
                        return SpaceServer.carriedSpin(level, hull);
                    }

                    @Override
                    public boolean landingLoaded(ServerLevel level, Point anchor) {
                        return SpaceApproach.full(level, anchor);
                    }

                    @Override
                    public boolean landingReady(
                            dev.kolustar.chart.GroundEphemeris.Body body,
                            double height,
                            boolean full) {
                        return SpaceApproach.ready(body, height, full);
                    }

                    @Override
                    public net.minecraft.world.level.ChunkPos landingColumn(
                            dev.kolustar.geometry.BentStripGeometry.Point anchor) {
                        return SpaceApproach.column(anchor);
                    }

                    @Override
                    public void hullEnteredChart(ServerLevel level, String id) {
                        SpaceServer.enteredChart(level, id);
                    }

                    @Override
                    public void hullArrived(
                            ServerLevel level, dev.ryanhcode.sable.sublevel.ServerSubLevel body) {
                        SpaceApproach.arrived(level, body);
                    }

                    @Override
                    public SpacePackets.Frame carriedFrame(
                            net.minecraft.server.level.ServerPlayer player) {
                        return SpaceServer.packet(player);
                    }
                });
    }

    private static void installSkyHost() {
        SkyHost.install(
                new SkyHost() {
                    @Override
                    public boolean underWay() {
                        return GroundFlightServer.underWay();
                    }

                    @Override
                    public double sunlight(
                            ServerLevel level,
                            net.minecraft.core.BlockPos pos,
                            GroundEphemerisSave clock) {
                        var eye =
                                SpaceServer.frame(level)
                                        .universe(
                                                new dev.kolustar.geometry.BentStripGeometry.Point(
                                                        pos.getX() + .5,
                                                        pos.getY() + .5,
                                                        pos.getZ() + .5),
                                                clock.model,
                                                clock.tick());
                        return SpaceView.sunlight(clock.model, eye, clock.tick());
                    }
                });
    }

    private static void installSpaceHost() {
        SpaceHost.install(
                new SpaceHost() {
                    @Override
                    public void targetCleared(net.minecraft.server.level.ServerPlayer player) { dev.kolustar.waypoint.MapNavigationServer.markerSelected(player); }

                    @Override
                    public void targetSync(
                            net.minecraft.server.level.ServerPlayer player, long session) {
                        WaypointServer.send(player, session);
                    }
                });
    }

}
