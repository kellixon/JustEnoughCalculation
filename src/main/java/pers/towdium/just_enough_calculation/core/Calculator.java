package pers.towdium.just_enough_calculation.core;

import net.minecraft.item.ItemStack;
import pers.towdium.just_enough_calculation.util.ItemStackHelper;
import pers.towdium.just_enough_calculation.util.ItemStackHelper.NBT;
import pers.towdium.just_enough_calculation.util.PlayerRecordHelper;
import pers.towdium.just_enough_calculation.util.exception.IllegalPositionException;
import pers.towdium.just_enough_calculation.util.wrappers.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Author: Towdium
 * Date:   2016/7/31.
 */

public class Calculator {
    List<CostList> costLists;

    public Calculator(ItemStack itemStack, long amount) {
        this(new CostList(NBT.setData(itemStack.copy(), ItemStackHelper.EnumStackAmountType.NUMBER, amount)));
    }

    public Calculator(List<ItemStack> itemStacks, ItemStack stack, long amount) {
        this(new CostList(new CostList(NBT.setData(stack.copy(), ItemStackHelper.EnumStackAmountType.NUMBER, amount)), new CostList(itemStacks)));
        costLists.add(new CostList(costLists.get(costLists.size() - 1), new CostList(itemStacks), true));
    }

    public Calculator(CostList cost) {
        int count = 0;
        costLists = new ArrayList<>();
        costLists.add(cost);
        List<ItemStack> cancellableItems = costLists.get(0).getValidItems();
        LOOP2:
        while (cancellableItems.size() != 0) {
            ++count;
            if (count > 10000) {
                throw new JECCalculatingCoreException();
            }
            // all the items possible tp cancel
            for (ItemStack stack : cancellableItems) {
                // all the recipes for one item
                LOOP1:
                for (Recipe recipe : PlayerRecordHelper.getAllRecipeOutput(stack)) {
                    CostList record = new CostList(costLists.get(costLists.size() - 1), new CostList(recipe, NBT.setAmount(stack.copy(), -NBT.getAmount(stack)), getCount(stack, recipe)));
                    for (CostList costList : costLists) {
                        if (costList.equals(record)) {
                            continue LOOP1;
                        }
                        costLists.add(record);
                        cancellableItems = record.getValidItems();
                        continue LOOP2;
                    }
                }
            }
            break;
        }
        costLists.get(costLists.size() - 1).finalise();
    }

    public List<ItemStack> getInput() {
        return getList(itemStack -> NBT.getAmount(itemStack) < 0, costList -> costList.items, itemStack -> NBT.setAmount(itemStack, -NBT.getAmount(itemStack)));
    }

    public List<ItemStack> getOutput() {
        return getList(itemStack -> NBT.getAmount(itemStack) > 0, costList -> costList.items, itemStack -> itemStack);
    }

    public List<ItemStack> getCatalyst() {
        return getList(itemStack -> NBT.getAmount(itemStack) > 0, costList -> costList.catalyst, itemStack -> itemStack);
    }

    public List<ItemStack> getProcedure() {
        List<ItemStack> procedure = costLists.get(costLists.size() - 1).procedure;
        ListIterator<ItemStack> iterator = procedure.listIterator(procedure.size());
        List<ItemStack> buffer = new ArrayList<>();
        while (iterator.hasPrevious()) {
            CostList.merge(CostList.EnumMergeType.NORMAL_MERGE, buffer, iterator.previous());
        }
        return buffer;
    }

    List<ItemStack> getList(Predicate<ItemStack> filter, Function<CostList, List<ItemStack>> getter, Function<ItemStack, ItemStack> modifier) {
        List<ItemStack> buffer = new ArrayList<>();
        getter.apply(costLists.get(costLists.size() - 1)).stream().filter(filter).forEach(itemStack -> CostList.merge(CostList.EnumMergeType.NORMAL_MERGE, buffer, modifier.apply(itemStack.copy())));
        return buffer;
    }

    protected long getCount(ItemStack itemStack, Recipe recipe) {
        long a = recipe.getAmountOutput(itemStack);
        return (-ItemStackHelper.NBT.getAmountInternal(itemStack) + a - 1) / a;
    }

    static class CostList {
        static final BiFunction<Long, Long, Long> NORMAL_MERGE = (i, j) -> i + j;
        static final BiFunction<Long, Long, Long> NORMAL_CANCEL = (i, j) -> i - j;
        static final BiFunction<Long, Long, Long> CATALYST_CANCEL = (i, j) -> j > i ? (i > 0 ? 0 : i) : j > 0 ? i - j : i;
        static final BiFunction<Long, Long, Long> CATALYST_MERGE = (i, j) -> -j > i ? 0 : j < 0 ? i + j : i;
        List<ItemStack> items = new ArrayList<>();
        List<ItemStack> catalyst = new ArrayList<>();
        List<ItemStack> procedure = new ArrayList<>();

        public CostList(List<ItemStack> inventory) {
            inventory.stream().filter(stack -> stack != null).forEach(stack -> items.add(ItemStackHelper.toItemStackJEC(stack.copy())));
        }

        public CostList(ItemStack itemStack) {
            long amount = NBT.getAmount(itemStack);
            catalyst.add(itemStack.copy());
            items.add(NBT.setAmount(itemStack.copy(), -amount));
        }

        public CostList(Recipe recipe, ItemStack dest, long amount) {
            merge(EnumMergeType.NORMAL_MERGE, items, recipe.output);
            merge(EnumMergeType.NORMAL_MERGE, catalyst, recipe.input);
            catalyst.forEach(itemStack -> NBT.setAmount(itemStack, NBT.getAmount(itemStack) * amount));
            merge(EnumMergeType.NORMAL_MERGE, catalyst, recipe.catalyst);
            merge(EnumMergeType.NORMAL_CANCEL, items, recipe.input);
            items.forEach(itemStack -> NBT.setAmount(itemStack, NBT.getAmount(itemStack) * amount));
            procedure.add(dest.copy());
        }

        public CostList(CostList caller, CostList callee) {
            this(caller, callee, false);
        }

        public CostList(CostList caller, CostList callee, boolean inventoryCancel) {
            if (inventoryCancel) {
                merge(EnumMergeType.NORMAL_MERGE, items, caller.items);
                merge(EnumMergeType.NORMAL_MERGE, catalyst, caller.catalyst);
                merge(EnumMergeType.CATALYST_CANCEL, items, callee.items);
                merge(EnumMergeType.CATALYST_CANCEL, catalyst, callee.items);
                caller.procedure.stream().forEachOrdered(itemStack -> procedure.add(itemStack.copy()));
            } else {
                merge(EnumMergeType.NORMAL_MERGE, items, caller.items);
                merge(EnumMergeType.NORMAL_MERGE, catalyst, caller.catalyst);
                merge(EnumMergeType.NORMAL_MERGE, items, callee.items);
                merge(EnumMergeType.CATALYST_CANCEL, catalyst, callee.catalyst);
                merge(EnumMergeType.NORMAL_MERGE, catalyst, callee.catalyst);
                merge(EnumMergeType.CATALYST_CANCEL, catalyst, callee.items);
                caller.procedure.stream().forEachOrdered(itemStack -> procedure.add(itemStack.copy()));
                callee.procedure.stream().forEachOrdered(itemStack -> procedure.add(itemStack.copy()));
            }
        }

        public static void merge(EnumMergeType type, List<ItemStack> container, List<ItemStack> itemStacks) {
            itemStacks.forEach(itemStack -> merge(type, container, itemStack));
        }

        public static void merge(EnumMergeType type, List<ItemStack> container, ItemStack[] itemStacks) {
            for (ItemStack itemStack : itemStacks) {
                merge(type, container, itemStack);
            }
        }

        public static void merge(EnumMergeType type, List<ItemStack> container, ItemStack itemStack) {
            if (itemStack == null) {
                return;
            }
            for (ItemStack itemStackInternal : container) {
                if (merge(itemStackInternal, itemStack, type.getFunc()))
                    return;
            }
            ItemStack stack = itemStack.copy();
            NBT.setAmount(stack, type.getFunc().apply(0L, NBT.getAmount(stack)));
            container.add(stack);
        }

        static boolean merge(ItemStack stackInternal, ItemStack stackExternal, BiFunction<Long, Long, Long> func) {
            if (ItemStackHelper.isItemEqual(stackExternal, stackInternal)) {
                if (NBT.getType(stackExternal) == NBT.getType(stackInternal)) {
                    NBT.setData(stackInternal, NBT.getType(stackInternal), func.apply(NBT.getAmount(stackInternal), NBT.getAmount(stackExternal)));
                    return true;
                } else if (NBT.getType(stackExternal) == ItemStackHelper.EnumStackAmountType.FLUID || NBT.getType(stackInternal) == ItemStackHelper.EnumStackAmountType.FLUID) {
                    return false;
                } else {
                    NBT.setData(stackInternal, ItemStackHelper.EnumStackAmountType.PERCENTAGE, func.apply(NBT.getAmountInternal(stackExternal), NBT.getAmountInternal(stackInternal)));
                    return true;
                }
            } else {
                return false;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CostList) {
                BiPredicate<ItemStack, List<ItemStack>> stackChecker = (itemStack, stacks) -> {
                    Singleton<Boolean> flag = new Singleton<>(false);
                    stacks.forEach(toCheck -> flag.value = flag.value || itemStack.equals(toCheck));
                    return flag.value;
                };

                BiPredicate<List<ItemStack>, List<ItemStack>> listChecker = (stacks1, stacks2) -> {
                    Singleton<Boolean> flag = new Singleton<>(true);
                    stacks1.forEach(itemStack -> flag.value = flag.value && stackChecker.test(itemStack, stacks2));
                    return flag.value;
                };

                BiPredicate<List<ItemStack>, List<ItemStack>> doubleChecker = (stacks1, stacks2) ->
                        listChecker.test(stacks1, stacks2) && listChecker.test(stacks2, stacks1);

                CostList costList = ((CostList) obj);
                return doubleChecker.test(items, costList.items) && doubleChecker.test(catalyst, costList.catalyst);
            } else {
                return false;
            }
        }

        public void finalise() {
            merge(EnumMergeType.CATALYST_MERGE, catalyst, items);
        }

        public List<ItemStack> getValidItems() {
            List<ItemStack> buffer = new ArrayList<>();
            items.stream().filter(itemStack -> ItemStackHelper.NBT.getAmount(itemStack) < 0).forEach(buffer::add);
            return buffer;
        }

        enum EnumMergeType {
            NORMAL_MERGE, NORMAL_CANCEL, CATALYST_CANCEL, CATALYST_MERGE;

            BiFunction<Long, Long, Long> getFunc() {
                switch (this) {
                    case NORMAL_MERGE:
                        return CostList.NORMAL_MERGE;
                    case NORMAL_CANCEL:
                        return CostList.NORMAL_CANCEL;
                    case CATALYST_CANCEL:
                        return CostList.CATALYST_CANCEL;
                    case CATALYST_MERGE:
                        return CostList.CATALYST_MERGE;
                    default:
                        throw new IllegalPositionException();
                }
            }
        }
    }

    public static class JECCalculatingCoreException extends RuntimeException {
    }
}

