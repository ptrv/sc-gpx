/*
    GPX file parser.

    Author: Peter Vasil <mail@petervasil.net>
    2013-02-10
*/

// http://wiki.openstreetmap.org/wiki/Mercator#Spherical_Mercator
Mercator {
    classvar r_major = 6378137.0;
    classvar r_minor = 6356752.3142;

    *lon2x { arg lon;
        ^(r_major * lon.degrad);
    }

    *lat2y { arg lat;
        var temp, es, eccent, phi, sinphi;
        var con, com, ts, y;
        if(lat > 89.5, {
            lat = 89.5;
        });
        if(lat < -89.5, {
            lat = -89.5;
        });
        temp = r_minor / r_major;
        es = 1.0 - (temp * temp);
        eccent = es.sqrt;
        phi = lat.degrad;
        sinphi = phi.sin;
        con = eccent * sinphi;
        com = 0.5 * eccent;
        con = pow(((1.0 - con) / (1.0 + con)), com);
        ts = ((0.5 * ((pi*0.5) - phi)).tan)/con;
        y = 0 - r_major * ts.log;
        ^y;
    }
}

GPXPoint {
    var <lat, <lon, <>ele, <>time;
    var <>timeDate;
    var <>mercX, <>mercY;

    var dateRegex = "([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})Z";
    var msdateRegex = "([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2}).([0-9]{3})Z";

    *new { |lat=0.0, lon=0.0, ele=0.0, time=""|
        ^super.newCopyArgs(lat, lon, ele, time).init;
    }

    *newFromXml { |xmlTrkpt|
        var vLat, vLon, vEle, vTime;
        vLat = xmlTrkpt.getAttribute("lat");
        vLon = xmlTrkpt.getAttribute("lon");
        // (vLat ++ " " ++ vLon).postln;
        xmlTrkpt.getChildNodes.do { |node|
            if(node.getTagName == "ele", {
                vEle = node.getText;
            });
            if(node.getTagName == "time", {
                vTime = node.getText;
            });
        };
        ^GPXPoint.new(vLat.asFloat, vLon.asFloat, vEle.asFloat, vTime);
    }

    init {
        var timeStrArray;
        var useDate=False;
        switch (time.size,
            {20},{
                timeStrArray = time.findRegexp(dateRegex);
                useDate = True;
            },
            {24},{
                timeStrArray = time.findRegexp(msdateRegex);
                useDate = True;
            });
        if(useDate == True, {
            timeDate = Date.new(
                timeStrArray[1][1].asInteger,
                timeStrArray[2][1].asInteger,
                timeStrArray[3][1].asInteger,
                timeStrArray[4][1].asInteger,
                timeStrArray[5][1].asInteger,
                timeStrArray[6][1].asInteger,
                0, 0
            );
        });
        this.mercX = Mercator.lon2x(this.lon);
        this.mercY = Mercator.lat2y(this.lat);
    }

    lat_ { |val|
        this.lat = val;
        this.mercY = Mercator.lat2y(val);
    }

    lon_ { |val|
        this.lon = val;
        this.mercX = Mercator.lon2x(val);
    }
}

GPXTrack : List {

    var <>name;

    *newFromXml { |xmltrk|
        var tmpTrack = List.new;
        var tmpName = "";
        xmltrk.getChildNodes.do { |node|
            if(node.getTagName == "name", {
                tmpName = node.getText;
            });
            if(node.getTagName == "trkseg", {
                tmpTrack.add(GPXTrackSeg.newFromXml(node));
            });
        };
        ^GPXTrack.newUsing(tmpTrack).name_(tmpName);
    }

    addTrackSeg { |trkseg|
        this.add(trkseg);
    }

    removeTrackSeg { |trkseg|
        this.remove(trkseg);
    }

    getTrackSeg { |trkSegIdx|
        ^this.at(trkSegIdx);
    }
}

GPXTrackSeg : List {

    *newFromXml { |xmlTrkSeg|
        var tmpTrkSeg = List.new;
        xmlTrkSeg.getChildNodes.do { |node|
            if(node.getTagName == "trkpt", {
                tmpTrkSeg.add(GPXPoint.newFromXml(node))
            });
        };
        ^GPXTrackSeg.newUsing(tmpTrkSeg);
    }

    addPoint { |trkpt|
        this.add(trkpt);
    }

    removePoint { |trkpt|
        this.remove(trkpt);
    }

    getPoint { |trkptIdx|
        ^this.at(trkptIdx);
    }
}

GPXWayPoint {

    var <>lat, <>lon;
    var <>name;
    var <>type;

    *new {|lat=0.0, lon=0.0, name="", type=""|
        ^super.newCopyArgs(lat, lon, name, type).init;
    }

    *newFromXml { |xmlTrkpt|
        var vLat, vLon, vName="", vType="";

        vLat = xmlTrkpt.getAttribute("lat").asFloat;
        vLon = xmlTrkpt.getAttribute("lon").asFloat;

        xmlTrkpt.getChildNodes.do { |node|
            case {node.getTagName == "name"}{
                vName = node.getText;
            }
            {node.getTagName == "type"}{
                vType = node.getText;
            }
        };
        ^GPXWayPoint.new(vLat, vLon, vName, vType);
    }

    init {
    }
}

GPXFile {

    var <>tracks;
    var <>wayPoints;

    var <domDocument, <domRoot;

    *new {
        ^super.new.init;
    }

    init {
        // var tarcks = List.new;
        // var wayPoints = List.new;
    }

    parse { |gpxPath|
        var document, root;
        tracks = List.new;
        wayPoints = List.new;

        document = DOMDocument.new;
        document.read(File(gpxPath, "r"));

        root = document.getDocumentElement;

        root.getChildNodes.do({ |node|
            if(node.getTagName == "trk",{
                // this.addTrack(GPXTrack.newFromXml(node));
                tracks.add(GPXTrack.newFromXml(node));
            });
            if(node.getTagName == "wpt", {
                this.addWayPoint(GPXWayPoint.newFromXml(node));
            });
        });
    }

    addTrack { |trk|
        this.tracks.add(trk);
    }

    removeTrack { |trk|
        this.tracks.remove(trk);
    }

    getTrack { |trkIdx|
        ^this.tracks.at(trkIdx);
    }

    getTrackCount {
        ^this.tracks.size;
    }

    addWayPoint { |wpt|
        this.wayPoints.add(wpt);
    }

    removeWayPoint{ |wpt|
        this.wayPoints.remove(wpt);
    }

    getWayPoint{ |wptIdx|
        ^this.wayPoints.at(wptIdx);
    }

    getWayPointCount {
        ^this.wayPoints.size;
    }

    draw {
        "not implemented yet!";
    }
}
