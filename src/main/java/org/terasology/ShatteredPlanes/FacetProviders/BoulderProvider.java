/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.ShatteredPlanes.FacetProviders;

import org.terasology.ShatteredPlanes.Facets.BiomeHeightFacet;
import org.terasology.math.TeraMath;
import org.terasology.math.geom.BaseVector2i;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector2f;
import org.terasology.utilities.procedural.BrownianNoise;
import org.terasology.utilities.procedural.PerlinNoise;
import org.terasology.utilities.procedural.SimplexNoise;
import org.terasology.utilities.procedural.SubSampledNoise;
import org.terasology.world.generation.*;
import org.terasology.world.generation.facets.SurfaceHeightFacet;
@Requires(@Facet(BiomeHeightFacet.class))
@Updates(@Facet(value = SurfaceHeightFacet.class, border=@FacetBorder(sides = 4)))
public class BoulderProvider implements FacetProvider {

    private BrownianNoise PreNoise;
    private SubSampledNoise mountainNoise1;
    private SubSampledNoise mountainNoise2;
    private SubSampledNoise noise;
    private float k = 0.05f;
    @Override
    public void setSeed(long seed) {
        PreNoise = new BrownianNoise(new PerlinNoise(seed + 25), 12);
        //PreNoise.setPersistence(0.001);
        mountainNoise1 = new SubSampledNoise(new SimplexNoise(seed - 50), new Vector2f(0.01f, 0.01f), 1);
        mountainNoise2 = new SubSampledNoise(PreNoise, new Vector2f(0.01f, 0.01f), 1);
        noise = new SubSampledNoise(new SimplexNoise(seed + 50), new Vector2f(0.001f, 0.001f), 1);
    }

    @Override
    public void process(GeneratingRegion region) {
        SurfaceHeightFacet surfaceHeightFacet = region.getRegionFacet(SurfaceHeightFacet.class);
        BiomeHeightFacet biomeHeightFacet = region.getRegionFacet(BiomeHeightFacet.class);
        Rect2i processRegion = surfaceHeightFacet.getWorldRegion();

        float[] sNoise1Values = mountainNoise1.noise(processRegion);
        float[] sNoise2Values = mountainNoise2.noise(processRegion);
        float[] sNoise3Values = noise.noise(processRegion);

        for(BaseVector2i pos : processRegion.contents()){
                int surfaceHeight = TeraMath.floorToInt(surfaceHeightFacet.getWorld(pos));
                float biomeHeight = biomeHeightFacet.getWorld(pos);
                float CanyonBaseHeight=10*(biomeHeight*biomeHeight-1f);
                float CanyonHeight = 35;
                float sigma = CanyonHeight/4;
                // check if height is within this region
                float maxCanyonHeight = CanyonBaseHeight + CanyonHeight;
                if ((surfaceHeight >= region.getRegion().minY() && surfaceHeight+maxCanyonHeight <= region.getRegion().maxY())) {

                    for (int wy = surfaceHeight; (wy <= region.getRegion().maxY() && wy <= surfaceHeight + maxCanyonHeight &&
                            biomeHeight > 0.6 && biomeHeight < 4); wy++) {

                        float noiseValue1 = sNoise1Values[surfaceHeightFacet.getWorldIndex(pos)];
                        float noiseValue2 = sNoise2Values[surfaceHeightFacet.getWorldIndex(pos)];
                        float noiseValue3 = sNoise3Values[surfaceHeightFacet.getWorldIndex(pos)];


                        // TODO: check for overlap
                        float noiseVal = Math.abs(noiseValue1/3 + noiseValue2/3 + noiseValue1/3);
                        float probability = gauss(CanyonHeight/2-wy,sigma)/1.5f;
                        if (noiseVal > (1 - probability)) {
                            surfaceHeightFacet.setWorld(pos, (float) wy+CanyonBaseHeight);
                        }
                    }
                }
        }
    }




    private float gauss(float x, float sigma) {
        return (float) /*1/(float)Math.sqrt(2*Math.PI*sigma * sigma)**/(float) Math.exp(-x * x / (2*sigma * sigma));
    }

}

