package biggerreactorsrework.core.util;

import java.util.*;

/** Modified 2026-10-27: emit only the outside of joined fluid volumes, including partial bend contacts. */
public final class FluidSurfaceMesh {
    private static final float EPS=0.00001f;
    public record Box(float x0,float y0,float z0,float x1,float y1,float z1) {
        public float min(int axis){return axis==0?x0:axis==1?y0:z0;}
        public float max(int axis){return axis==0?x1:axis==1?y1:z1;}
        public boolean valid(){return Float.isFinite(x0+y0+z0+x1+y1+z1) && x1>x0 && y1>y0 && z1>z0;}
    }
    public record Face(int axis,boolean positive,float plane,float u0,float v0,float u1,float v1) {
        public float[] vertex(int index) {
            int order=positive?index:(4-index)%4;
            float u=order==1 || order==2?u1:u0,v=order>=2?v1:v0;
            float[] point=new float[3];point[axis]=plane;point[(axis+1)%3]=u;point[(axis+2)%3]=v;return point;
        }
        public double area(){return (u1-u0)*(v1-v0);}
    }
    public static List<Face> faces(Box box,List<Box> joined,int hidden) {
        if(!box.valid())return List.of();
        var result=new ArrayList<Face>();
        for(int axis=0;axis<3;axis++)for(int sign=0;sign<2;sign++) {
            if((hidden & 1<<(axis*2+sign))!=0)continue;
            boolean positive=sign==1;float plane=positive?box.max(axis):box.min(axis);
            int u=(axis+1)%3,v=(axis+2)%3;
            var neighbors=new ArrayList<Box>();
            var us=new TreeSet<Float>();var vs=new TreeSet<Float>();
            us.add(box.min(u));us.add(box.max(u));vs.add(box.min(v));vs.add(box.max(v));
            for(var other:joined) {
                if(other==box || !other.valid())continue;
                boolean beyond=positive?other.min(axis)<=plane+EPS && other.max(axis)>plane+EPS:
                        other.max(axis)>=plane-EPS && other.min(axis)<plane-EPS;
                if(!beyond || other.max(u)<=box.min(u)+EPS || other.min(u)>=box.max(u)-EPS ||
                        other.max(v)<=box.min(v)+EPS || other.min(v)>=box.max(v)-EPS)continue;
                neighbors.add(other);
                us.add(Math.clamp(other.min(u),box.min(u),box.max(u)));us.add(Math.clamp(other.max(u),box.min(u),box.max(u)));
                vs.add(Math.clamp(other.min(v),box.min(v),box.max(v)));vs.add(Math.clamp(other.max(v),box.min(v),box.max(v)));
            }
            var ua=new ArrayList<>(us);var va=new ArrayList<>(vs);
            for(int i=0;i<ua.size()-1;i++)for(int j=0;j<va.size()-1;j++) {
                float lowU=ua.get(i),highU=ua.get(i+1),lowV=va.get(j),highV=va.get(j+1);
                if(highU-lowU<EPS || highV-lowV<EPS)continue;
                float midU=(lowU+highU)*.5f,midV=(lowV+highV)*.5f;
                boolean covered=false;
                for(var other:neighbors)if(midU>=other.min(u)-EPS && midU<=other.max(u)+EPS &&
                        midV>=other.min(v)-EPS && midV<=other.max(v)+EPS){covered=true;break;}
                if(!covered)result.add(new Face(axis,positive,plane,lowU,lowV,highU,highV));
            }
        }
        return result;
    }
    private FluidSurfaceMesh() { }
}
