package ru.clanplugin.clan;

import java.util.*;

public class ClanTeleportManager {

    public static class TeleportRequest {
        private final UUID sender;
        private final UUID target;
        private final long timestamp;

        public TeleportRequest(UUID sender, UUID target) {
            this.sender = sender;
            this.target = target;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getSender() {
            return sender;
        }

        public UUID getTarget() {
            return target;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 60000L; // 60 seconds lifetime
        }
    }

    // Map of Sender UUID -> TeleportRequest
    private final Map<UUID, TeleportRequest> requests = new HashMap<>();

    public void createRequest(UUID sender, UUID target) {
        requests.put(sender, new TeleportRequest(sender, target));
    }

    public TeleportRequest getRequestFrom(UUID sender) {
        TeleportRequest request = requests.get(sender);
        if (request != null && request.isExpired()) {
            requests.remove(sender);
            return null;
        }
        return request;
    }

    public List<TeleportRequest> getActiveRequestsTo(UUID target) {
        List<TeleportRequest> active = new ArrayList<>();
        Iterator<Map.Entry<UUID, TeleportRequest>> it = requests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TeleportRequest> entry = it.next();
            TeleportRequest req = entry.getValue();
            if (req.isExpired()) {
                it.remove();
            } else if (req.getTarget().equals(target)) {
                active.add(req);
            }
        }
        // Sort by timestamp descending (latest first)
        active.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return active;
    }

    public void removeRequest(UUID sender) {
        requests.remove(sender);
    }
}