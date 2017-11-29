# topothin
The problem:
A spatial dataset contains adjoining polygons that need to be thinned while retaining the spatial
relationships between polygons. Any simple thinning algorithm will almost certainly destroy the
spatial relationships.

The solution:
Reverse engineer the underlying topology of the polygon set, thin the edges and reassemble the polygons.

Topothin is a Java library that finds nodes in multiple polygonal layers, creates edges from
linear rings and the node database, thins the edges and reassembles the thinned rings and polygons.
Topothin depends on the JTS Topology Suite (https://www.locationtech.org/projects/technology.jts).

A related project, topothin-abs (https://github.com/pyuill/topothin-abs), employs this library to
thin datasets published by The Australian Bureau of Statistics (ABS)
