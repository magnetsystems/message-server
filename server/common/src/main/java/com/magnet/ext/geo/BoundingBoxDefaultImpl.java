/*   Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.magnet.ext.geo;

import java.util.List;

/**
 */
public class BoundingBoxDefaultImpl implements  BoundingBox{
    private static final String TAG=BoundingBoxDefaultImpl.class.getName();
    private GeoPoint point1;
    private GeoPoint point2;

    BoundingBoxDefaultImpl(GeoPoint point1,GeoPoint point2) {
        this.point1 = point1;
        this.point2 = point2;
    }

    @Override
    public GeoPoint point1() {
        return point1;
    }

    @Override
    public GeoPoint point2() {
        return point2;
    }

    @Override
    public boolean isPointInsideShape(GeoPoint point) {

        double minLat=0, maxLat=0,minLon=0,maxLon=0;

        if (point1.latitude()<point2.latitude())
        {
            minLat = point1.latitude();
            maxLat = point2.latitude();
        }else {
            minLat = point2.latitude();
            maxLat = point2.latitude();
        }

        if (point1.longitude()<point2.longitude())
        {
            minLon = point1.longitude();
            maxLon = point2.longitude();
        }else {
            minLon = point2.longitude();
            maxLon = point1.longitude();
        }

        return  (point1.latitude() >=minLat && point1.latitude()<=maxLat ) &&
                (point1.longitude() >=minLon && point1.longitude()<=maxLon );

    }

    @Override
    public boolean intersects(GeoShape shape) {
        return false;
    }

    @Override
    public int getDistanceToCenterFromPoint(GeoPoint point, boolean isMetric) {
        return 0;
    }

    @Override
    public List<GeoPoint> getPointsInsideShape(List<GeoPoint> points) {
        return null;
    }

    @Override
    public List<GeoPoint> getPointsOutsideShape(List<GeoPoint> points) {
        return null;
    }

    @Override
    public GeoPoint center() {
        return GeoPointFactory.createGeoPoint(
                (this.point1.latitude() + this.point2.latitude())/2.0d,
                (this.point1.longitude() + this.point2.longitude())/2.0d);
   }


   public static BoundingBox getBoundingBox(List<GeoPoint> points){

       if (points==null || points.size() <1) return null;

       double minLat=90.0d,maxLat=-90.0d;
       double minLon=180.0d,maxLon=-180.0d;

       //Log.d(TAG," Points ====> " + (points==null?0:points.size()));

       for (int indx=0;indx<points.size();indx++) {
          GeoPoint point = points.get(indx);
          if (point==null) continue;

          minLat = Math.min(point.latitude(),minLat);
          maxLat = Math.max(point.latitude(),maxLat);
          minLon = Math.min(point.longitude(),minLon);
          maxLon = Math.max(point.longitude(),maxLon);
       }

       return new BoundingBoxDefaultImpl( GeoPointFactory.createGeoPoint(minLat,minLon),
               GeoPointFactory.createGeoPoint(maxLat,maxLon));
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoundingBoxDefaultImpl that = (BoundingBoxDefaultImpl) o;

        if (point1 != null ? !point1.equals(that.point1) : that.point1 != null) return false;
        if (point2 != null ? !point2.equals(that.point2) : that.point2 != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = point1 != null ? point1.hashCode() : 0;
        result = 31 * result + (point2 != null ? point2.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BoundingBoxDefaultImpl{" +
                "point1=" + point1 +
                ", point2=" + point2 +
                '}';
    }
}
