package org.geoserver.cluster.hazelcast;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.cluster.Event;
import org.geoserver.config.GeoServer;

/**
 * Synchronizer that does a full geoserver reload on any event.
 * <p>
 * This synchronizer assumes a shared data directory among nodes in the cluster.
 * </p>
 * @author Justin Deoliveira, OpenGeo
 */
public class ReloadHzSynchronizer extends HzSynchronizer {

    /** lock during reload */
    protected AtomicBoolean eventLock = new AtomicBoolean();

    public ReloadHzSynchronizer(HzCluster cluster, GeoServer gs, GeoServerConfigurationLock geoServerConfigurationLock) {
        super(cluster, gs,geoServerConfigurationLock);
    }
    
    @Override
    protected void processEventQueue(Queue<Event> q) throws Exception {
        //lock during event processing
        eventLock.set(true);
        geoServerConfigurationLock.lock(LockType.WRITE);
        try {
            gs.reload();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Reload failed", e);
        }
        finally {
            q.clear();
            geoServerConfigurationLock.unlock(LockType.WRITE);
            eventLock.set(false);
        }
    }

    @Override
    protected void dispatch(Event e) {
        //check lock, if locked it means event in response to configuration reload, don't propagate
        if (eventLock.get()) {
            return;
        }

        super.dispatch(e);
    }
}
