package com.example.ironnest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Управление:
 *  - снаряд в руке: зарядить
 *  - пустая рука: изменить угол возвышения (4 положения)
 *  - пустая рука + Shift: повернуть орудие
 *  - красный камень (фронт сигнала) или огниво: выстрел
 */
public class ArtilleryCannonBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty ELEVATION = IntegerProperty.create("elevation", 0, 3);
    public static final BooleanProperty LOADED = BooleanProperty.create("loaded");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final int[] ANGLES = {20, 40, 60, 80};
    private static final double MUZZLE_VELOCITY = 3.0;

    public ArtilleryCannonBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ELEVATION, 1)
                .setValue(LOADED, false)
                .setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ELEVATION, LOADED, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.is(IronNestMod.ARTILLERY_SHELL.get())) {
            if (state.getValue(LOADED)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(LOADED, true), 3);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                sound(level, pos, SoundEvents.PISTON_CONTRACT);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.is(Items.FLINT_AND_STEEL)) {
            if (!level.isClientSide && fire(level, pos, state)) {
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.isEmpty()) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    Direction next = state.getValue(FACING).getClockWise();
                    level.setBlock(pos, state.setValue(FACING, next), 3);
                    player.displayClientMessage(Component.translatable("message.ironnest.facing",
                            Component.translatable("direction.ironnest." + next.getName())), true);
                } else {
                    int next = (state.getValue(ELEVATION) + 1) % ANGLES.length;
                    level.setBlock(pos, state.setValue(ELEVATION, next), 3);
                    player.displayClientMessage(Component.translatable("message.ironnest.elevation", ANGLES[next]), true);
                }
                sound(level, pos, SoundEvents.LEVER_CLICK);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            BlockState newState = state.setValue(POWERED, powered);
            level.setBlock(pos, newState, 3);
            if (powered) {
                fire(level, pos, newState);
            }
        }
    }

    private boolean fire(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(LOADED)) {
            return false;
        }
        Direction dir = state.getValue(FACING);
        double angle = Math.toRadians(ANGLES[state.getValue(ELEVATION)]);
        double horizontal = Math.cos(angle);
        double vertical = Math.sin(angle);

        double x = pos.getX() + 0.5 + dir.getStepX();
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5 + dir.getStepZ();

        ArtilleryShellEntity shell = new ArtilleryShellEntity(level, x, y, z);
        shell.shoot(dir.getStepX() * horizontal, vertical, dir.getStepZ() * horizontal, (float) MUZZLE_VELOCITY, 0.0F);
        level.addFreshEntity(shell);

        level.setBlock(pos, state.setValue(LOADED, false), 3);
        sound(level, pos, SoundEvents.GENERIC_EXPLODE);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 12, 0.2, 0.2, 0.2, 0.02);
        }
        return true;
    }

    private static void sound(Level level, BlockPos pos, SoundEvent event) {
        level.playSound(null, pos, event, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
