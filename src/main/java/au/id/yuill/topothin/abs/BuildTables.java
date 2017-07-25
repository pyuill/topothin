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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.io.WKBReader;

import java.sql.*;
import java.util.ArrayList;

/**
 * A class to build display tables corresponding to Australian Statistical Geography Standard
 * datasets published by the Australian Bureau of Statistics. The ThinAbsTables class depends
 * on these tables existing.
 * <br><br>
 * This class depends on the creation of PostGIS tables from ABS datasets by external means
 * eg QGIS import.
 *
 * @version 1.0
 * @author Peter Yuill
 */
public class BuildTables {

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
            System.out.println("usage: BuildTables releaseYear dbUrl dbUser dbPassword");
            System.exit(0);
        }
        Class.forName("org.postgresql.Driver");
        conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
        stmt = conn.createStatement();

        createAdminTable("poa", 4);
        createAdminTable("lga", 5);
        createAdminTable("sed", 5);
        createAdminTable("ced", 3);
        createAdminTable("ste", 1);
        createPostcodeTable();
        populateState();
        populateAdminTable("poa");
        populateAdminTable("lga");
        populateAdminTable("sed");
        populateAdminTable("ced");

        populateRelationships();
    }

    public static void createAdminTable(String adminCode, int codeSize) throws Exception {
        stmt.execute("drop table if exists " + adminCode + "_disp");
        stmt.execute("create table " + adminCode + "_disp (" + adminCode +
                "_code varchar(" + codeSize + ") primary key, name varchar(50), " +
                "lon double precision, lat double precision, geojson varchar, " +
                "geom GEOMETRY(MULTIPOLYGON,4283))");
    }

    public static void createPostcodeTable() throws Exception {
        stmt.execute("drop table if exists postcode");
        stmt.execute("create table postcode (poa_code varchar(4) primary key, lga_code varchar(5), " +
                "ste_code varchar(1), sed_code varchar(5), ced_code varchar(3))");
    }

    public static void populateState() throws Exception {
        stmt.execute("insert into ste_disp (ste_code, name, lat, lon) " +
                "select ste_code" + releaseYear + ", ste_name" + releaseYear +
                ", ST_Y(ST_Centroid(ST_Collect(f.geom))), ST_X(ST_Centroid(ST_Collect(f.geom))) from " +
                "(select ste_code" + releaseYear + ", ste_name" + releaseYear + ", (ST_Dump(geom)).geom from lga" + releaseYear +
                ") as f group by f.ste_code" + releaseYear + ", f.ste_name" + releaseYear);
    }

    public static void populateAdminTable(String adminCode) throws Exception {
        stmt.execute("insert into " + adminCode + "_disp (" + adminCode + "_code, name, lon, lat) " +
                "select " + adminCode + "_code" + releaseYear + ", " + adminCode + "_name" + releaseYear +
                ", ST_X(ST_Centroid(geom)), ST_Y(ST_Centroid(geom)) from " + adminCode + releaseYear +
                " where geom is not null");
    }

    public static void populateRelationships() throws Exception {
        WKBReader reader = new WKBReader();
        ArrayList<Poa> poaList = new ArrayList();
        ArrayList<Lga> lgaList = new ArrayList();
        ArrayList<AdminArea> sedList = new ArrayList();
        ArrayList<AdminArea> cedList = new ArrayList();
        ResultSet rs = stmt.executeQuery("select poa_code" + releaseYear + ", ST_AsEWKB(geom) from poa" +
                releaseYear + " where geom is not null");
        while(rs.next()) {
            Poa poa = new Poa();
            poa.poaCode = rs.getString(1);
            poa.geom = (MultiPolygon)reader.read(rs.getBytes(2));
            poaList.add(poa);
        }
        rs.close();
        rs = stmt.executeQuery("select lga_code" + releaseYear + ", ste_code" + releaseYear +
                ", ST_AsEWKB(geom) from lga" + releaseYear + " where geom is not null");
        while(rs.next()) {
            Lga lga = new Lga();
            lga.lgaCode = rs.getString(1);
            lga.steCode = rs.getString(2);
            lga.geom = (MultiPolygon)reader.read(rs.getBytes(3));
            lgaList.add(lga);
        }
        rs.close();
        long start = System.currentTimeMillis();
        for (Poa poa: poaList) {
            poa.area = 0.0;
            for (Lga lga: lgaList) {
                if (poa.geom.coveredBy(lga.geom)) {
                    poa.lgaCode = lga.lgaCode;
                    poa.steCode = lga.steCode;
                    poa.area = Double.MAX_VALUE;
                    continue;
                } else if (poa.geom.overlaps(lga.geom)) {
                    Geometry intersection = poa.geom.intersection(lga.geom);
                    if (!intersection.isEmpty()) {
                        int count = intersection.getNumGeometries();
                        double area = 0.0;
                        for (int i = 0; i < count; i++) {
                            Geometry n = intersection.getGeometryN(i);
                            area += n.getArea();
                        }
                        if (area > poa.area) {
                            poa.area = area;
                            poa.lgaCode = lga.lgaCode;
                            poa.steCode = lga.steCode;
                        }
                    }
                }
            }
            if (poa.lgaCode == null) {
                System.out.println(poa.poaCode + " null LGA");
            }
        }
        System.out.println("Time: " + (System.currentTimeMillis() - start));
        rs = stmt.executeQuery("select sed_code" + releaseYear + ", ST_AsEWKB(geom) from sed" +
                releaseYear + " where geom is not null");
        while(rs.next()) {
            AdminArea sed = new AdminArea();
            sed.code = rs.getString(1);
            sed.geom = (MultiPolygon)reader.read(rs.getBytes(2));
            sedList.add(sed);
        }
        rs.close();
        start = System.currentTimeMillis();
        for (Poa poa: poaList) {
            poa.area = 0.0;
            for (AdminArea sed: sedList) {
                if (poa.geom.coveredBy(sed.geom)) {
                    poa.sedCode = sed.code;
                    poa.area = Double.MAX_VALUE;
                    continue;
                } else if (poa.geom.overlaps(sed.geom)) {
                    Geometry intersection = poa.geom.intersection(sed.geom);
                    if (!intersection.isEmpty()) {
                        int count = intersection.getNumGeometries();
                        double area = 0.0;
                        for (int i = 0; i < count; i++) {
                            Geometry n = intersection.getGeometryN(i);
                            area += n.getArea();
                        }
                        if (area > poa.area) {
                            poa.area = area;
                            poa.sedCode = sed.code;
                        }
                    }
                }
            }
            if (poa.sedCode == null) {
                System.out.println(poa.poaCode + " null State Electoral Division");
            }
        }
        System.out.println("Time: " + (System.currentTimeMillis() - start));
        rs = stmt.executeQuery("select ced_code" + releaseYear + ", ST_AsEWKB(geom) from ced" +
                releaseYear + " where geom is not null");
        while(rs.next()) {
            AdminArea ced = new AdminArea();
            ced.code = rs.getString(1);
            ced.geom = (MultiPolygon)reader.read(rs.getBytes(2));
            cedList.add(ced);
        }
        rs.close();
        start = System.currentTimeMillis();
        for (Poa poa: poaList) {
            poa.area = 0.0;
            for (AdminArea ced: cedList) {
                if (poa.geom.coveredBy(ced.geom)) {
                    poa.cedCode = ced.code;
                    poa.area = Double.MAX_VALUE;
                    continue;
                } else if (poa.geom.overlaps(ced.geom)) {
                    Geometry intersection = poa.geom.intersection(ced.geom);
                    if (!intersection.isEmpty()) {
                        int count = intersection.getNumGeometries();
                        double area = 0.0;
                        for (int i = 0; i < count; i++) {
                            Geometry n = intersection.getGeometryN(i);
                            area += n.getArea();
                        }
                        if (area > poa.area) {
                            poa.area = area;
                            poa.cedCode = ced.code;
                        }
                    }
                }
            }
            if (poa.cedCode == null) {
                System.out.println(poa.poaCode + " null Commonwealth Electoral Division");
            }
        }
        System.out.println("Time: " + (System.currentTimeMillis() - start));
        PreparedStatement ps = conn.prepareStatement("insert into postcode(poa_code, lga_code, ste_code, sed_code, ced_code)" +
                " values (?,?,?,?,?)");
        for (Poa poa: poaList) {
            ps.setString(1, poa.poaCode);
            ps.setString(2, poa.lgaCode);
            ps.setString(3, poa.steCode);
            ps.setString(4, poa.sedCode);
            ps.setString(5, poa.cedCode);
            ps.execute();
        }
        ps.close();
        conn.close();
    }
}
