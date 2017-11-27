/*
 * Copyright (c) 2017 Peter Yuill
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 */
package au.id.yuill.topothin;

import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

import java.sql.Connection;

/**
 * A source spatial table.
 *
 * @version 1.0
 * @author Peter Yuill
 */
public interface Table {

    public void populateTopoCoordData(Connection conn, WKBReader reader, TopoCoordData tcd) throws Exception;

    public void saveThinnedGeometry(Connection conn, WKBWriter writer, TopoCoordData tcd) throws Exception;
}
