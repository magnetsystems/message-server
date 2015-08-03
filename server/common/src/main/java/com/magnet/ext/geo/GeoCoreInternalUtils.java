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
public final class GeoCoreInternalUtils {
	
	private static final double MILES_2_KM = 1.60934d;
	private static final double  KM_2_MILES = 0.621371d;
	private static final double RADIUS_IN_METRIC = 6371d;
	private static final double RADIUS_IMPERIAL = 3958d;
	private static final double PI = 3.14159;
	private static final int E6 = 1000000;
		
	private GeoCoreInternalUtils() {};
	

	protected static double distanceBetweenPoints(GeoPoint point1,GeoPoint point2,boolean isMetric) {
        double lat1InRad = degrees2Radians(point1.latitude());
        double lat2InRad = degrees2Radians(point2.latitude());

        double sinLat1 = Math.sin(lat1InRad);
	    double sinLat2 = Math.sin(lat2InRad);
	    double cosLat1 = Math.cos(lat1InRad);
	    double cosLat2 = Math.cos(lat2InRad);

	    double cosOfLon1MinusLon2 = Math.cos(degrees2Radians(point1.longitude()) -  degrees2Radians(point2.longitude()));

	    double val =  (Math.acos(sinLat1 * sinLat2 + cosLat1 * cosLat2 *cosOfLon1MinusLon2)) * (isMetric?RADIUS_IN_METRIC:RADIUS_IMPERIAL);
        return Math.round(val*100)/100d;
	}

    protected static double degrees2Radians(double degrees) {
		return (degrees * PI/180.0d);
	}

    protected static double radians2Degrees(double radians) {
		return (radians * 180.0d/PI);
	}

    protected static double kmToMiles(double distance) {
		return (distance * MILES_2_KM);
	}

    protected static double milesToKm(double distance) {
		return (distance*KM_2_MILES);
	}

    protected static int toE6(double number) {
		return (int)(E6*number);
	}
	
}
