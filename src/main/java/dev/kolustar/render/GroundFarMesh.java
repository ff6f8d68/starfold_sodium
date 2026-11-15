package dev.kolustar.render;

import dev.kolustar.chart.PlanetShape;
import dev.kolustar.geometry.BentStripGeometry.Point;
import dev.kolustar.geometry.CubeSurfaceTopology.Cell;
import dev.kolustar.geometry.CubeSurfaceTopology.Face;
import dev.kolustar.worldgen.lod.*;
import dev.kolustar.worldgen.lod.VoxelMesher.Quad;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Shared resident/far adapter to the voxel mesher. No interpolated terrain planes. */
final class GroundFarMesh {
    record Surface(double height, BlockState material, double floor,
            GroundSurface.Run canopy, GroundSurface.Run trunk, int rgb,
            dev.kolustar.worldgen.surface.SurfaceFunction.Column detail, double canopyCoverage,
            VoxelColumn voxels) {
        Surface(double h, BlockState m, double f, GroundSurface.Run c, GroundSurface.Run t,
                int rgb, dev.kolustar.worldgen.surface.SurfaceFunction.Column d, double coverage) {
            this(h,m,f,c,t,rgb,d,coverage,null);
        }
        Surface(double h, BlockState m, double f, GroundSurface.Run c, GroundSurface.Run t,
                int rgb, dev.kolustar.worldgen.surface.SurfaceFunction.Column d) { this(h,m,f,c,t,rgb,d,1); }
        Surface(double h, BlockState m, double f, GroundSurface.Run c, GroundSurface.Run t, int rgb) { this(h,m,f,c,t,rgb,null); }
        Surface(double h, BlockState m, double f, GroundSurface.Run c, GroundSurface.Run t) { this(h,m,f,c,t,-1); }
        Surface(double h, BlockState m, double f) { this(h,m,f,null,null); }
        Surface(double h, BlockState m) { this(h,m,h); }
        Surface withFloor(double f) { return new Surface(height,material,f,canopy,trunk,rgb,detail,canopyCoverage,voxels); }
        static Surface voxel(VoxelColumn column) {
            var runs = column.runs();
            var top = runs.isEmpty() ? null : runs.getLast();
            return new Surface(top == null ? -128 : top.top()-128.,
                    top == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : top.block(),
                    runs.isEmpty() ? -128 : runs.getFirst().bottom()-128.,null,null,-1,null,0,column);
        }
        VoxelColumn column() {
            if (voxels != null) return voxels;
            // Registry-only fixtures and the null-terrain startup shell use the same boxes.
            var runs = new ArrayList<VoxelColumn.Run>();
            int high = (int)Math.round(height+128);
            if (!material.isAir()) runs.add(new VoxelColumn.Run(Math.min(high-1,(int)Math.floor(floor+128)-64),high,material));
            for (var r : new GroundSurface.Run[]{trunk,canopy})
                if (r != null && !r.material().isAir()) runs.add(new VoxelColumn.Run(r.bottom(),r.top()+1,r.material()));
            return VoxelColumn.compose(runs);
        }
    }
    interface Field { Surface cell(Face face, int x, int z, int step); }

    static int level(double distance, int previous) {
        return level(distance,previous,70,720);
    }
    static int level(double distance,int previous,double fov,int height) {
        // Whole-planet fallback stays coarse until bounded resident tiles replace its pixels.
        return Math.max(Math.max(16, dev.kolustar.KolustarConfig.coarseStep()),
                VoxelLodPolicy.screenStep(distance,previous,fov,height));
    }
    /** Remote planets have no resident detail layer: their visible colours need the screen-sized mip. */
    static int remoteLevel(double distance, int previous, double fov, int height) {
        return Math.max(4, VoxelLodPolicy.screenStep(distance, previous, fov, height));
    }
    static double distance(PlanetShape shape, Point camera, int patch, double height) {
        return distance(shape,camera,Face.VALUES[GroundViewPolicy.face(patch)],GroundViewPolicy.x(patch),GroundViewPolicy.z(patch),256,height);
    }
    /** Closest point on the spherical face rectangle, including its edges. */
    static double distance(PlanetShape shape, Point camera, Face f, int x0, int z0, int width, double height) {
        double half=shape.half(),radius=shape.radius()+height,a=x0-half,b=z0-half;
        double endX=Math.min(a+width,half),endZ=Math.min(b+width,half);
        double n=camera.dot(f.normal),u=camera.dot(f.u),v=camera.dot(f.v),best=-Double.MAX_VALUE;
        if(n>0) { double x=u*half/n,z=v*half/n; if(x>=a && x<=endX && z>=b && z<=endZ) best=camera.length(); }
        for(double x:new double[]{a,endX}) {
            double denominator=n*half+u*x;
            double z=denominator>0 ? Math.clamp(v*(half*half+x*x)/denominator,b,endZ) : b;
            best=Math.max(best,dot(n,u,v,half,x,z));
            best=Math.max(best,dot(n,u,v,half,x,endZ)); best=Math.max(best,dot(n,u,v,half,x,b));
        }
        for(double z:new double[]{b,endZ}) {
            double denominator=n*half+v*z;
            double x=denominator>0 ? Math.clamp(u*(half*half+z*z)/denominator,a,endX) : a;
            best=Math.max(best,dot(n,u,v,half,x,z));
        }
        return Math.sqrt(Math.max(0,camera.dot(camera)+radius*radius-2*radius*best));
    }
    private static double dot(double n,double u,double v,double half,double x,double z) {
        return (n*half+u*x+v*z)/Math.sqrt(half*half+x*x+z*z);
    }
    static List<Quad> build(PlanetShape shape,int patch,int step,Field field) {
        return build(shape,Face.VALUES[GroundViewPolicy.face(patch)],GroundViewPolicy.x(patch),GroundViewPolicy.z(patch),256,step,field);
    }
    static List<Quad> build(PlanetShape shape,Face face,int x,int z,int width,int step,Field field) {
        int x1=Math.min(x+width,shape.size()), z1=Math.min(z+width,shape.size());
        var cached=cached(shape,field);
        int nx=Math.max(1,(x1-x)/step), nz=Math.max(1,(z1-z)/step);
        var samples=new Surface[(nx+1)*(nz+1)];
        for(int j=0;j<=nz;j++) for(int i=0;i<=nx;i++) {
            int sx=Math.min(x+i*step,shape.size()-1), sz=Math.min(z+j*step,shape.size()-1);
            samples[i+j*(nx+1)]=cached.cell(face,sx,sz,step);
        }
        var out=new ArrayList<Quad>();
        double base=-128;
        for(int j=0;j<nz;j++) for(int i=0;i<nx;i++) {
            int ax=x+i*step,bx=Math.min(x+(i+1)*step,x1),az=z+j*step,bz=Math.min(z+(j+1)*step,z1);
            var s00=samples[i+j*(nx+1)];
            var s10=samples[i+1+j*(nx+1)];
            var s11=samples[i+1+(j+1)*(nx+1)];
            var s01=samples[i+(j+1)*(nx+1)];
            double h00=h(s00), h10=h(s10), h11=h(s11), h01=h(s01);
            var mat=dominant(s00,s10,s11,s01);
            if(!mat.isAir()) {
                // Top polygon surface (downsampled/seed-approximated heightfield), not voxel boxes.
                add(out,mat,3,new Point(ax,h00,az),new Point(bx,h10,az),new Point(bx,h11,bz),new Point(ax,h01,bz),
                        step>=4 && mat.is(net.minecraft.tags.BlockTags.LEAVES) ? -2
                                : step>=4 && mat.getFluidState().isEmpty() ? -3 : -1, false);
                if(!mat.getFluidState().isEmpty()) {
                    add(out,mat,2,new Point(ax,base,az),new Point(bx,base,az),new Point(bx,base,bz),new Point(ax,base,bz),-1,true);
                }
            }
            // Skirts at tile edges so far terrain reads as continuous ground, not floating sheets.
            if(i==0) skirt(out,mat,ax,az,ax,bz,h00,h01,base,0);
            if(i==nx-1) skirt(out,mat,bx,bz,bx,az,h11,h10,base,1);
            if(j==0) skirt(out,mat,bx,az,ax,az,h10,h00,base,4);
            if(j==nz-1) skirt(out,mat,ax,bz,bx,bz,h01,h11,base,5);
        }
        return List.copyOf(out);
    }

    private static double h(Surface s) { return s==null ? -128 : s.height(); }
    private static BlockState dominant(Surface a, Surface b, Surface c, Surface d) {
        for (var s : new Surface[]{a,b,c,d}) if (s != null && !s.material().isAir()) return s.material();
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }
    private static void add(List<Quad> out, BlockState mat, int axis,
            Point p0, Point p1, Point p2, Point p3, int rgb, boolean volume) {
        if (mat.isAir() && axis==3) return;
        var q = new Quad(mat, axis/2, List.of(p0,p1,p2,p3), volume, rgb);
        subdivide(q,out);
    }
    private static void skirt(List<Quad> out, BlockState mat, double x0,double z0,double x1,double z1,
            double h0,double h1,double base,int axis) {
        if (mat.isAir() || (h0<=base && h1<=base)) return;
        add(out, mat, axis, new Point(x0,base,z0), new Point(x1,base,z1), new Point(x1,h1,z1), new Point(x0,h0,z0), -1, true);
    }
    /** Shared 16-block bend nodes, including side edges. All source faces remain axis-aligned. */
    private static void subdivide(Quad q,List<Quad> out) {
        var p=q.points(); var a=p.getFirst(); var u=p.get(1).sub(a); var v=p.get(3).sub(a);
        int nu=Math.max(1,(int)Math.ceil(Math.hypot(u.x(),u.z())/16));
        int nv=Math.max(1,(int)Math.ceil(Math.hypot(v.x(),v.z())/16));
        for(int i=0;i<nu;i++) for(int j=0;j<nv;j++) {
            double s0=(double)i/nu,s1=(double)(i+1)/nu,t0=(double)j/nv,t1=(double)(j+1)/nv;
            out.add(new Quad(q.material(),q.axis(),List.of(a.add(u.scale(s0)).add(v.scale(t0)),a.add(u.scale(s1)).add(v.scale(t0)),
                    a.add(u.scale(s1)).add(v.scale(t1)),a.add(u.scale(s0)).add(v.scale(t1))),q.volume(),q.rgb()));
        }
    }
    static int side(Quad q) {
        if(!q.volume() && VoxelMesher.plant(q.material()))return q.axis()==0 ? -2 : -3;
        if(q.axis()==1) return -1;
        var p=q.points(); var n=p.get(1).sub(p.getFirst()).cross(p.get(2).sub(p.getFirst()));
        return q.axis()==0 ? (n.x()<0 ? 0:1) : (n.z()<0 ? 2:3);
    }
    static Field cached(PlanetShape shape,Field field) {
        var cells=new HashMap<Long,Surface>();
        return (f,x,z,s)-> {
            var c=shape.topology().canonical(new Cell(f,x,0,z)).cell();
            int a=c.u()/s*s,b=c.v()/s*s;
            long key=((c.face().ordinal()*2048L+a)*2048+b)*128+s;
            return cells.computeIfAbsent(key,k->field.cell(c.face(),a,b,s));
        };
    }
    static void wall(List<Quad> out,BlockState material,int x,int z,int right,int far,double high,double low,int side) {
        if(low>=high) return;
        int axis=switch(side) {case 0->0;case 1->1;case 2->4;case 3->5;default->throw new IllegalArgumentException("Wall side");};
        out.add(new Quad(material,axis/2,VoxelMesher.points(axis,x,low,z,right,high,far)));
    }
    private GroundFarMesh() {}
}
