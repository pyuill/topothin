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

import java.util.ArrayList;
import java.util.List;

/**
 * A holder class for lists of edges for exterior and interior rings.
 *
 * @version 1.0
 * @author Peter Yuill
 */
public class TopoPoly {
    public List<RingEdge> exterior;
    public List<List<RingEdge>> interiorList = new ArrayList();
}
