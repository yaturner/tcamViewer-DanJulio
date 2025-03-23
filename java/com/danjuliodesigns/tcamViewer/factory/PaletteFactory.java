package com.danjuliodesigns.tcamViewer.factory;

import com.danjuliodesigns.tcamViewer.pallete.Arctic;
import com.danjuliodesigns.tcamViewer.pallete.Banded;
import com.danjuliodesigns.tcamViewer.pallete.Blackhot;
import com.danjuliodesigns.tcamViewer.pallete.DoubleRainbow;
import com.danjuliodesigns.tcamViewer.pallete.Fusion;
import com.danjuliodesigns.tcamViewer.pallete.Gray;
import com.danjuliodesigns.tcamViewer.pallete.Ironblack;
import com.danjuliodesigns.tcamViewer.pallete.Isotherm;
import com.danjuliodesigns.tcamViewer.pallete.Rainbow;
import com.danjuliodesigns.tcamViewer.pallete.Sepia;

public class PaletteFactory {
    private final String[] paletteNames = {
            "Arctic",
            "Banded",
            "Blackhot",
            "DoubleRainbow",
            "Fusion",
            "Gray",
            "Ironblack",
            "Isotherm",
            "Rainbow",
            "Sepia"
    };
    private final int[][][] palettes = {
            Arctic.palette,
            Banded.palette,
            Blackhot.pallete,
            DoubleRainbow.palette,
            Fusion.palette,
            Gray.palette,
            Ironblack.palette,
            Isotherm.palette,
            Rainbow.palette,
            Sepia.palette
    };

    public String[] getPaletteNames() {
        return paletteNames;
    }

    public String getPaletteName(int index) {
        if(index < paletteNames.length) {
            return paletteNames[index];
        } else {
            return null;
        }
    }

    public int[][] getPaletteByName(final String name) {
        for(int index=0; index<paletteNames.length; index++) {
            if(paletteNames[index].equalsIgnoreCase(name)) {
                return palettes[index];
            }
        }
        return null;
    }
}