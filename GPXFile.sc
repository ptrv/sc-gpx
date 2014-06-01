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
        var con, com, ts;
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
        ^(0 - r_major * ts.log);
    }
}

GPXPoint {
    var <lat, <lon, <>ele, <>time;
    var <>timeDate;
    var <>mercX, <>mercY;

    var dateRegex = "([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})Z";
    var msdateRegex = "([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2}).([0-9]{3})Z";

    *new { arg lat=0.0, lon=0.0, ele=0.0, time="";
        ^super.newCopyArgs(lat, lon, ele, time).init;
    }

	*newFromXml { arg xmlTrkpt;
        var vLat, vLon, vEle, vTime;
        vLat = xmlTrkpt.getAttribute("lat");
        vLon = xmlTrkpt.getAttribute("lon");
        // (vLat ++ " " ++ vLon).postln;
        xmlTrkpt.getChildNodes.do { arg node;
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
        var useDate = false;
        switch (time.size,
            {20},{
                timeStrArray = time.findRegexp(dateRegex);
                useDate = true;
            },
            {24},{
                timeStrArray = time.findRegexp(msdateRegex);
                useDate = true;
            },{
                timeDate = Date.newFromRawSeconds(time.asInt);
            });
        if(useDate == true, {
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

    *newFromXml { arg xmltrk;
        var tmpTrack = List.new;
        var tmpName = "";
        xmltrk.getChildNodes.do { arg node;
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

    *newFromXml { arg xmlTrkSeg;
        var tmpTrkSeg = List.new;
        xmlTrkSeg.getChildNodes.do { arg node;
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

	*new { arg lat=0.0, lon=0.0, name="", type="";
        ^super.newCopyArgs(lat, lon, name, type).init;
    }

	*newFromXml { arg xmlTrkpt;
        var vLat, vLon, vName="", vType="";

        vLat = xmlTrkpt.getAttribute("lat").asFloat;
        vLon = xmlTrkpt.getAttribute("lon").asFloat;

        xmlTrkpt.getChildNodes.do { arg node;
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

    var gpxFilePath;

    *new {
        ^super.new.init;
    }

    init {
    }

	read { arg gpxPath;
        var s = String.readNew(File(gpxPath, "r"));
		gpxFilePath = gpxPath;
		this.parse(s);
	}

    parse { arg gpxXml;
        var document, root;
        tracks = List.new;
        wayPoints = List.new;

        document = DOMDocument.new.parseXML(gpxXml);

        root = document.getDocumentElement;

        root.getChildNodes.do({ arg node;
            if(node.getTagName == "trk",{
                tracks.add(GPXTrack.newFromXml(node));
            });
            if(node.getTagName == "wpt", {
                wayPoints.add(GPXWayPoint.newFromXml(node));
            });
        });
        domDocument = document;
        domRoot = root;
    }

    normalizePoints { arg pts;
        var points = List[];
        var minX, maxX, minY, maxY;
        var deltaX, deltaY;

        minX = 10000000000.0;
        maxX = -10000000000.0;
        minY = 10000000000.0;
        maxY = -10000000000.0;
        pts.do { arg pt;
            if(pt.mercX < minX, {minX = pt.mercX});
            if(pt.mercX > maxX, {maxX = pt.mercX});
            if(pt.mercY < minY, {minY = pt.mercY});
            if(pt.mercY > maxY, {maxY = pt.mercY});
        };
        // ("min/max x: "++minX++"/"++maxX++" y: "++minY++"/"++maxY).postln;

        deltaX = maxX - minX;
        deltaY = maxY - minY;
        if(deltaX < deltaY, {
            minX = minX - (deltaY - deltaX)/2.0;
            maxX = maxX + (deltaY - deltaX)/2.0;
        },{
            if(deltaX > deltaY, {
                minY = minY - ((deltaX - deltaY)/2.0);
                maxY = maxY + ((deltaX - deltaY)/2.0);
            });
        });
        // ("min/max x: "++minX2++"/"++maxX2++" y: "++minY2++"/"++maxY2).postln;

		pts.do { arg pt;
            var x = pt.mercX;
            var y = pt.mercY;
            x = (x - minX) / (maxX - minX);
            y = (y - minY) / (maxY - minY);
            points.add(Point(x, y));
        };
        ^points;
    }

    getAllPoints {
        var points = List[];
        tracks.do { arg trk;
            trk.do { arg seg;
				points = points ++ seg;
            };
        };
        ^points;
    }

    show { arg winWidth=500, winHeight=500;
        var window, gpxView, points, pointsN, pointsScreen, which;
        var viewW, viewH, mousepoint, mousedist;

        points = this.getAllPoints;

        pointsN = this.normalizePoints(points);

        window = Window("GPX file: "++gpxFilePath, Rect(128, 64, winWidth, winHeight));
        gpxView = UserView(window,window.view.bounds.insetBy(10,10));
        viewW = gpxView.bounds.width;
        viewH = gpxView.bounds.height;

        pointsScreen = pointsN.collect({ arg pt;
            Rect.aboutPoint(Point(pt.x * viewW, viewH - (pt.y * viewH)), 2, 2)
        });

		gpxView.drawFunc_({ arg me;
            Pen.fillColor = Color.grey(0.1);
            Pen.fillRect(me.bounds.moveTo(0,0));
            Pen.width = 1;
            pointsScreen.do { arg c, i;
                Pen.fillColor = Color.white;
                Pen.fillOval(c);
            };
            Pen.strokeColor = Color.black;
            if(which.notNil) {
                Pen.width = 2;
                Pen.strokeColor = Color.red;
                Pen.strokeOval(pointsScreen[which]);
                Pen.fillColor = Color.white;
                Pen.stringAtPoint(
                    points[which].lat.asString++", "++points[which].lon.asString,
                    Point(
                        pointsScreen[which].origin.x,
                        pointsScreen[which].origin.y-15));
            };
            Pen.stroke;
        });
        gpxView.mouseDownAction_({ arg v, x, y;
            mousepoint = Point(x,y);
            which = nil;
            which = pointsScreen.detectIndex { arg c, i;  c.containsPoint(mousepoint) };
            if(which.notNil) {
                mousedist = mousepoint - (pointsScreen[which].origin);
                v.doAction;
            };
        });
        gpxView.mouseUpAction_({ arg v, x, y;
            which = nil;
            v.refresh;
        });
        gpxView.action_({
            [which, points[which].lat, points[which].lon].postln;
            gpxView.refresh;
        });

        window.front;
    }
}

+ Date {
    *newFromRawSeconds { arg rawSeconds;
        var vYear, vMonth, vDay, vHour, vMin, vSec, vWDay;
        var dayclock, dayno, yearr = 1970;
        var leapyear, yearsize, ytab;

        leapyear = { arg val;
            if (val % 400 == 0, {
                true;
            },{
                if(val % 100 == 0, {
                    false;
                },{
                    if(val % 4 == 0, {
                        true;
                    },{
                        false;
                    });
                });
            });
        };
        yearsize = { arg val;
            if(leapyear.value(val) == true, {366},{365});
        };

        ytab = { arg val;
            if(leapyear.value(val) == true, {
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