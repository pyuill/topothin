/*
 * Copyright (c) 2017 Peter Yuill
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 */
package au.id.yuill.topothin;

import org.locationtech.jts.geom.*;

import java.io.Serializable;
import java.util.*;

/**
 * Maintains a model of the spatial universe under study including:
 * <br>All spatial tables and their rows
 * <br>All coordinates and their relationships to LineStrings
 * <br>All edges found within LineStrings
 * <br><br>Functions are provided to load spatial features, create edges,
 * thin edges, construct spatial features from edges and save features.
 *
 * @version 1.0
 * @author Peter Yuill
 */
public class TopoCoordData implements Serializable {

    static final long serialVersionUID = 1L;

    public TopoCoord[][] points = new TopoCoord[65536][];
    public Map<Edge, Edge> edgeMap = new HashMap();
    public Map<Table,List<Row>> tableMap = new HashMap();
    public int coords;
    public GeometryFactory factory;

    private Simplifier simplifier;

    public TopoCoordData() {
        this.simplifier = new DefaultSimplifier();
        this.factory = new GeometryFactory();
    }

    public TopoCoordData(Simplifier simplifier, int srid) {
        this.simplifier = simplifier;
        this.factory = new GeometryFactory(new PrecisionModel(), srid);
    }

    public void addRow(Row row) {
        List<Row> rows = tableMap.get(row.table);
        if (rows == null) {
            rows = new ArrayList();
            tableMap.put(row.table, rows);
        }
        rows.add(row);
        for (int i = 0; i < row.mp.getNumGeometries(); i++) {
            addPolygon((Polygon)row.mp.getGeometryN(i));
        }
    }

    public void addPolygon(Polygon poly) {
        addLineString(poly.getExteriorRing());
        int interiorRings = poly.getNumInteriorRing();
        for (int i = 0; i < interiorRings; i++) {
            addLineString(poly.getInteriorRingN(i));
        }
    }

    public void addLineString(LineString ls) {
        CoordinateSequence seq = ls.getCoordinateSequence();
        int size = seq.size();
        for (int i = 0; i < size; i++) {
            Coordinate coord = seq.getCoordinate(i);
            addCoordinate(coord, ls);
        }
    }

    public void addCoordinate(Coordinate coord, LineString lineString) {
        coords++;
        TopoCoord p = new TopoCoord();
        p.lon = coord.x;
        p.lat = coord.y;
        p.lineString = new LineString[] {lineString};
        int bucketIndex = p.bucketIndex();
        coord.z = bucketIndex;
        TopoCoord[] leaf = points[bucketIndex];
        if (leaf == null) {
            leaf = new TopoCoord[1];
            leaf[0] = p;
            points[bucketIndex] = leaf;
        } else {
            boolean newTopoCoord = true;
            for (TopoCoord ltc: leaf) {
                if (ltc.isGeoEq(p)) { // in the database
                    newTopoCoord = false;
                    // look for lineString already in the list - specifically first and last point are the same
                    boolean newLineString = true;
                    for (LineString ls: ltc.lineString) {
                        if (ls == lineString) {
                            newLineString = false;
                            break;
                        }
                    }
                    if (newLineString) {
                        int rSize = ltc.lineString.length;
                        LineString[] newLsArray = new LineString[rSize + 1];
                        System.arraycopy(ltc.lineString, 0, newLsArray, 0, rSize);
                        newLsArray[rSize] = lineString;
                        ltc.lineString = newLsArray;
                    }
                    break; // found in database
                }
            }
            if (newTopoCoord) {
                TopoCoord[] newLeaf = new TopoCoord[leaf.length + 1];
                System.arraycopy(leaf, 0, newLeaf, 0, leaf.length);
                newLeaf[leaf.length] = p;
                points[bucketIndex] = newLeaf;
            }
        }
    }

    public TopoCoord getTopoCoord(Coordinate coord) {
        TopoCoord p = new TopoCoord();
        p.lon = coord.x;
        p.lat = coord.y;
        TopoCoord[] leaf = points[(int)coord.z];
        for (TopoCoord ltc: leaf) {
            if (ltc.isGeoEq(p)) { // in the database
                return ltc;
            }
        }
        return null;
    }

    public int pointCount() {
        int count = 0;
        for (int i = 0; i < points.length; i++) {
            if (points[i] != null) {
                count += points[i].length;
            }
        }
        return count;
    }

    public void findNodes() {
        System.out.println("Finding Nodes");
        for (Table table: tableMap.keySet()) {
            for (Row row: tableMap.get(table)) {
                MultiPolygon multi = row.mp;
                for (int i = 0; i < multi.getNumGeometries(); i++) {
                    Polygon poly = (Polygon)multi.getGeometryN(i);
                    findNodesInLineString(poly.getExteriorRing());
                    for (int j = 0; j < poly.getNumInteriorRing(); j++) {
                        findNodesInLineString(poly.getInteriorRingN(j));
                    }
                }
            }
        }

    }

    private void findNodesInLineString(LineString ls) {
        CoordinateSequence seq = ls.getCoordinateSequence();
        TopoCoord last = null;
        for (int k = 0; k < seq.size(); k++) {
            Coordinate coord = seq.getCoordinate(k);
            TopoCoord tc = getTopoCoord(coord);
            if (last != null) {
                last.testNode(tc);
            }
            last = tc;
        }

    }

    public void print() {
        for (Table table: tableMap.keySet()) {
            for (Row row: tableMap.get(table)) {
                System.out.println(row.name);
                MultiPolygon multi = row.mp;
                for (int i = 0; i < multi.getNumGeometries(); i++) {
                    Polygon poly = (Polygon)multi.getGeometryN(i);
                    print(poly.getExteriorRing());
                    for (int j = 0; j < poly.getNumInteriorRing(); j++) {
                        print(poly.getInteriorRingN(j));
                    }
                }
            }
        }

    }

    public void createEdges() throws Exception {
        System.out.println("Creating Edges");
        for (Table table: tableMap.keySet()) {
            for (Row row : tableMap.get(table)) {
                MultiPolygon multi = row.mp;
                row.topoPolyList = new ArrayList();
                for (int i = 0; i < multi.getNumGeometries(); i++) {
                    Polygon poly = (Polygon) multi.getGeometryN(i);
                    TopoPoly tp = new TopoPoly();
                    tp.exterior = new ArrayList();
                    if (createEdges((LinearRing)poly.getExteriorRing(), factory, tp.exterior, false)) {
                        for (int j = 0; j < poly.getNumInteriorRing(); j++) {
                            List<RingEdge> interior = new ArrayList();
                            tp.interiorList.add(interior);
                            createEdges((LinearRing)poly.getInteriorRingN(j), factory, interior, true);
                        }
                        row.topoPolyList.add(tp);
                    }
                }
                if (row.topoPolyList.size() == 0) { // All islands, find the largest polygon
                    Polygon max = null;
                    double maxArea = 0.0;
                    for (int i = 0; i < multi.getNumGeometries(); i++) {
                        Polygon p = (Polygon)multi.getGeometryN(i);
                        double area = p.getArea();
                        if (area > maxArea) {
                            max = p;
                            maxArea = area;
                        }
                    }
                    TopoPoly tp = new TopoPoly();
                    tp.exterior = new ArrayList();
                    createEdges((LinearRing)max.getExteriorRing(), factory, tp.exterior, true);
                    for (int j = 0; j < max.getNumInteriorRing(); j++) {
                        List<RingEdge> interior = new ArrayList();
                        tp.interiorList.add(interior);
                        createEdges((LinearRing)max.getInteriorRingN(j), factory, interior, true);
                    }
                    row.topoPolyList.add(tp);
                }
            }
        }
    }

    private boolean createEdges(LinearRing ring, GeometryFactory fac, List<RingEdge> edgeList,
                                boolean constructNodeIfNeeded) throws Exception {
        TopoCoord node1 = null;
        List<Coordinate> coords = new ArrayList();
        int orphanCoords = 0;
        CoordinateSequence seq = ring.getCoordinateSequence();
        for (int i = 0; i < seq.size(); i++) {
            Coordinate coord = seq.getCoordinate(i);
            TopoCoord tc = getTopoCoord(coord);
            if (node1 == null) {
                if (tc.node) {
                    node1 = tc;
                    coords.add(coord);
                } else {
                    orphanCoords++;
                }
            } else {
                coords.add(coord);
                if (tc.node) {
                    Edge polyEdge = new Edge(node1, tc, coords.toArray(new Coordinate[coords.size()]));
                    Edge mapEdge = edgeMap.get(polyEdge);
                    if (mapEdge == null) {
                        mapEdge = polyEdge;
                        edgeMap.put(polyEdge, polyEdge);
                    }
                    edgeList.add(new RingEdge(mapEdge, mapEdge.isForward(polyEdge)));
                    node1 = tc;
                    coords = new ArrayList();
                    coords.add(coord);
                }
            }
        }
        if (node1 == null) { // The LineString is an island
            if (constructNodeIfNeeded) {
                node1 = getTopoCoord(seq.getCoordinate(0));
                node1.node = true;
                Edge edge = new Edge(node1, node1, seq.toCoordinateArray());
                edgeMap.put(edge, edge);
                edgeList.add(new RingEdge(edge, true));
                return true;
            } else {
                return false; // No polygon created
            }
        }
        // at this point there may be orphan coords waiting to be added to the last linestring
        if (orphanCoords > 0) {
            for (int i = 0; i < seq.size(); i++) {
                Coordinate coord = seq.getCoordinate(i);
                TopoCoord tc = getTopoCoord(coord);
                coords.add(coord);
                if (tc.node) {
                    Edge polyEdge = new Edge(node1, tc, coords.toArray(new Coordinate[coords.size()]));
                    Edge mapEdge = edgeMap.get(polyEdge);
                    if (mapEdge == null) {
                        mapEdge = polyEdge;
                        edgeMap.put(polyEdge, polyEdge);
                    }
                    edgeList.add(new RingEdge(mapEdge, mapEdge.isForward(polyEdge)));
                    break;
                }
            }
        }
        return true;
    }

    public void simplifyEdges() {
        System.out.println("Simplifying edges");
        int originalCoords = 0;
        int simplifiedCoords = 0;
        for (Edge edge: edgeMap.values()) {
            LineString ls = factory.createLineString(edge.getCoordArray());
            LineString simple = simplifier.simplify(ls);
            edge.setCoordArray(simple.getCoordinates());
        }

    }

    public void createThinnedPolygons() throws Exception {
        System.out.println("Creating thinned Polygons");
        for (Table table: tableMap.keySet()) {
            for (Row row : tableMap.get(table)) {
                List<Polygon> polyList = new ArrayList();
                for (TopoPoly poly: row.topoPolyList) {
                    LinearRing exterior = createLinearRing(poly.exterior);
                    List<LinearRing> interior = new ArrayList();
                    for (List<RingEdge> intEdges: poly.interiorList) {
                        interior.add(createLinearRing(intEdges));
                    }
                    if (interior.size() == 0)  {
                        polyList.add(factory.createPolygon(exterior));
                    } else {
                        polyList.add(factory.createPolygon(exterior, interior.toArray(new LinearRing[interior.size()])));
                    }
                }
                row.mp = factory.createMultiPolygon(polyList.toArray(new Polygon[polyList.size()]));
            }
        }
    }

    private LinearRing createLinearRing(List<RingEdge> edgeList) throws Exception {
        List<Coordinate> coords = new ArrayList();
        for (RingEdge ringEdge: edgeList) {
            Coordinate[] ec = ringEdge.edge.getCoordArray();
            Coordinate coord;
            if (ringEdge.forward) {
                coord = ec[0];
            } else {
                coord = ec[ec.length - 1];
            }
            if (coords.size() == 0) {
                coords.add(coord);
            } else if (!coord.equals(coords.get(coords.size() - 1))) {
                throw new Exception("Invalid edge sequence: " + ringEdge.forward + " " + coord + " " + coords.get(coords.size() - 1));
            }
            if (ringEdge.forward) {
                for (int j = 1; j < ec.length; j++) {
                    coords.add(ec[j]);
                }
            } else {
                for (int j = (ec.length - 2); j >= 0; j--) {
                    coords.add(ec[j]);
                }
            }
        }
        LinearRing ring = factory.createLinearRing(coords.toArray(new Coordinate[coords.size()]));
        return ring;
    }

    private void findCodes(Set<String> codeSet, TopoCoord node) {

    }

    private void print(LineString ls) {
        CoordinateSequence seq = ls.getCoordinateSequence();
        for (int k = 0; k < seq.size(); k++) {
            System.out.println("  " + seq.getCoordinate(k));
        }

    }
}
