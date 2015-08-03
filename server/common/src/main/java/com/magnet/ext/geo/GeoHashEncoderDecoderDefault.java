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

import java.util.HashMap;
import java.util.Map;

/**
 */

public class GeoHashEncoderDecoderDefault implements GeoHashEncoderDecoder {

    private static final int HASH_LEN = 12; //max number of characters in geohash
    private static final int NO_OF_BITS_BASE32 = 5;
    private static final String BASE_32_CHARS = "0123456789bcdefghjkmnpqrstuvwxyz";
    public static final double MAX_LAT = 90.0d;
    public static final double MIN_LAT = -90.0d;
    public static final double MIN_LON = -180.0d;
    public static final double MAX_LON = 180.0d;

    private static Map<Character,Integer> inverseMap = new HashMap<Character,Integer>();

    static {
        for(int indx=0;indx<BASE_32_CHARS.length();indx++) {
            inverseMap.put(BASE_32_CHARS.charAt(indx),indx);
        }
    }

    protected GeoHashEncoderDecoderDefault(){

    }




    public String encodePoint(GeoPoint point) {
        long bits = 0;

        int count = 0;
        //byte []
        boolean even = true;

        double lt1= MIN_LAT, lt2= MAX_LAT;
        double ln1= MIN_LON, ln2= MAX_LON;

        while(count < (HASH_LEN*5)) {
            if (even) {
                double mid = (double) (ln1+ln2)/2.0f;
                if (point.longitude() >=mid) {
                     bits <<=1;
                    bits |=0x1;
                    ln1 = mid;
                } else {
                    //bits = bits.shiftLeft(1);
                    bits <<=1;
                    ln2=mid;
                }

            }else {
                double mid = (double) (lt1+lt2)/2.0f;
                if (point.latitude() >=mid) {
                    bits <<=1;
                    bits |=0x1;
                    lt1 = mid;
                } else {
                   bits <<=1;
                   lt2=mid;
                }
            }
            even=!even;
            count++;
        }
        //bits = bits.shiftLeft(4);
        bits <<= 4;
        //bits.;// need to remove the most significant 4 bits ( 64(long) - 60(HASH_LEN*5 bits))
        return toBase32(bits);
    }



    public BoundingBox decodePoint(String geoHash) {

        if (geoHash==null || geoHash.trim().length() <1 || geoHash.trim().length() > HASH_LEN) {
            return null;
        }

        double lt1= MIN_LAT, lt2= MAX_LAT; // -90 0 90 Intervals
        double ln1=MIN_LON,ln2=MAX_LON; // -180 0 180 Intervals
        int bitMasks [] = {16,8,4,2,1};

        int noOfChars = geoHash.length();
        boolean even = true;

        for (int indx=0;indx<noOfChars;indx++) {

            char key = geoHash.charAt(indx);
            Integer val =  inverseMap.get(key);
            if (val==null) {
                throw new RuntimeException("Invalid characters found in geocode");
            }

            for(int ndx=0;ndx < NO_OF_BITS_BASE32 ;ndx++) {
                int isBitOn = val.intValue() & bitMasks[ndx];
                if ( even ) {
                    double mid = (double)(ln1 + ln2) / 2.0f;
                    if (isBitOn>0) {
                        ln1 = mid;
                    } else {
                        ln2 = mid;
                    }
                }else {
                    double mid = (double)(lt1 + lt2) / 2.0f;
                    if (isBitOn>0) {
                        lt1 = mid;
                    } else {
                        lt2 = mid;
                    }
                }
                even=!even;
            }

        }
        GeoPoint point1= GeoPointFactory.createGeoPoint(lt1,ln1);
        GeoPoint point2= GeoPointFactory.createGeoPoint(lt2,ln2);
        return BoundingBoxFactory.createBoundingBox(point1,point2);
    }



    private String toBase32(long bits) {
        long bitCopy = bits;
        long bitMask = 0xf800000000000000L;
        int signBit = 0x7fffffff;
        StringBuilder hash = new StringBuilder();

        for(int indx=0;indx<HASH_LEN;indx++) {
            int val = (int) ((bitCopy &bitMask)>>>59) ;//  ignore the sign bit
            hash.append(BASE_32_CHARS.charAt(val));
             bitCopy<<=5;
        }
        return hash.toString();

    }


}
