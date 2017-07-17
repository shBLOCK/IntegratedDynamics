package org.cyclops.integrateddynamics.core.network;

import lombok.Data;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import org.cyclops.cyclopscore.datastructure.DimPos;
import org.cyclops.integrateddynamics.api.PartStateException;
import org.cyclops.integrateddynamics.api.network.*;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.IPartState;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A network element for parts.
 * @author rubensworks
 */
@Data
public class PartNetworkElement<P extends IPartType<P, S>, S extends IPartState<P>> extends NetworkElementBase implements IPartNetworkElement<P, S>, IEnergyConsumingNetworkElement {

    private final P part;
    private final PartTarget target;

    protected static DimPos getCenterPos(PartTarget target) {
        return target.getCenter().getPos();
    }

    protected static EnumFacing getCenterSide(PartTarget target) {
        return target.getCenter().getSide();
    }

    protected static DimPos getTargetPos(PartTarget target) {
        return target.getTarget().getPos();
    }

    protected static EnumFacing getTargetSide(PartTarget target) {
        return target.getTarget().getSide();
    }

    @Override
    public IPartContainer getPartContainer() {
        return PartHelpers.getPartContainer(getCenterPos(getTarget()));
    }

    @Override
    public void setPriority(INetwork network, int priority) {
        //noinspection deprecation
        part.setPriority(network, NetworkHelpers.getPartNetwork(network), getTarget(), getPartState(), priority);
    }

    @Override
    public int getPriority() {
        return hasPartState() ? part.getPriority(getPartState()) : 0;
    }

    @Override
    public boolean canRevalidate(INetwork network) {
        return canRevalidatePositioned(network, getCenterPos(getTarget()));
    }

    @Override
    public void revalidate(INetwork network) {
        super.revalidate(network);
        revalidatePositioned(network, getCenterPos(getTarget()));
    }

    @Override
    public boolean isLoaded() {
        return getCenterPos(getTarget()).isLoaded();
    }

    public boolean hasPartState() {
        if (isLoaded()) {
            IPartContainer partContainer = getPartContainer();
            return partContainer != null && partContainer.hasPart(getCenterSide(getTarget()));
        }
        return false;
    }

    @Override
    public S getPartState() throws PartStateException {
        IPartContainer partContainer = getPartContainer();
        if(partContainer != null) {
            return (S) partContainer.getPartState(getCenterSide(getTarget()));
        } else {
            throw new PartStateException(getCenterPos(getTarget()), getTarget().getCenter().getSide());
        }
    }

    @Override
    public int getConsumptionRate() {
        return getPart().getConsumptionRate(getPartState());
    }

    @Override
    public void postUpdate(INetwork network, boolean updated) {
        part.postUpdate(network, NetworkHelpers.getPartNetwork(network), getTarget(), getPartState(), updated);
    }

    @Override
    public int getUpdateInterval() {
        return part.getUpdateInterval(getPartState());
    }

    @Override
    public boolean isUpdate() {
        return part.isUpdate(getPartState());
    }

    @Override
    public void update(INetwork network) {
        part.update(network, NetworkHelpers.getPartNetwork(network), getTarget(), getPartState());
    }

    @Override
    public void beforeNetworkKill(INetwork network) {
        part.beforeNetworkKill(network, NetworkHelpers.getPartNetwork(network), target, getPartState());
    }

    @Override
    public void afterNetworkAlive(INetwork network) {
        part.afterNetworkAlive(network, NetworkHelpers.getPartNetwork(network), target, getPartState());
    }

    @Override
    public void afterNetworkReAlive(INetwork network) {
        part.afterNetworkReAlive(network, NetworkHelpers.getPartNetwork(network), target, getPartState());
    }

    @Override
    public void addDrops(List<ItemStack> itemStacks, boolean dropMainElement) {
        part.addDrops(getTarget(), getPartState(), itemStacks, dropMainElement);
    }

    @Override
    public boolean onNetworkAddition(INetwork network) {
        IPartNetwork partNetwork = NetworkHelpers.getPartNetwork(network);
        boolean res = partNetwork.addPart(getPartState().getId(), getTarget().getCenter());
        if(res) {
            part.onNetworkAddition(network, partNetwork, target, getPartState());
        }
        return res;
    }

    @Override
    public void onNetworkRemoval(INetwork network) {
        IPartNetwork partNetwork = NetworkHelpers.getPartNetwork(network);
        partNetwork.removePart(getPartState().getId());
        part.onNetworkRemoval(network, partNetwork, target, getPartState());
    }

    @Override
    public void onPreRemoved(INetwork network) {
        part.onPreRemoved(network, NetworkHelpers.getPartNetwork(network), target, getPartState());
    }

    @Override
    public void onPostRemoved(INetwork network) {
        part.onPostRemoved(network, NetworkHelpers.getPartNetwork(network), target, getPartState());
    }

    @Override
    public void onNeighborBlockChange(@Nullable INetwork network, IBlockAccess world, Block neighborBlock) {
        part.onBlockNeighborChange(network, NetworkHelpers.getPartNetwork(network), target, getPartState(), world, neighborBlock);
    }

    @Override
    public P getNetworkEventListener() {
        return getPart();
    }

    public boolean equals(Object o) {
        return o instanceof IPartNetworkElement && compareTo((INetworkElement) o) == 0;
    }

    @Override
    public int hashCode() {
        int result = part.hashCode();
        result = 31 * result + target.hashCode();
        result = 31 * result + getPriority();
        return result;
    }

    @Override
    public int compareTo(INetworkElement o) {
        if(o instanceof IPartNetworkElement) {
            IPartNetworkElement p = (IPartNetworkElement) o;
            int compClass = Integer.compare(this.getPart().getClass().hashCode(), p.getPart().getClass().hashCode());
            if (compClass == 0) {
                // If this or the other part is not loaded, we IGNORE the priority,
                // because that depends on tile entity data, which requires loading the part/chunk.
                int compPriority = !isLoaded() || !p.isLoaded() ? 0 : -Integer.compare(this.getPriority(), p.getPriority());
                if (compPriority == 0) {
                    int compPart = Integer.compare(part.hashCode(), p.getPart().hashCode());
                    if (compPart == 0) {
                        int compPos = getCenterPos(getTarget()).compareTo(getCenterPos(p.getTarget()));
                        if (compPos == 0) {
                            return getCenterSide(getTarget()).compareTo(getCenterSide(p.getTarget()));
                        }
                        return compPos;
                    }
                    return compPart;
                } else {
                    return compPriority;
                }
            } else {
                return compClass;
            }
        }
        return Integer.compare(hashCode(), o.hashCode());
    }
}
