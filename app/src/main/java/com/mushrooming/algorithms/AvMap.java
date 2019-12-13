package com.mushrooming.algorithms;

import com.mushrooming.base.App;
import com.mushrooming.base.Position;

import java.util.Collection;


/**
 * Created by piotrek on 04.11.17.
 */

public class AvMap {

    // https://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-meters/2964
    public static double XSCALE = 111111; // approx 111111 meters for one degree
    public static double YSCALE = 111111; // approx 111111*cos(xGPS) is one degree

    public int size = 905;
    public int center = size/2;

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
        //xpos = size/2;
        //ypos = size/2;
        xmin = center;
        xmax = center;
        ymin = center;
        ymax = center;
        //availableTerrain[(int)xpos][(int)ypos] = true;
    }

    // only for tests
    public void setZeroPos() {
        positionGPSofCenter = new Position(0,0);
    }

    public boolean availableTerrain(int i, int j) {
        return availableTerrain[i][j];
    }

    private void updateRanges(int newx, int newy) {
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
        if (xdiff*3 > 2*size || ydiff*3 > 2*size) {
            boolean[][] newAvTer = new boolean[2*size-1][2*size-1];
            int diff = (2*size - 1)/2 - size/2;
            for (int i=0; i<size; ++i) {
                for (int j=0; j<size; ++j) {
                    newAvTer[diff+i][diff+j] = availableTerrain[i][j];
                }
            }
            availableTerrain = newAvTer;
            size *= 2;
            center = size / 2;
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
        int x1 = relativeToCenter.getIntX() + center, y1 = relativeToCenter.getIntY() + center;
        if(shouldIgnore(x1,y1)) return;
        updateRanges(x1, y1);
        availableTerrain[x1][y1] = true;
    }


    public void markCenterRelativeMapPositions(Collection<MapPosition> relativeToCenterList) {
        for (MapPosition p : relativeToCenterList) {
            markCenterRelativeMapPosition(p);
        }
    }


    public void markPosition(Position posGPS) {

        MapPosition centerRelative = getCenterRelativeMapPositionFromGPS(posGPS);

        int x1 = centerRelative.getIntX();
        int y1 = centerRelative.getIntY();

        if(shouldIgnore(x1,y1)) return;

        updateRanges(x1, y1);
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

    public MapPosition getRelativeToCurrentMapPosition(MapPosition relativeToCenter) {
        if (relativeToCenter == null) return null;
        return new MapPosition(relativeToCenter.getX()-center, relativeToCenter.getY()-center);
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

        if (x1_< size/6 || x1_ > (5*size)/6 || y1_ < size/6 || y1_ > (5*size)/6) {

            // recenter map on this position

            positionGPSofCenter = posGPS;

            int xdelta = x1_ - center;
            int ydelta = y1_ - center;
            int x1,x2,y1,y2,xd,yd;

            // determine direction of offset and thus sequence of map update
            if (xdelta > 0) {
                x1 = Basic.max(xmin-xdelta, 0);
                x2 = xmax;
                xd = 1;
            } else {
                x1 = Basic.min(xmax-xdelta, size);
                x2 = xmin;
                xd = -1;
            }
            if (ydelta > 0) {
                y1 = Basic.max(ymin-ydelta, 0);
                y2 = ymax;
                yd = 1;
            } else {
                y1 = Basic.min(ymax-ydelta, size);
                y2 = ymin;
                yd = -1;
            }

            // update map without copying, writing on fields whose data will be out of map range first
            for (int i=x1; i!=x2; i+=xd) {
                for (int j=y1; j!=y2; j+=yd) {
                    if (notIn(i-xdelta) || notIn(j-ydelta)) availableTerrain[i][j] = false;
                    else availableTerrain[i][j] = availableTerrain[i-xdelta][j-ydelta];
                }
            }

            xmin = Basic.max(xmin-xdelta, 0);
            xmax = Basic.min(xmax-xdelta, size);
            ymin = Basic.max(ymin-ydelta, 0);
            ymax = Basic.min(ymax-ydelta, size);

        }

    }

}
