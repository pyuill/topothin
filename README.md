The problem:
I have a dataset containing adjoining polygons and I need to thin the polygons while retaining the relationships
between them. Any simple thinning algorithm will almost certainly destroy the spatial relationships between the polygons.

A solution:
Reverse engineer the underlying topology of the polygon set, thin the edges and reassemble the polygons. The TopoCoordData
class does just that, and it handles multiple layers.

An example solution:
The Australian Bureau of Statistics publishes a number of Australian Statistical Geography Standard datasets:
http://www.abs.gov.au/websitedbs/D3310114.nsf/home/ABS+Geography+Publications
This project includes a sample implementation of thinning for four non-ABS structures (Postal Area, Local Government
Area, State Electoral Division, Commonwealth Electoral Division). The sample accesses data in a PostGIS database, so the
ABS datasets must be loaded into PostGIS tables using an external tool (eg QGIS). The sample runs in two phases. The first
(BuildTables) creates the tables for the thinned layers and also determines a best estimate of relationship between Postal
Area and the other tables. The second phase (ThinAbsTables) reverse engineers the topology, thins edges, reassembles polygons
and creates a geojson version of each polygon.
