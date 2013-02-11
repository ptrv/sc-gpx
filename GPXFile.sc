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
            },{
                timeDate = Date.newFromRawSeconds(time.asInt);
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
        mercX = Mercator.lon2x(lon);
        mercY = Mercator.lat2y(lat);
    }

    lat_ { arg val;
        lat = val;
        mercY = Mercator.lat2y(val);
    }

    lon_ { arg val;
        lon = val;
        mercX = Mercator.lon2x(val);
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
        tracks.add(trk);
    }

    removeTrack { |trk|
        tracks.remove(trk);
    }

    getTrack { |trkIdx|
        ^tracks.at(trkIdx);
    }

    getTrackCount {
        ^tracks.size;
    }

    addWayPoint { |wpt|
        wayPoints.add(wpt);
    }

    removeWayPoint{ |wpt|
        wayPoints.remove(wpt);
    }

    getWayPoint{ |wptIdx|
        ^wayPoints.at(wptIdx);
    }

    getWayPointCount {
        ^wayPoints.size;
    }

    draw {
        "not implemented yet!";
    }
}

+ Date {
    *newFromRawSeconds { arg rawSeconds;
        var vYear, vMonth, vDay, vHour, vMin, vSec, vWDay;
        var dayclock, dayno, yearr = 1970;
        var leapyear, yearsize, ytab;

        leapyear = {|val|
            if (val % 400 == 0, {
                True;
            },{
                if(val % 100 == 0, {
                    False;
                },{
                    if(val % 4 == 0, {
                        True;
                    },{
                        False;
                    });
                });
            });
        };
        yearsize = {|val|
            if(leapyear.value(val) == True, {366},{365});
        };

        ytab = {|val|
            if(leapyear.value(val) == True, {
                [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
            },{
                [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
            });
        };

        dayclock = rawSeconds % 86400; //secs day
        dayno = (rawSeconds / 86400).asInteger;

        vSec = dayclock % 60;
        vMin = ((dayclock % 3600) / 60).asInteger;
        vHour = dayclock / 3600;
        vWDay = (dayno + 4) % 7;
        while({dayno >= yearsize.value(yearr)}, {
            dayno = dayno - yearsize.value(yearr);
            yearr = yearr + 1;
        });

        vYear = yearr;
        vMonth = 0;
        while({dayno >= ytab.value(yearr).at(vMonth)}, {
            dayno = dayno - ytab.value(yearr).at(vMonth);
            vMonth = vMonth + 1;
        });

        vDay = (dayno + 1).asInteger;

        ^Date(vYear, vMonth, vDay, vHour, vMin, vSec, vWDay, rawSeconds);
    }
}