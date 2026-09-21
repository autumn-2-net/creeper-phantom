package net.creeperphantom;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/** Optional GTCEu bridge. No GT classes are linked when the mod is absent. */
final class GtMultiblocks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Adapter adapter = load();

    enum Kind { OTHER, UNFORMED_CONTROLLER, FORMED_CONTROLLER, UNREADABLE_MACHINE }

    static boolean available() {
        return adapter != null && !adapter.failed;
    }

    static Kind classify(BlockEntity entity) {
        return adapter == null ? Kind.OTHER : adapter.classify(entity);
    }

    private static Adapter load() {
        if (!ModList.get().isLoaded("gtceu")) return null;
        try {
            ClassLoader loader = GtMultiblocks.class.getClassLoader();
            return bind(Class.forName("com.gregtechceu.gtceu.api.machine.IMachineBlockEntity", false, loader),
                    Class.forName("com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController", false, loader));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            LOGGER.warn("GT multiblock targeting is unavailable: incompatible GTCEu API", e);
            return null;
        }
    }

    static Adapter bind(Class<?> holder, Class<?> controller) throws ReflectiveOperationException {
        Method formed = controller.getMethod("isFormed");
        if (formed.getReturnType() != boolean.class) throw new NoSuchMethodException("isFormed must return boolean");
        return new Adapter(holder, controller, holder.getMethod("getMetaMachine"), formed);
    }

    static final class Adapter {
        private final Class<?> holder;
        private final Class<?> controller;
        private final Method getMachine;
        private final Method isFormed;
        private volatile boolean failed;

        private Adapter(Class<?> holder, Class<?> controller, Method getMachine, Method isFormed) {
            this.holder = holder;
            this.controller = controller;
            this.getMachine = getMachine;
            this.isFormed = isFormed;
        }

        Kind classify(Object entity) {
            if (!holder.isInstance(entity)) return Kind.OTHER;
            if (failed) return Kind.UNREADABLE_MACHINE;
            try {
                Object machine = getMachine.invoke(entity);
                if (!controller.isInstance(machine)) return Kind.OTHER;
                return (boolean) isFormed.invoke(machine) ? Kind.FORMED_CONTROLLER : Kind.UNFORMED_CONTROLLER;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                // Log once and disable the bridge instead of throwing from an entity tick every frame.
                failed = true;
                LOGGER.warn("Disabling GT multiblock targeting after an API failure", e);
                return Kind.UNREADABLE_MACHINE;
            }
        }
    }

    private GtMultiblocks() {}
}
