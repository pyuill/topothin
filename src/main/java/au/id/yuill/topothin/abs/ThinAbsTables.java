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
package au.id.yuill.topothin.abs;

import au.id.yuill.topothin.DefaultSimplifier;
import au.id.yuill.topothin.Row;
import au.id.yuill.topothin.Table;
import au.id.yuill.topothin.TopoCoordData;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.GeometryCombiner;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Load ABS data for postcode (poa), local government area (lga), state electoral division (sed)
 * and commonwealth electoral division (ced).
 * <br>Find nodes in all LineStrings, find edges, simplify edges, create simplified polygons
 * and save thinned geometry back to the display tables. Also create GeoJSON versions of the
 * features. Lastly create polygons for states and save them.
 * <br><br>This program depends on BuildTables to create the display versions of admin tables.
 *
 * @version 1.0
 * @author Peter Yuill
 */
public class ThinAbsTables {

    public static String releaseYear;
    public static String dbUrl;
    public static String dbUser;
    public static String dbPass;
    public static Connection conn;
    public static Statement stmt;

    public static void main(String[] args) throws Exception {
        if (args.length == 4) {
            releaseYear = args[0];
            dbUrl = args[1];
            dbUser = args[2];
            dbPass = args[3];
        } else {
            System.out.println("usage: ThinAbsTables releaseYear dbUrl dbUser dbPassword");
            System.exit(0);
        }
        Class.forName("org.postgresql.Driver");
        conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);

        WKBReader reader = new WKBReader();
        WKBWriter writer = new WKBWriter(2, true);
        TopoCoordData tcd = new TopoCoordData(new DefaultSimplifier(), 4283);

        Table poaTable = new AbsTable(releaseYear, "poa", null);
        Table lgaTable = new LgaTable(releaseYear, null);
        Table sedTable = new AbsTable(releaseYear, "sed", null);
        Table cedTable = new AbsTable(releaseYear, "ced", null);

        poaTable.populateTopoCoordData(conn, reader, tcd);
        lgaTable.populateTopoCoordData(conn, reader, tcd);
        sedTable.populateTopoCoordData(conn, reader, tcd);
        cedTable.populateTopoCoordData(conn, reader, tcd);

        tcd.findNodes();

        tcd.createEdges();

        tcd.simplifyEdges();

        tcd.createThinnedPolygons();

        poaTable.saveThinnedGeometry(conn, writer, tcd);
        lgaTable.saveThinnedGeometry(conn, writer, tcd);
        sedTable.saveThinnedGeometry(conn, writer, tcd);
        cedTable.saveThinnedGeometry(conn, writer, tcd);

        PreparedStatement ps = conn.prepareStatement("update ste_disp set geom = ? where ste_code = ?");

        List<Row> lgaList = tcd.tableMap.get(lgaTable);
        Map<String, List<LgaRow>> stateMap = new HashMap();
        for (Row row: lgaList) {
            LgaRow lga = (LgaRow)row;
            List<LgaRow> stateList = stateMap.get(lga.stateCode);
            if (stateList == null) {
                stateList = new ArrayList();
                stateMap.put(lga.stateCode, stateList);
            }
            stateList.add(lga);
        }

        for (String steCode: stateMap.keySet()) {
            List<MultiPolygon> mpList = new ArrayList();
            for (LgaRow lgaRow: stateMap.get(steCode)) {
                mpList.add(lgaRow.mp);
            }
            Geometry comb = GeometryCombiner.combine(mpList);
            System.out.println(steCode + " Combined " + comb.getClass().getName() + " " + comb.getSRID() + " " + comb.getNumGeometries());
            Geometry state = comb.buffer(0.0);
            System.out.println(steCode + " Buffer " + state.getClass().getName() + " " + state.getSRID() + " " + state.getNumGeometries());
            if (state instanceof Polygon) {
                state = new MultiPolygon(new Polygon[] {(Polygon)state}, tcd.factory);
            }
            ps.setBytes(1, writer.write(state));
            ps.setString(2, steCode);
            ps.execute();
        }
        Statement stmt = conn.createStatement();
        stmt.execute("update ste_disp set geojson = ST_AsGeoJSON(geom,6,0) where geom is not null");
        stmt.close();
        conn.close();
    }
}
