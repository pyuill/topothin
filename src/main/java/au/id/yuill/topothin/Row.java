/*
 * Copyright (c) 2017 Peter Yuill
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 */
package au.id.yuill.topothin;

import org.locationtech.jts.geom.MultiPolygon;

import java.util.List;

/**
 * A holder class for spatial table rows.
 *
 * @version 1.0
 * @author Peter Yuill
 */
public class Row {
    public Table table;
    public String code;
    public String name;
    public MultiPolygon mp;
    public List<TopoPoly> topoPolyList;
}
