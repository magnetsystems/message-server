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

import java.lang.ref.SoftReference;

/**
 */
public class GeoPointDefaultImpl implements GeoPoint {

    private static final int E6 = 1000000;

    private int latitudeE6=0;
    private int longitudeE6=0;
    private String name = null;
    private String description = null;
    private SoftReference<Object> data = null;

    public GeoPointDefaultImpl(double latitude,double longitude) {
        this.latitudeE6 = (int) (latitude * E6);
        this.longitudeE6 =(int) (longitude*E6);
    }

    @Override
    public double latitude() {
        return ((double)this.latitudeE6/E6);
    }

    @Override
    public int latitudeAsE6() {
        return this.latitudeE6;
    }

    @Override
    public double longitude() {
        return ((double)this.longitudeE6/E6);
    }

    @Override
    public int longitudeAsE6() {
        return this.longitudeE6;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name=name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String description) {
        this.description=description;
    }

    @Override
    public Object getData() {
        return this.data.get();
    }

    @Override
    public void setData(Object data) {
        this.data=new SoftReference<Object>(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoPointDefaultImpl that = (GeoPointDefaultImpl) o;

        if (latitudeE6 != that.latitudeE6) return false;
        if (longitudeE6 != that.longitudeE6) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = latitudeE6;
        result = 31 * result + longitudeE6;
        return result;
    }

    @Override
    public String toString() {
        return "GeoPointDefaultImpl{" +
                "latitude=" + latitude() +
                ", longitude=" + longitude() +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", data=" + data +
                '}';
    }
}
