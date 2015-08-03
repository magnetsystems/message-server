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

/**
 */
public class TestGeohash {


    public static void main(String [] args) {
        testGeohash();
        testDecodeGeohash();
    }

    public static void testGeohash()
    {
        GeoPoint point1= GeoFactoryDefault.getInstance().createGeoPoint(34.3073,-97.0318);
        String hash  = GeoFactoryDefault.getInstance().encodePointToGeohash(point1);
        System.out.println(" Expected :\"9y4gpvrwe0jz\"   got :" + hash);

        GeoPoint point2= GeoFactoryDefault.getInstance().createGeoPoint(28.0621,-81.7863);
        String hash2  = GeoFactoryDefault.getInstance().encodePointToGeohash(point2);
        System.out.println(" Expected :\"dhvzdut194dd\"   got :" + hash2);

    }



    public static void testDecodeGeohash()
    {
        BoundingBox box= GeoFactoryDefault.getInstance().decodeGeoHash("9y4gpvrwe0jz");
        //String hash2  = GeoFactoryDefault.getInstance().encodePointToGeohash(point2);
        System.out.println(" Expected :\"34.3073,-97.0318 \"   got :" + box.point1());

    }

}
