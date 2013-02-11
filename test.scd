/*
    Test file for GPXFile.sc.

    Peter Vasil <mail@petervasil.net>
    2013-02-11
*/

(
var gpx = GPXFile.new.parse(Document.current.dir ++ "/test.gpx");

gpx.getTrackCount.do { |i|
    gpx.getTrack(i).do { |seg|
        seg.do { |pt|
            (""++pt.lat++" "++pt.lon).postln;
            // (""++pt.mercX++" "++pt.mercY).postln;
        };
    };
};
gpx.getWayPointCount.do { |i|
    var wpt = gpx.getWayPoint(i);
    (""++wpt.lat++" "++wpt.lon++" "++wpt.name++" "++wpt.type).postln;
};
)
