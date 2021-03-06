package de.photon.aacadditionpro.user.data;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.user.Data;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.datastructures.buffer.ContinuousArrayBuffer;
import de.photon.aacadditionpro.util.datastructures.buffer.ContinuousBuffer;
import de.photon.aacadditionpro.util.packetwrappers.server.WrapperPlayServerKeepAlive;
import lombok.Getter;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class KeepAliveData extends Data
{
    // This needs to be so high to prevent flagging during TimeOuts.
    public static final byte KEEPALIVE_QUEUE_SIZE = 20;

    @Getter
    private final AtomicInteger ignoredKeepAlives = new AtomicInteger(0);

    static {
        ProtocolLibrary.getProtocolManager().addPacketListener(new KeepAliveDataUpdater());
    }

    @Getter
    /* The central Deque of the KeepAlive packet handling.
     *  Synchronized access to the Deque is a must.
     *
     *  Start deleting entries when the queue extends more than 4 times the normal size to prevent crashes.*/
    private final ContinuousBuffer<KeepAlivePacketData> keepAlives = new ContinuousArrayBuffer<KeepAlivePacketData>(KEEPALIVE_QUEUE_SIZE)
    {
        @Override
        public void onForget(KeepAlivePacketData forgotten)
        {
            if (!forgotten.hasRegisteredResponse()) {
                ignoredKeepAlives.getAndIncrement();
            }
        }
    };

    public KeepAliveData(User user)
    {
        super(user);
    }

    /**
     * Calculates how long the client needs to answer a KeepAlive packet on average.
     * Only uses the last 4 values for the calculation.
     */
    public long recentKeepAliveResponseTime() throws IllegalStateException
    {
        synchronized (keepAlives) {
            Preconditions.checkState(!keepAlives.isEmpty(), "KeepAlive queue is empty.");

            long sum = 0;
            byte datapoints = 0;

            final Iterator<KeepAlivePacketData> iterator = keepAlives.descendingIterator();
            KeepAlivePacketData data;

            while (iterator.hasNext() && datapoints <= 3) {
                data = iterator.next();

                // Leave out ignored packets.
                if (data.timeDifference >= 0) {
                    sum += data.timeDifference;
                    datapoints++;
                }
            }

            Preconditions.checkState(datapoints > 0, "No answered KeepAlive packets found.");
            return sum / datapoints;
        }
    }

    @Override
    public void unregister()
    {
        this.keepAlives.clear();
        super.unregister();
    }

    public static class KeepAlivePacketData
    {
        private final long timestamp = System.currentTimeMillis();
        @Getter
        private final long keepAliveID;
        @Getter
        private long timeDifference = -1;

        public KeepAlivePacketData(long keepAliveID) {this.keepAliveID = keepAliveID;}

        public void registerResponse()
        {
            timeDifference = System.currentTimeMillis() - timestamp;
        }

        public boolean hasRegisteredResponse()
        {
            return timeDifference >= 0;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            KeepAlivePacketData that = (KeepAlivePacketData) o;
            return keepAliveID == that.keepAliveID;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(keepAliveID);
        }
    }

    /**
     * A singleton class to reduce the reqired {@link com.comphenix.protocol.events.PacketListener}s to a minimum.
     */
    private static class KeepAliveDataUpdater extends PacketAdapter
    {
        private KeepAliveDataUpdater()
        {
            super(AACAdditionPro.getInstance(), ListenerPriority.MONITOR, PacketType.Play.Server.KEEP_ALIVE);
        }

        @Override
        public void onPacketSending(PacketEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user == null) {
                return;
            }

            // Register the KeepAlive
            synchronized (user.getKeepAliveData().getKeepAlives()) {
                user.getKeepAliveData().getKeepAlives().bufferObject(new KeepAliveData.KeepAlivePacketData(new WrapperPlayServerKeepAlive(event.getPacket()).getKeepAliveId()));
            }
        }
    }
}
