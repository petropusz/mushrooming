package com.mushrooming.algorithms;

import com.mushrooming.base.Position;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by piotrek on 22.11.17.
 */
public class AvMapTest {

    double eps = 1e-8;

    @Test
    public void markMapPosition() throws Exception {
        AvMap av = new AvMap();
        double x = Basic.min((0.5-AvMap.recenterCriterium)*av.getSize()*0.4, 0.6*AvMap.resizeCriterium*av.getSize());
        double y = Basic.min((0.5-AvMap.recenterCriterium)*av.getSize()*0.44, 0.6*AvMap.resizeCriterium*av.getSize());
        MapPosition mp = new MapPosition(x,y);
        av.markCenterRelativeMapPosition(mp);
        assertEquals(true, av.availableTerrain(mp.getIntX(),mp.getIntY()));
    }

    @Test
    public void markGPSPosition() throws Exception {
        AvMap av = new AvMap();
        double x = Basic.min((0.5-AvMap.recenterCriterium)*av.getSize()*0.4, 0.6*AvMap.resizeCriterium*av.getSize());
        double y = Basic.min((0.5-AvMap.recenterCriterium)*av.getSize()*0.44, 0.6*AvMap.resizeCriterium*av.getSize());
        MapPosition mp = new MapPosition(x,y);
        av.markPosition(av.getNonRelativeGPSposition(mp));
        assertEquals(true, av.availableTerrain(mp.getIntX(),mp.getIntY()));
    }

    @Test
    public void testPositionTranslation() throws Exception {
        AvMap av = new AvMap();
        double x = Basic.min((0.5-AvMap.recenterCriterium)*av.getSize()*0.4, 0.6*AvMap.resizeCriterium*av.getSize());
        double y = Basic.min((0.5-AvMap.recenterCriterium)*av.getSize()*0.44, 0.6*AvMap.resizeCriterium*av.getSize());
        MapPosition mp = new MapPosition(x, y);
        MapPosition mp2 = new MapPosition(x+av.getCenter(),y+av.getCenter());
        MapPosition mpTranslated = av.getAbsoluteMapPositionFromCenterRelative(mp);
        assertTrue(Basic.abs(mp2.getX() - mpTranslated.getX()) < eps);
        assertTrue(Basic.abs(mp2.getY() - mpTranslated.getY()) < eps);
        MapPosition mp2translated = av.getCenterRelativeMapPositionFromAbsolute(mp2);
        assertTrue(Basic.abs(mp.getX() - mp2translated.getX()) < eps);
        assertTrue(Basic.abs(mp.getY() - mp2translated.getY()) < eps);
        MapPosition toGPSandBack = av.getCenterRelativeMapPositionFromGPS(
                av.getNonRelativeGPSposition(mp)
        );
        assertTrue(Basic.abs(mp.getX() - toGPSandBack.getX()) < eps);
        assertTrue(Basic.abs(mp.getY() - toGPSandBack.getY()) < eps);
    }

    @Test
    public void markPositions() throws Exception {
        AvMap av = new AvMap();
        double x = Basic.min((0.5-AvMap.recenterCriterium)*av.getSize()*0.4, 0.6*AvMap.resizeCriterium*av.getSize());
        double y = Basic.min((0.5-AvMap.recenterCriterium)*av.getSize()*0.44, 0.6*AvMap.resizeCriterium*av.getSize());
        MapPosition mp1 = new MapPosition(x+1, x+2);
        MapPosition mp2 = new MapPosition(x-1, x+3);
        MapPosition mp3 = new MapPosition(x/2, y/2+1);
        ArrayList<MapPosition> list = new ArrayList<>();
        list.add(mp1);
        list.add(mp2);
        list.add(mp3);
        av.markCenterRelativeMapPositions(list);
        for (MapPosition mp : list) {
            assertEquals(true, av.availableTerrain(mp.getIntX(), mp.getIntY()));
        }
    }


    @Test
    public void recenter() throws Exception {
        AvMap av;
        int base = (int)((0.5-AvMap.recenterCriterium)*AvMap.startsize + 3);  // big enough to resize

        // test in every direction
        for (int i=-1; i<=1; ++i) {
            for (int j=-1; j<=1; ++j) {
                if (i == 0 && j == 0) continue;

                int x = i*base;
                int y = j*base;
                av = new AvMap();
                Position oldCenter = av.getNonRelativeGPSposition(new MapPosition(0,0));  // we get GPS coord of center
                av.markPosition(oldCenter);
                MapPosition mp = new MapPosition(x, y);
                av.recenter(av.getNonRelativeGPSposition(mp));
                MapPosition mp2 = av.getCenterRelativeMapPositionFromGPS(oldCenter);
                assertEquals(mp2.getIntX(), -mp.getIntX());  // with non-integers this is up to +-1
                assertEquals(mp2.getIntY(), -mp.getIntY());  // because of rounding and numerical errors
                assertEquals(true, av.availableTerrain(-mp.getIntX(), -mp.getIntY()));
            }
        }
    }

    @Test
    public void positionMapRecenterResize() throws Exception {
        AvMap av;
        int base = (int)(0.49*AvMap.startsize);  // big enough to recenter and also resize as
                                                 // resizeCriterium has to be noticeably < 0.5
        // test in every direction
        for (int i=-1; i<=1; ++i) {
            for (int j=-1; j<=1; ++j) {
                if (i==0 && j==0) continue;
                av = new AvMap();
                int size0 = av.getSize();
                int x = i*base;
                int y = j*base;
                av.markCenterRelativeMapPosition(new MapPosition(0,0));
                av.markCenterRelativeMapPosition(new MapPosition(3,4));
                av.markCenterRelativeMapPosition(new MapPosition(x,y));  // recentering here
                assertEquals(true, av.availableTerrain(0,0));
                assertEquals(true, av.availableTerrain(-x,-y));
                assertEquals(true, av.availableTerrain(-x+3,-y+4));
                assertTrue(av.getSize() == size0*2-1);
            }
        }
    }

}