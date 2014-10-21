/*
 * Copyright 2014 MovingBlocks
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
package org.terasology.tutorialWorldGeneration;

import org.terasology.math.Region3i;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector3i;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.chunks.CoreChunk;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.WorldRasterizer;

public class HouseRasterizer implements WorldRasterizer {
    Block stone;

    @Override
    public void initialize() {
        stone = CoreRegistry.get(BlockManager.class).getBlock("Core:Stone");
    }

    @Override
    public void generateChunk(CoreChunk chunk, Region chunkRegion) {
        HouseFacet houseFacet = chunkRegion.getFacet(HouseFacet.class);
        for (Vector3i position : houseFacet.getWorldRegion()) {
            if (houseFacet.getWorld(position)) {
                // there should be a house here
                // create a couple 3d regions to help iterate through the cube shape, inside and out
                Vector3i centerHousePosition = position.clone();
                centerHousePosition.add(0, 4, 0);
                Region3i walls = Region3i.createFromCenterExtents(centerHousePosition, 4);
                Region3i inside = Region3i.createFromCenterExtents(centerHousePosition, 3);

                // loop through each of the positions in the cube, ignoring the is
                for (Vector3i newBlockPosition : walls) {
                    if (chunkRegion.getRegion().encompasses(newBlockPosition)
                            && !inside.encompasses(newBlockPosition)) {
                        chunk.setBlock(TeraMath.calcBlockPos(newBlockPosition), stone);
                    }
                }
            }
        }
    }
}
