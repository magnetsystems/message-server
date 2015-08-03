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
public class GeoFactoryDefault implements GeoFactory {



    private static class GeoFactoryHolder  {
        static final GeoFactoryDefault factory = new GeoFactoryDefault();
    }

    private GeoFactoryDefault(){};

    public static GeoFactory getInstance() {
        return GeoFactoryHolder.factory;
    }

    @Override
    public GeoPoint createGeoPoint(double latitude, double longitude) {
        return GeoPointFactory.createGeoPoint(latitude,longitude);
    }

    @Override
    public GeoPoint createGeoPointAsE6(int latitudeAsE6, int longitudeAsE6) {
        return GeoPointFactory.createGeoPoint(latitudeAsE6,longitudeAsE6);
    }

    @Override
    public BoundingBox createBoundingBox(GeoPoint point1, GeoPoint point2) {
        return BoundingBoxFactory.createBoundingBox(point1, point2);
    }

    @Override
    public BoundingBox decodeGeoHash(String geohash) {
        return GeohashEncoderDecoderFactory.createEncoderDecoder().decodePoint(geohash);
    }

    @Override
    public String encodePointToGeohash(GeoPoint point) {
        return GeohashEncoderDecoderFactory.createEncoderDecoder().encodePoint(point);
    }

    @Override
    public BoundingBox boundingBoxForPoints(List<GeoPoint> points) {
         return BoundingBoxDefaultImpl.getBoundingBox(points);
    }

    public double distanceBetweenPoints(GeoPoint point1,GeoPoint point2,boolean isMetric) {
        return GeoCoreInternalUtils.distanceBetweenPoints(point1, point2, isMetric);
    }
}
