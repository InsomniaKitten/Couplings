/*
 * Copyright (C) 2019 InsomniaKitten
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.chloedawn.couplings;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class Trapdoors {
  private static final ThreadLocal<Boolean> USE_NEIGHBORS = ThreadLocal.withInitial(() -> true);

  private Trapdoors() {
  }

  public static void used(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player, final Hand hand, final BlockHitResult hit, final ActionResult usageResult) {
    if (usageResult.isAccepted() && Couplings.areTrapdoorsEnabled() && USE_NEIGHBORS.get() && (!player.isSneaking() || Couplings.isSneakingIgnored())) {
      USE_NEIGHBORS.set(false);
      final Block block = state.getBlock();
      final boolean open = state.get(TrapdoorBlock.OPEN);
      final BlockHalf half = state.get(TrapdoorBlock.HALF);
      final Direction facing = state.get(HorizontalFacingBlock.FACING);
      final Direction opposite = facing.getOpposite();
      for (final BlockPos offset : BlockPos.iterate(
        pos.offset(facing.rotateYCounterclockwise(), Couplings.getCouplingRange()),
        pos.offset(facing.rotateYClockwise(), Couplings.getCouplingRange())
      )) {
        if (pos.equals(offset)) {
          ((BlockPos.Mutable) offset).setOffset(facing);
          if (Couplings.isUsable(world, offset, player)) {
            final BlockState mirror = world.getBlockState(offset);
            if (block == mirror.getBlock() && equals(open, half, opposite, mirror)) {
              if (Couplings.use(mirror, world, hand, player, hit, offset.toImmutable(), usageResult)) {
                USE_NEIGHBORS.set(false);
                return;
              }
            }
          }
          ((BlockPos.Mutable) offset).setOffset(opposite);
        } else if (Couplings.isUsable(world, offset, player)) {
          final BlockState other = world.getBlockState(offset);
          if (block == other.getBlock() && equals(open, half, facing, other)) {
            if (Couplings.use(other, world, hand, player, hit, offset.toImmutable(), usageResult)) {
              USE_NEIGHBORS.set(false);
              return;
            }
            ((BlockPos.Mutable) offset).setOffset(facing);
            if (Couplings.isUsable(world, offset, player)) {
              final BlockState mirror = world.getBlockState(offset);
              if (block == mirror.getBlock() && equals(open, half, opposite, mirror)) {
                if (Couplings.use(mirror, world, hand, player, hit, offset.toImmutable(), usageResult)) {
                  USE_NEIGHBORS.set(false);
                  return;
                }
              }
            }
            ((BlockPos.Mutable) offset).setOffset(opposite);
          }
        }
      }
      USE_NEIGHBORS.set(true);
    }
  }

  private static boolean equals(final boolean open, final BlockHalf half, final Direction facing, final BlockState state) {
    return open != state.get(TrapdoorBlock.OPEN) && half == state.get(TrapdoorBlock.HALF) && facing == state.get(HorizontalFacingBlock.FACING);
  }
}