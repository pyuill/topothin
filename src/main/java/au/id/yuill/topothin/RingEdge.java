/*
 * Copyright (c) 2017 Peter Yuill
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 */
package au.id.yuill.topothin;

/**
 * A holder class for edge instances within rings. This is necessary because edges may
 * be ordered in the reverse sequence required for a particular ring.
 *
 * @version 1.0
 * @author Peter Yuill
 */
public class RingEdge {
    public boolean forward;
    public Edge edge;

    public RingEdge(Edge edge, boolean forward) {
        this.edge = edge;
        this.forward = forward;
    }
}
