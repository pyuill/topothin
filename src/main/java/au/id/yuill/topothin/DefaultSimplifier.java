/*
 * Copyright (c) 2017 Peter Yuill
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 */
package au.id.yuill.topothin;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

/**
 * Default implementation of the Simplifier interface, employing the JTS TopologyPreservingSimplifier.
 *
 * @version 1.0
 * @author Peter Yuill
 */
public class DefaultSimplifier implements Simplifier {

    private double lengthFactor = 0.02;
    private double maxDistanceTolerance = 0.01;

    public DefaultSimplifier() {
    }

    public DefaultSimplifier(double lengthFactor, double maxDistanceTolerance) {
        this.lengthFactor = lengthFactor;
        this.maxDistanceTolerance = maxDistanceTolerance;
    }

    /**
     *  Generate a simplified version of a source LineString.
     *  This implementation uses the JTS TopologyPreservingSimplifier. The distance tolerance
     *  is dependant on the length of the LineString; longer LineStrings being subjected to
     *  more aggressive thinning. A default maximum dt of 0.01 (about 1km) is applied as very
     *  long LineStrings appear to produce exaggerated results.
     *
     * @param source The source LineString to be simplified
     * @return The simplified LineString
     */
    public LineString simplify(LineString source) {
        double toleranceDistance = source.getLength() * lengthFactor;
        if (toleranceDistance > maxDistanceTolerance) {
            toleranceDistance = maxDistanceTolerance;
        }
        LineString simple = (LineString) TopologyPreservingSimplifier.simplify(source, toleranceDistance);
        return simple;
    }
}
