package dev.kolustar.worldgen.plan;

import static org.junit.jupiter.api.Assertions.*;
import dev.kolustar.geometry.CubeSurfaceTopology.Face;
import dev.kolustar.worldgen.surface.SurfaceFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import org.junit.jupiter.api.*;
import java.io.*;
import java.util.*;

class SurfaceDeterminismTest {
    @BeforeAll static void boot() throws Exception { PlanetPlanFixtures.boot(); }
    @AfterAll static void close() throws Exception { PlanetPlanFixtures.close(); }

    @Test void blocksAndEverySummaryCellMatchEv19Bytes() throws Exception {
        var base=TerrainPlanS2BTest.flat(1);
        var volume=TerrainPlanS2BTest.volume("floating_rock",base.position(base.node(Face.TOP,20,16)),210,32,8,24,
                dev.kolustar.worldgen.plan.terrain.TerrainPlan.Operation.FILL);
        var fixed=base.withContent(List.of(),List.of(volume),List.of());
        var bounds=new PlanetPlan.Bounds(160,204,160,207,204,161);
        var voxels=new ArrayList<PlanetPlan.Voxel>();
        for(int x=0;x<48;x++) for(int z=0;z<2;z++) voxels.add(new PlanetPlan.Voxel(x,0,z,Blocks.GOLD_BLOCK.defaultBlockState()));
        var piece=new PlanetPlan.Piece(0,new BlockPos(160,204,160),Rotation.NONE,Mirror.NONE,bounds,bounds,TerrainAdjustment.NONE);
        var site=new PlanetPlan.Site(1,ResourceLocation.parse("test:narrow_roof"),Face.TOP,160,160,bounds,List.of(piece));
        var plan=new PlanetPlan(512,fixed,List.of(site),List.of(new PlanetPlan.Template(voxels)));
        var terrain=TerrainPlanS2BTest.terrain(fixed);terrain.initializePlan(()->plan);
        var function=new SurfaceFunction(terrain);
        var bytes=new ByteArrayOutputStream();
        try(var out=new DataOutputStream(bytes)) {
            for(var face:Face.VALUES) for(int z:new int[]{0,160,161,256,511})for(int x:new int[]{0,160,192,256,336,511})
                for(var block:function.blocks(face,x,z))out.writeUTF(block.toString());
            // Persist raw double bits and all ordered material shares, including the rasterized roof/volume.
            var field=dev.kolustar.worldgen.surface.SurfaceSummary.class.getDeclaredField("levels");field.setAccessible(true);
            var levels=(SurfaceFunction.Cell[][])field.get(terrain.surfaceSummary(function));
            for(var level:levels) for(var cell:level) {
                out.writeDouble(cell.height());out.writeDouble(cell.canopyCoverage());out.writeDouble(cell.canopyTop());out.writeInt(cell.linearRgb());
                for(var shares:List.of(cell.materials(),cell.canopyMaterials())) {
                    out.writeInt(shares.size());
                    for(var share:shares) {out.writeUTF(share.block().toString());out.writeDouble(share.share());}
                }
            }
        }
        var hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        // Captured with ev19 (3e64f9f8) classes: 2,456,863 bytes, before the async refactor.
        // Refreshed by the kolustar rename: ids sort before minecraft:, reordering equal shares.
        assertEquals("74553c4160b1dc70d6aa0d7c9c03bb3fed1892ac9294aab36e54859e39894bab",hash);
    }
}
