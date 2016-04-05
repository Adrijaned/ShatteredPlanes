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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.terasology.ShatteredPlanes.Facets.BiomeFacet;
import org.terasology.ShatteredPlanes.ShatteredPlanesBiome;
import org.terasology.core.world.generator.facetProviders.PositionFilters;
import org.terasology.core.world.generator.facetProviders.SurfaceObjectProvider;
import org.terasology.core.world.generator.facets.FloraFacet;
import org.terasology.core.world.generator.rasterizers.FloraType;
import org.terasology.entitySystem.Component;
import org.terasology.math.geom.Vector3i;
import org.terasology.rendering.nui.properties.Range;
import org.terasology.utilities.procedural.Noise;
import org.terasology.utilities.procedural.WhiteNoise;
import org.terasology.world.biomes.Biome;
import org.terasology.world.generation.*;
import org.terasology.world.generation.facets.SeaLevelFacet;
import org.terasology.world.generation.facets.SurfaceHeightFacet;

import java.util.List;
import java.util.Map;

/**
 * Determines where plants can be placed.  Will put plants one block above the surface if it is in the correct biome.
 */
@Produces(FloraFacet.class)
@Requires({
        @Facet(SeaLevelFacet.class),
        @Facet(SurfaceHeightFacet.class),
        @Facet(BiomeFacet.class)
//    @Facet(value = DensityFacet.class, border = @FacetBorder(bottom = 1))
})
public class DefaultFloraProvider extends SurfaceObjectProvider<Biome, FloraType> implements ConfigurableFacetProvider {

    private Noise densityNoiseGen;

    private Configuration configuration = new Configuration();

    private Map<FloraType, Float> typeProbs = ImmutableMap.of(
            FloraType.GRASS, 0.85f,
            FloraType.FLOWER, 0.1f,
            FloraType.MUSHROOM, 0.05f);

    private Map<ShatteredPlanesBiome, Float> biomeProbs = ImmutableMap.<ShatteredPlanesBiome, Float>builder()
            .put(ShatteredPlanesBiome.FOREST, 0.3f)
            .put(ShatteredPlanesBiome.PLAINS, 0.2f)
            .put(ShatteredPlanesBiome.MOUNTAINS, 0.2f)
            .put(ShatteredPlanesBiome.SNOW, 0.001f)
            .put(ShatteredPlanesBiome.BEACH, 0.001f)
            .put(ShatteredPlanesBiome.OCEAN, 0f)
            .put(ShatteredPlanesBiome.RIFT, 0f)
            .put(ShatteredPlanesBiome.DESERT, 0.001f).build();

    public DefaultFloraProvider() {

        for (ShatteredPlanesBiome biome : ShatteredPlanesBiome.values()) {
            float biomeProb = biomeProbs.get(biome);
            for (FloraType type : typeProbs.keySet()) {
                float typeProb = typeProbs.get(type);
                float prob = biomeProb * typeProb;
                register(biome, type, prob);
            }
        }

        register(ShatteredPlanesBiome.BEACH, FloraType.MUSHROOM, 0);
        register(ShatteredPlanesBiome.BEACH, FloraType.FLOWER, 0);
        register(ShatteredPlanesBiome.DESERT, FloraType.MUSHROOM, 0);
        register(ShatteredPlanesBiome.SNOW, FloraType.MUSHROOM, 0);
        register(ShatteredPlanesBiome.RIFT, FloraType.MUSHROOM, 0);
    }

    /**
     * @param configuration the default configuration to use
     */
    public DefaultFloraProvider(Configuration configuration) {
        this();
        this.configuration = configuration;
    }

    @Override
    public void setSeed(long seed) {
        super.setSeed(seed);

        densityNoiseGen = new WhiteNoise(seed);
    }

    @Override
    public void process(GeneratingRegion region) {
        SurfaceHeightFacet surface = region.getRegionFacet(SurfaceHeightFacet.class);
        BiomeFacet biomeFacet = region.getRegionFacet(BiomeFacet.class);

        FloraFacet facet = new FloraFacet(region.getRegion(), region.getBorderForFacet(FloraFacet.class));

        List<Predicate<Vector3i>> filters = getFilters(region);
        populateFacet(facet, surface, biomeFacet, filters);

        region.setRegionFacet(FloraFacet.class, facet);
    }

    protected List<Predicate<Vector3i>> getFilters(GeneratingRegion region) {
        List<Predicate<Vector3i>> filters = Lists.newArrayList();

        SeaLevelFacet seaLevel = region.getRegionFacet(SeaLevelFacet.class);
        filters.add(PositionFilters.minHeight(seaLevel.getSeaLevel()));

        filters.add(PositionFilters.probability(densityNoiseGen, configuration.density));

        return filters;
    }

    @Override
    public String getConfigurationName() {
        return "Flora";
    }

    @Override
    public Component getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(Component configuration) {
        this.configuration = (Configuration) configuration;
    }

    public static class Configuration implements Component {
        @Range(min = 0, max = 1.0f, increment = 0.05f, precision = 2, description = "Define the overall flora density")
        public float density = 0.4f;
    }

}