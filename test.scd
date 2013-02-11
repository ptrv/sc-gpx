/*
    Test file for GPXFile.sc.

    Peter Vasil <mail@petervasil.net>
    2013-02-11
*/

(
g = GPXFile.new.parse(Document.current.dir ++ "/test.gpx");

g.tracks.do { |trk|
    trk.do { |seg|
        seg.do { |pt|
            (pt.lat.asString++" "++pt.lon.asString).postln;
            // (pt.mercX.asString++" "++pt.mercY.asString).postln;
        };
    };
};
g.wayPoints.do { |wpt|
    (wpt.lat.asString++" "++wpt.lon.asString++" "++wpt.name++" "++wpt.type).postln;
};
// g.show(800, 800);
g.show;
)
