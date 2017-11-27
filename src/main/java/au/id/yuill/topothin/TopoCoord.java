/*
 * Copyright (c) 2017 Peter Yuill
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 */
package au.id.yuill.topothin;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import java.io.Serializable;
import java.text.DecimalFormat;

/**
 * A coordinate with links to the LineStrings that contain it, thus enabling
 * the identification of nodes in LineStrings.
 *
 * @version 1.0
 * @author Peter Yuill
 */
public class TopoCoord implements Serializable {

    static final long serialVersionUID = 1L;

    public static int DEGREE_TO_INT = 8388608; // 2 to the power 23
    public double lat;
    public double lon;
    public boolean node = false;
    public LineString[] lineString = new LineString[0];

    public int bucketIndex() {
        return ((Double.valueOf(lat * DEGREE_TO_INT).intValue() & 0x000f) << 8) |
                (Double.valueOf(lon * DEGREE_TO_INT).intValue() & 0x000f);
    }

    public boolean isGeoEq(Coordinate c) {
        return ((this.lat == c.y) && (this.lon == c.x));
    }

    public boolean isGeoEq(TopoCoord tc) {
        return ((this.lat == tc.lat) && (this.lon == tc.lon));
    }

    public void testNode(TopoCoord nextTc) {
        // test for new poly
        for (LineString nextLs: nextTc.lineString) {
            boolean match = false;
            for (LineString thisLs: lineString) {
                if (nextLs == thisLs) {
                    match = true;
                    break;
                }
            }
            if (!match) { // next tc contains a new id, thus is a node
                nextTc.node = true;
                break;
            }
        }
        for (LineString thisLs: lineString) {
            boolean match = false;
            for (LineString nextLs : nextTc.lineString) {
                if (nextLs == thisLs) {
                    match = true;
                    break;
                }
            }
            if (!match) { // this tc contains a new id, thus is a node
                node = true;
                break;
            }
        }
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("#00.000000");
        StringBuilder sb = new StringBuilder("  ");
        sb.append(df.format(lat));
        sb.append(' ');
        sb.append(df.format(lon));
        if (node) {
            sb.append(" *");
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return bucketIndex();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TopoCoord) {
            TopoCoord that = (TopoCoord)obj;
            if ((this.lat == that.lat) && (this.lon == that.lon)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
