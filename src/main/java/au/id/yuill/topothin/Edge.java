/*
 * Copyright (c) 2017 Peter Yuill
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * <br>
 * Note that until the JTS Topology Suite is formally released under EPL v1.0
 * the licenses of this program (EPL) and JTS (LGPL) are incompatible. This program
 * will not reach a 1.0 release status until JTS is released by the Eclipse working group
 * LocationTech under EPL v1.0
 */
package au.id.yuill.topothin;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A holder class for edge data including the end nodes and all the coordinates.
 *
 * @version 1.0
 * @author Peter Yuill
 */
public class Edge {
    private TopoCoord node1;
    private TopoCoord node2;
    private Coordinate[] coordArray;
    private int hash;

    public Edge(TopoCoord node1, TopoCoord node2, Coordinate[] coordArray) {
        this.node1 = node1;
        this.node2 = node2;
        this.coordArray = coordArray;
        hash = node1.bucketIndex() + node2.bucketIndex();
    }

    public TopoCoord getNode1() {
        return node1;
    }

    public TopoCoord getNode2() {
        return node2;
    }

    public Coordinate[] getCoordArray() { return coordArray; }

    public void setCoordArray(Coordinate[] coordArray) { this.coordArray = coordArray; }

    public boolean isForward(Edge that) {
        if (this.equals(that)) {
            if (this.node1.equals(that.node1)) {
                return true;
            } else {
                return false;
            }
        } else {
            throw new RuntimeException("Edges unequal");
        }
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Edge) {
            Edge that = (Edge)obj;
            int listSize = this.coordArray.length;
            if (that.coordArray.length == listSize) {
                if (this.node1.equals(that.node1) && this.node2.equals(that.node2)) {
                    if (this.coordArray[1].equals(that.coordArray[1])) {
                        return true;
                    } else {
                        return false;
                    }
                } else if (this.node1.equals(that.node2) && this.node2.equals(that.node1)) {
                    // the coordinates are reversed
                    if (this.coordArray[1].equals(that.coordArray[listSize - 2])) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
