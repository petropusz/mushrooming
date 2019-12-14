package com.mushrooming.algorithms;

import com.mushrooming.base.App;
import com.mushrooming.base.Position;

import java.util.Collection;


/**
 * Created by piotrek on 04.11.17.
 */

public class AvMap {

    public static double recenterCriterium = 0.11; // part of size from margin to recenter the map
                                                   // this + resizeCriterium HAS TO be < 0.5
                                                   // otherwise with 2 points marked in turns (stable positions):
                                                   // in center and almost resizeCriterium to the north
                                                   // this class would recenter every time
                                                   // ALSO need recenter on resize to be safe from this
    public static double resizeCriterium = 0.38; // part of size that difference on x or y between
                                                 // furthest points has to be to enlarge *2
                                                 // has to be < 0.5, because otherwise
                                                 // recentering could move some device out of map

    // https://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-meters/2964
    public static double XSCALE = 111111; // approx 111111 meters for one degree
    public static double YSCALE = 111111; // approx 111111*cos(xGPS) is one degree
    public static int startsize = 905;

    private int size = startsize;
    private int center = size/2;

    private boolean[][] availableTerrain = new boolean[size][size];

    // TODO maybe add option to mark obstacles?
    // but GPS accuracy of determining on which side of an obstacle someone is is a problem in using that

    private Position positionGPSofCenter; // use latitude and longitude as int coordinate on plane
    //private int xpos, ypos; // x and y pos of center - always size/2, except for recenter
    private int xmin, xmax, ymin, ymax; // range of used area of a map

    // all functions (except getRelativeToCurrentMapPosition) take relative positions as arguments!

    public AvMap() {
        Position pos = null;
        if (App.instance().getLocationService() != null) {
            pos = App.instance().getLocationService().getLastPosition();
        }
        if (pos == null) pos = new Position(0,0);
        positionGPSofCenter = pos;
        xmin = center;
        xmax = center;
        ymin = center;
        ymax = center;
        //availableTerrainAbsolute[(int)xpos][(int)ypos] = true;
    }

    public int getSize() {
        return size;
    }

    public int getCenter() {
        return center;
    }

    // only for tests
    public void setZeroPos() {
        positionGPSofCenter = new Position(0,0);
    }

    public boolean availableTerrainAbsolute(int i, int j) {
        return availableTerrain[i][j];
    }

    public boolean availableTerrain(int i, int j) {
        return availableTerrain[i+center][j+center];
    }

    // position needed not to accumulate errors when recentering needed
    private void updateRanges(int newx, int newy, Position posGPS) {
        xmin = Basic.min(xmin, newx);
        xmax = Basic.max(xmax, newx);
        ymin = Basic.min(ymin, newy);
        ymax = Basic.max(ymax, newy);
        // if any of the ranges is more than 2/3 of map,
        // increase the map twice
        // this shouldn't happen and even more should not happen more than ~1-2 times
        // as when team members move for a big distance but keep close
        // map will only be recentered
        int xdiff = xmax - xmin;
        int ydiff = ymax - ymin;
        if ((double)xdiff > resizeCriterium*size || (double)ydiff > resizeCriterium*size) {
            int needed = Basic.max(xdiff,ydiff);
            int oldsize = size;
            int oldcenter = center;
            // as will later recenter here (see comments below),
            // need to update GPS pos of center
            positionGPSofCenter = posGPS;
            while (needed > resizeCriterium*size) {
                size = size * 2 - 1;
                center = size / 2;
            }
            boolean[][] newAvTer = new boolean[size][size];
            int diffx = center - newx;
            int diffy = center - newy;
            // also recenter to keep invariants described near resizeCriterium declaration
            // 'regular' recenter won't have recentering conditions met after resize
            for (int i=0; i<oldsize; ++i) {
                for (int j=0; j<oldsize; ++j) {
                    newAvTer[diffx+i][diffy+j] = availableTerrain[i][j];
                }
            }
            availableTerrain = newAvTer;
            // changing xyminmax so that won't keep map of regions from which users moved
            // if they are close to each other in the new region
            // shouldn't move map out of someone's position with that
            // as one would have to move very quickly
            // in a very unprobable case sth like that happens
            // would lose some information about terrain availability there
            xmin = center;
            xmax = center;
            ymin = center;
            ymax = center;
        }
    }

    // check if should be ignored as incorrect measurement
    private boolean shouldIgnore(int newx, int newy) {
        int xdiff = xmax - xmin;
        int ydiff = ymax - ymin;
        return
           (Basic.abs(newx-xmin) >  xdiff+center ||
            Basic.abs(newx-xmax) >  xdiff+center ||
            Basic.abs(newy-ymin) >  ydiff+center ||
            Basic.abs(newy-ymax) >  ydiff+center);
            // impossible to move that fast between measurements
            // if no data for some time user should be considered as disconnected
            // and assembly should be ordered
    }

    public void markCenterRelativeMapPosition(MapPosition relativeToCenter) {
        Position gps = getNonRelativeGPSposition(relativeToCenter);
        MapPosition absolute = getAbsoluteMapPositionFromCenterRelative(relativeToCenter);
        int x1 = absolute.getIntX(), y1 = absolute.getIntY();
        if(shouldIgnore(x1,y1)) return;
        updateRanges(x1, y1, gps);
        // absolute could have changed, and could have been recentered
        // use GPS position to conveniently deal with that
        absolute = getAbsoluteMapPositionFromCenterRelative(getCenterRelativeMapPositionFromGPS(gps));
        x1 = absolute.getIntX();
        y1 = absolute.getIntY();
        availableTerrain[x1][y1] = true;
        recenter(getNonRelativeGPSposition(relativeToCenter));  //every position marking does this
    }


    public void markCenterRelativeMapPositions(Collection<MapPosition> relativeToCenterList) {
        for (MapPosition p : relativeToCenterList) {
            markCenterRelativeMapPosition(p);
        }
    }


    public void markPosition(Position posGPS) {

        MapPosition relative = getCenterRelativeMapPositionFromGPS(posGPS);
        MapPosition absolute = getAbsoluteMapPositionFromCenterRelative(relative);

        int x1 = absolute.getIntX();
        int y1 = absolute.getIntY();

        if(shouldIgnore(x1,y1)) return;

        updateRanges(x1, y1, posGPS);
        // absolute could have changed, and could have been recentered
        absolute = getAbsoluteMapPositionFromCenterRelative(getCenterRelativeMapPositionFromGPS(posGPS));
        x1 = absolute.getIntX();
        y1 = absolute.getIntY();
        availableTerrain[x1][y1] = true;

        recenter(posGPS);  //every position marking does this
    }


    public void markPositions(Collection<Position> posGPSlist) {
        for (Position p : posGPSlist) {
            markPosition(p);
        }
    }

    public MapPosition getCenterRelativeMapPositionFromAbsolute(MapPosition absolute) {
        return new MapPosition(absolute.getX() - center, absolute.getY() - center);
    }

    public MapPosition getAbsoluteMapPositionFromCenterRelative(MapPosition centerRel) {
        return new MapPosition(centerRel.getX() + center, centerRel.getY() + center);
    }

    public MapPosition getCenterRelativeMapPositionFromGPS(Position posGPS) {
        if (posGPS == null) return null;
        double xGPS = posGPS.getX();
        double yGPS = posGPS.getY();
        return new MapPosition(
            (xGPS - positionGPSofCenter.getX())*XSCALE,
            (yGPS - positionGPSofCenter.getY())*YSCALE*Math.cos(xGPS)
        );
    }

    public Position getNonRelativeGPSposition(MapPosition centerRelative){
        double xGPS = centerRelative.getX() * (1.0/XSCALE) + positionGPSofCenter.getX();
        double yGPS = centerRelative.getY() * (1.0/ (YSCALE*Math.cos(xGPS)) ) + positionGPSofCenter.getY();
        return new Position(xGPS, yGPS);
    }

    public boolean notIn(int p) {
        return (p<0 || p>=size); //compare with defined
    }

    public void recenter(Position posGPS) {

        MapPosition absolute = getAbsoluteMapPositionFromCenterRelative(
                getCenterRelativeMapPositionFromGPS(posGPS)
        );

        int x1_ = absolute.getIntX();
        int y1_ = absolute.getIntY();

        if (x1_ < recenterCriterium*size || x1_ > (1.-recenterCriterium)*size ||
            y1_ < recenterCriterium*size || y1_ > (1.-recenterCriterium)*size) {

            // recenter map on this position

            positionGPSofCenter = posGPS;

            int xdelta = x1_ - center;
            int ydelta = y1_ - center;
            int x1,x2,y1,y2,xd,yd;

            // determine direction of offset and thus sequence of map update
            if (xdelta > 0) {
                x1 = 0; //Basic.max(xmin-xdelta, 0);
                x2 = size; //Basic.max(xmax-xdelta, 0);
                xd = 1;
            } else {
                x1 = size-1; //Basic.min(xmax-xdelta, size);
                x2 = -1; //Basic.min(xmin-xdelta, size);
                xd = -1;
            }
            if (ydelta > 0) {
                y1 = 0; //Basic.max(ymin-ydelta, 0);
                y2 = size; //Basic.max(ymax-ydelta, 0);
                yd = 1;
            } else {
                y1 = size-1; //Basic.min(ymax-ydelta, size);
                y2 = -1; //Basic.min(ymin-ydelta, size);
                yd = -1;
            }

            // update map without copying, writing on fields whose data will be out of map range first
            for (int i=x1; i!=x2; i+=xd) {
                for (int j=y1; j!=y2; j+=yd) {
                    if (notIn(i+xdelta) || notIn(j+ydelta)) availableTerrain[i][j] = false;
                    else availableTerrain[i][j] = availableTerrain[i+xdelta][j+ydelta];
                }
            }

            xmin = Basic.max(xmin-xdelta, 0);
            xmax = Basic.min(xmax-xdelta, size);
            ymin = Basic.max(ymin-ydelta, 0);
            ymax = Basic.min(ymax-ydelta, size);

        }

    }

}
